package dao;

import java.util.List;

/**
 * Generic DAO Interface - demonstrates Generics
 * @param <T> Entity type
 */
public interface DAO<T> {
    boolean add(T entity) throws Exception;
    T getById(int id) throws Exception;
    List<T> getAll() throws Exception;
    boolean update(T entity) throws Exception;
    boolean delete(int id) throws Exception;
}
