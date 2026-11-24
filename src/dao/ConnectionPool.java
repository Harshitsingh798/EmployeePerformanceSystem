package dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small, dependency-free JDBC connection pool.
 *
 * <p><b>Why this exists.</b> The original implementation handed the <em>same</em>
 * {@link Connection} to every DAO call while the DAOs closed it again through
 * try-with-resources. With several {@code SwingWorker}s loading data at once
 * (as the dashboards do on start-up) one thread could close the connection
 * another thread was still using, producing
 * {@code "No operations allowed after connection closed"}. A pool removes that
 * race: each caller gets exclusive use of a connection, and closing it returns
 * it to the pool instead of tearing it down.
 *
 * <p><b>How the return-to-pool behaviour is achieved.</b> JDBC code closes a
 * connection with try-with-resources, so the pool hands out a dynamic
 * {@link Proxy} whose {@code close()} method returns the physical connection to
 * the pool rather than closing it. That keeps every DAO unchanged while making
 * the ownership rules correct.
 *
 * <p>Connections are created lazily up to {@code maxSize}; callers wait on a
 * {@link BlockingQueue} when the pool is exhausted, which is what keeps the
 * background workers from opening an unbounded number of sockets.
 */
public final class ConnectionPool implements AutoCloseable {

    /** Default number of physical connections kept open. */
    public static final int DEFAULT_MAX_SIZE = 5;

    /** How long a caller waits for a free connection before failing. */
    private static final long BORROW_TIMEOUT_SECONDS = 10;

    private final String url;
    private final String user;
    private final String password;
    private final int maxSize;

    /** Idle connections waiting to be handed out. */
    private final BlockingQueue<Connection> idle;

    /** Every physical connection this pool has ever created (for shutdown). */
    private final List<Connection> created = new ArrayList<>();

    /** Number of physical connections currently owned by the pool. */
    private final AtomicInteger size = new AtomicInteger(0);

    /** {@code true} once {@link #close()} has been called. */
    private volatile boolean closed;

    /**
     * Creates a pool for the given database coordinates.
     *
     * @param url      JDBC URL
     * @param user     database user
     * @param password database password
     * @param maxSize  maximum number of concurrent physical connections
     */
    public ConnectionPool(String url, String user, String password, int maxSize) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.maxSize = Math.max(1, maxSize);
        this.idle = new ArrayBlockingQueue<>(this.maxSize);
    }

    /**
     * Borrows a connection from the pool.
     *
     * @return a connection whose {@code close()} returns it to the pool
     * @throws SQLException if no connection could be obtained
     */
    public Connection borrow() throws SQLException {
        if (closed) {
            throw new SQLException("Connection pool has been closed");
        }
        while (true) {
            Connection pooled = idle.poll();
            if (pooled != null) {
                if (pooled.isClosed()) {
                    // Dead entry - drop it and try again.
                    size.decrementAndGet();
                    continue;
                }
                return wrap(pooled);
            }
            if (size.get() < maxSize) {
                // Reserve a slot before connecting so two threads cannot
                // both decide that there is room for one more.
                if (size.incrementAndGet() > maxSize) {
                    size.decrementAndGet();
                } else {
                    try {
                        Connection fresh = DriverManager.getConnection(url, user, password);
                        synchronized (created) {
                            created.add(fresh);
                        }
                        return wrap(fresh);
                    } catch (SQLException e) {
                        size.decrementAndGet();
                        throw e;
                    }
                }
            }
            try {
                Connection waited = idle.poll(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (waited == null) {
                    throw new SQLException("Timed out after " + BORROW_TIMEOUT_SECONDS
                            + "s waiting for a database connection");
                }
                if (waited.isClosed()) {
                    size.decrementAndGet();
                    continue;
                }
                return wrap(waited);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for a database connection", e);
            }
        }
    }

    /**
     * Returns a physical connection to the pool.
     *
     * @param physical the connection that was handed out
     */
    void release(Connection physical) {
        if (physical == null) {
            return;
        }
        if (closed) {
            closeQuietly(physical);
            return;
        }
        try {
            if (physical.isClosed()) {
                size.decrementAndGet();
                return;
            }
            if (!idle.offer(physical)) {
                // Pool is full - the connection is surplus, so close it.
                size.decrementAndGet();
                closeQuietly(physical);
            }
        } catch (SQLException e) {
            size.decrementAndGet();
            closeQuietly(physical);
        }
    }

    /**
     * Closes every physical connection owned by this pool.
     */
    @Override
    public void close() {
        closed = true;
        List<Connection> snapshot;
        synchronized (created) {
            snapshot = new ArrayList<>(created);
            created.clear();
        }
        for (Connection connection : snapshot) {
            closeQuietly(connection);
        }
        idle.clear();
        size.set(0);
    }

    /**
     * @return the number of physical connections currently open
     */
    public int getOpenConnectionCount() {
        return size.get();
    }

    /**
     * @return the number of connections sitting idle in the pool
     */
    public int getIdleConnectionCount() {
        return idle.size();
    }

    /**
     * Wraps a physical connection in a proxy whose {@code close()} returns the
     * connection to this pool instead of closing it, and which discards the
     * connection when a statement fails (for example after MySQL restarted).
     */
    private Connection wrap(Connection physical) {
        return (Connection) Proxy.newProxyInstance(
                ConnectionPool.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "close":
                            release(physical);
                            return null;
                        case "isClosed":
                            return physical.isClosed();
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "toString":
                            return "PooledConnection[" + physical + "]";
                        default:
                            try {
                                return method.invoke(physical, args);
                            } catch (InvocationTargetException e) {
                                Throwable cause = e.getCause();
                                if (cause instanceof SQLException) {
                                    // The connection is in an unknown state -
                                    // never hand it back to the pool.
                                    discard(physical);
                                }
                                throw cause;
                            }
                    }
                });
    }

    /**
     * Permanently removes a broken connection from the pool.
     */
    private void discard(Connection physical) {
        size.decrementAndGet();
        closeQuietly(physical);
        synchronized (created) {
            created.remove(physical);
        }
    }

    /**
     * Closes a connection without throwing.
     */
    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Nothing useful can be done while tearing a connection down.
        }
    }
}
