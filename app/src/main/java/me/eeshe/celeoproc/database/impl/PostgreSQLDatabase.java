package me.eeshe.celeoproc.database.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.eeshe.celeoproc.config.AppSecrets;
import me.eeshe.celeoproc.database.Database;

public final class PostgreSQLDatabase implements Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLDatabase.class);

    private static final String POOL_NAME = "celeoproc-pool";
    private static final long CONNECTION_TIMEOUT_MILLIS = 30_000;
    private static final long MAX_LIFETIME_MILLIS = 1_800_000;

    private final HikariConfig hikariConfig;

    private HikariDataSource dataSource;

    public PostgreSQLDatabase(AppSecrets appSecrets) {
        Objects.requireNonNull(appSecrets, "AppSecrets must not be null");
        this.hikariConfig = buildHikariConfig(appSecrets);
    }

    private static HikariConfig buildHikariConfig(AppSecrets appSecrets) {
        final HikariConfig config = new HikariConfig();

        config.setPoolName(POOL_NAME);
        config.setJdbcUrl(appSecrets.getPostgresUrl());
        config.setUsername(appSecrets.getPostgresUser());
        config.setPassword(appSecrets.getPostgresPassword());
        config.setMaximumPoolSize(appSecrets.getDatabasePoolSize());
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MILLIS);
        config.setMaxLifetime(MAX_LIFETIME_MILLIS);
        config.setInitializationFailTimeout(1);

        return config;
    }

    @Override
    public void connect() throws SQLException {
        LOGGER.info("Connecting to PostgreSQL...");
        this.dataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("Successfully connected to PostgreSQL");
    }

    @Override
    public void disconnect() throws SQLException {
        if (dataSource == null) {
            return;
        }
        dataSource.close();
        dataSource = null;
        LOGGER.info("Disconnected from PostgreSQL");
    }

    @Override
    public Connection getConnection() {
        if (dataSource == null) {
            throw new IllegalStateException("Database is not connected, call connect() first");
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException exception) {
            LOGGER.error("Failed to acquire database connection", exception);
            throw new IllegalStateException("Failed to acquire database connection", exception);
        }
    }
}
