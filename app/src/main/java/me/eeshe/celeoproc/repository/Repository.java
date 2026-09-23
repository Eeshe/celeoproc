package me.eeshe.celeoproc.repository;

import java.sql.SQLException;

/**
 * Common lifecycle shared by all repositories.
 *
 * <p>{@link #initialize()} is called once at startup and is responsible for
 * preparing the persistence layer (for example creating the backing tables).
 * {@link #shutdown()} is called once at shutdown to release resources.
 */
public interface Repository {

    void initialize() throws SQLException;

    void shutdown() throws SQLException;
}
