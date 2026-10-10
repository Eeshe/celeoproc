package me.eeshe.celeoproc.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.PowerOutageRegistryMessage;

/**
 * Repository test backed by a real PostgreSQL container so the production SQL
 * dialect (batch inserts/updates, UUID, foreign key cascade) is exercised.
 */
class PowerOutageRegistryMessageRepositoryImplTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:18.6"));

    private static Database database;
    private static PowerOutageLogRepositoryImpl logRepository;
    private static PowerOutageRegistryMessageRepositoryImpl repository;

    static {
        // Docker Engine 29 requires API >= 1.40, but the docker-java client shaded
        // inside Testcontainers defaults to 1.32.
        System.setProperty("api.version", "1.44");
    }

    @BeforeAll
    static void startContainer() throws SQLException {
        POSTGRES.start();
        database = new DriverManagerDatabase(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        logRepository = new PowerOutageLogRepositoryImpl(database);
        logRepository.initialize();
        repository = new PowerOutageRegistryMessageRepositoryImpl(database);
        repository.initialize();
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @BeforeEach
    void clearTables() throws SQLException {
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM power_outage_log")) {
            statement.executeUpdate();
        }
    }

    @Test
    void saveAllPersistsRowsAndGetByPowerOutageLogIdReturnsThem() {
        final UUID logId = saveLog();
        final PowerOutageRegistryMessage first = new PowerOutageRegistryMessage(10L, null, logId, 100L);
        final PowerOutageRegistryMessage second = new PowerOutageRegistryMessage(20L, null, logId, 200L);

        repository.saveAll(List.of(first, second));

        final List<PowerOutageRegistryMessage> stored = repository.getByPowerOutageLogId(logId);
        assertEquals(2, stored.size());
        assertTrue(stored.contains(first));
        assertTrue(stored.contains(second));
        assertNull(stored.getFirst().registryInMessageId());
    }

    @Test
    void updateAllFillsRegistryInMessageId() {
        final UUID logId = saveLog();
        repository.saveAll(List.of(
                new PowerOutageRegistryMessage(10L, null, logId, 100L),
                new PowerOutageRegistryMessage(20L, null, logId, 200L)));

        repository.updateAll(List.of(
                new PowerOutageRegistryMessage(10L, 11L, logId, 100L),
                new PowerOutageRegistryMessage(20L, 21L, logId, 200L)));

        final List<PowerOutageRegistryMessage> stored = repository.getByPowerOutageLogId(logId);
        assertEquals(2, stored.size());
        assertEquals(Long.valueOf(11L), stored.stream()
                .filter(message -> message.registryOutMessageId() == 10L)
                .findFirst().orElseThrow().registryInMessageId());
        assertEquals(Long.valueOf(21L), stored.stream()
                .filter(message -> message.registryOutMessageId() == 20L)
                .findFirst().orElseThrow().registryInMessageId());
    }

    @Test
    void deletingLogCascadesRegistryMessages() {
        final UUID logId = saveLog();
        repository.saveAll(List.of(new PowerOutageRegistryMessage(10L, null, logId, 100L)));

        logRepository.delete(logId);

        assertTrue(repository.getByPowerOutageLogId(logId).isEmpty());
    }

    @Test
    void getByPowerOutageLogIdReturnsEmptyForUnknownLog() {
        assertTrue(repository.getByPowerOutageLogId(UUID.randomUUID()).isEmpty());
    }

    private static UUID saveLog() {
        final UUID logId = UUID.randomUUID();
        logRepository.save(new PowerOutageLog(logId, 1L, Instant.parse("2025-05-06T10:00:00Z"), null));
        return logId;
    }

    /**
     * Minimal {@link Database} implementation that hands out plain JDBC
     * connections to the test container.
     */
    private static final class DriverManagerDatabase implements Database {
        private final String url;
        private final String user;
        private final String password;

        private DriverManagerDatabase(final String url, final String user, final String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public void connect() {
            // Connections are opened on demand, nothing to do.
        }

        @Override
        public void disconnect() {
            // Connections are closed by the caller, nothing to do.
        }

        @Override
        public Connection getConnection() {
            try {
                return DriverManager.getConnection(url, user, password);
            } catch (final SQLException exception) {
                throw new IllegalStateException("Failed to acquire test connection", exception);
            }
        }
    }
}
