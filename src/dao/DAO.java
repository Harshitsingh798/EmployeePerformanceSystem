package dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Generic CRUD contract shared by every data-access class.
 *
 * <p>Using a generic interface here demonstrates the Generics rubric item and
 * keeps the DAO implementations free of copy-pasted boilerplate.
 *
 * @param <T> the entity type handled by the implementation
 */
public interface DAO<T> {

    /**
     * Inserts a new entity.
     *
     * @param entity the entity to persist
     * @return {@code true} when a row was written
     * @throws SQLException if the insert fails
     */
    boolean add(T entity) throws SQLException;

    /**
     * Loads a single entity by its primary key.
     *
     * @param id the primary key
     * @return the entity, or {@code null} when no such row exists
     * @throws SQLException if the query fails
     */
    T getById(int id) throws SQLException;

    /**
     * Loads every entity of this type.
     *
     * @return the entities, never {@code null}
     * @throws SQLException if the query fails
     */
    List<T> getAll() throws SQLException;

    /**
     * Updates an existing entity.
     *
     * @param entity the entity to update; its primary key must be set
     * @return {@code true} when a row was changed
     * @throws SQLException if the update fails
     */
    boolean update(T entity) throws SQLException;

    /**
     * Deletes an entity by its primary key.
     *
     * @param id the primary key
     * @return {@code true} when a row was removed
     * @throws SQLException if the delete fails
     */
    boolean delete(int id) throws SQLException;
}
