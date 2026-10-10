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

/**
 * Repository test backed by a real PostgreSQL container so the production SQL
 * dialect (UUID, TIMESTAMPTZ, {@code ON CONFLICT}, index DDL) is exercised.
 */
class PowerOutageLogRepositoryImplTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:18.6"));

    private static Database database;
    private static PowerOutageLogRepositoryImpl repository;

    private static final Instant RANGE_START = Instant.parse("2025-05-05T00:00:00Z");
    private static final Instant RANGE_END = Instant.parse("2025-05-10T23:59:59Z");

    static {
        // Docker Engine 29 requires API >= 1.40, but the docker-java client shaded
        // inside Testcontainers defaults to 1.32.
        System.setProperty("api.version", "1.44");
    }

    @BeforeAll
    static void startContainer() throws SQLException {
        POSTGRES.start();
        database = new DriverManagerDatabase(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        repository = new PowerOutageLogRepositoryImpl(database);
        repository.initialize();
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @BeforeEach
    void clearTable() throws SQLException {
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM power_outage_log")) {
            statement.executeUpdate();
        }
    }

    @Test
    void savePersistsAndGetWithinRangeReturnsStoredLog() {
        final PowerOutageLog log = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");

        repository.save(log);

        final List<PowerOutageLog> result = repository.getWithinRange(RANGE_START, RANGE_END);
        assertEquals(List.of(log), result);
        assertEquals(log.id(), result.getFirst().id());
        assertEquals(log.userId(), result.getFirst().userId());
    }

    @Test
    void getWithinRangeIncludesOutageStartingInRange() {
        final PowerOutageLog overnight = log(2L, "2025-05-10T23:00:00Z", "2025-05-11T03:00:00Z");

        repository.save(overnight);

        final List<PowerOutageLog> result = repository.getWithinRange(RANGE_START, RANGE_END);
        assertEquals(List.of(overnight), result);
    }

    @Test
    void getWithinRangeExcludesOutagesOutsideRange() {
        repository.save(log(3L, "2025-05-04T20:00:00Z", "2025-05-04T22:00:00Z"));
        repository.save(log(4L, "2025-05-11T05:00:00Z", "2025-05-11T06:00:00Z"));

        final List<PowerOutageLog> result = repository.getWithinRange(RANGE_START, RANGE_END);
        assertTrue(result.isEmpty());
    }

    @Test
    void getWithinRangeIncludesInclusiveBoundaries() {
        final PowerOutageLog startsAtRangeStart = log(5L, "2025-05-05T00:00:00Z", "2025-05-05T02:00:00Z");
        final PowerOutageLog endsAtRangeEnd = log(6L, "2025-05-10T20:00:00Z", "2025-05-10T23:59:59Z");

        repository.save(startsAtRangeStart);
        repository.save(endsAtRangeEnd);

        final List<PowerOutageLog> result = repository.getWithinRange(RANGE_START, RANGE_END);
        assertEquals(List.of(startsAtRangeStart, endsAtRangeEnd), result);
    }

    @Test
    void getWithinRangeOrdersByElectricityOut() {
        final PowerOutageLog third = log(9L, "2025-05-09T01:00:00Z", "2025-05-09T02:00:00Z");
        final PowerOutageLog first = log(7L, "2025-05-06T01:00:00Z", "2025-05-06T02:00:00Z");
        final PowerOutageLog second = log(8L, "2025-05-07T01:00:00Z", "2025-05-07T02:00:00Z");

        repository.save(third);
        repository.save(first);
        repository.save(second);

        final List<PowerOutageLog> result = repository.getWithinRange(RANGE_START, RANGE_END);
        assertEquals(List.of(first, second, third), result);
    }

    @Test
    void getByIdReturnsStoredLog() {
        final PowerOutageLog log = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");

        repository.save(log);

        assertEquals(Optional.of(log), repository.getById(log.id()));
    }

    @Test
    void getByIdReturnsEmptyForUnknownId() {
        assertTrue(repository.getById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void deleteRemovesStoredLog() {
        final PowerOutageLog log = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");
        repository.save(log);

        repository.delete(log.id());

        assertTrue(repository.getById(log.id()).isEmpty());
        assertTrue(repository.getWithinRange(RANGE_START, RANGE_END).isEmpty());
    }

    @Test
    void savePersistsNullElectricityIn() {
        final PowerOutageLog incomplete = new PowerOutageLog(UUID.randomUUID(), 1L,
                Instant.parse("2025-05-06T10:00:00Z"), null);

        repository.save(incomplete);

        final Optional<PowerOutageLog> stored = repository.getById(incomplete.id());
        assertEquals(Optional.of(incomplete), stored);
        assertNull(stored.orElseThrow().electricityIn());
    }

    @Test
    void updateFillsElectricityIn() {
        final PowerOutageLog incomplete = new PowerOutageLog(UUID.randomUUID(), 1L,
                Instant.parse("2025-05-06T10:00:00Z"), null);
        repository.save(incomplete);

        final PowerOutageLog completed = new PowerOutageLog(incomplete.id(), 1L,
                incomplete.electricityOut(), Instant.parse("2025-05-06T12:00:00Z"));
        repository.update(completed);

        assertEquals(Optional.of(completed), repository.getById(incomplete.id()));
    }

    @Test
    void getIncompletePowerOutageLogReturnsNewestIncompleteLog() {
        repository.save(new PowerOutageLog(UUID.randomUUID(), 1L,
                Instant.parse("2025-05-06T10:00:00Z"), null));
        final PowerOutageLog newest = new PowerOutageLog(UUID.randomUUID(), 1L,
                Instant.parse("2025-05-07T10:00:00Z"), null);
        repository.save(newest);
        repository.save(log(1L, "2025-05-08T10:00:00Z", "2025-05-08T12:00:00Z"));

        assertEquals(Optional.of(newest), repository.getIncompletePowerOutageLog(1L));
        assertTrue(repository.getIncompletePowerOutageLog(99L).isEmpty());
    }

    @Test
    void getPreviousCompletedPowerOutageLogReturnsNewestBeforeInstant() {
        final PowerOutageLog earlier = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");
        final PowerOutageLog later = log(1L, "2025-05-07T10:00:00Z", "2025-05-07T12:00:00Z");
        repository.save(earlier);
        repository.save(later);

        assertEquals(Optional.of(earlier),
                repository.getPreviousCompletedPowerOutageLog(1L, Instant.parse("2025-05-07T10:00:00Z")));
        assertEquals(Optional.of(later),
                repository.getPreviousCompletedPowerOutageLog(1L, Instant.parse("2025-05-08T00:00:00Z")));
        assertTrue(repository.getPreviousCompletedPowerOutageLog(99L, Instant.parse("2025-05-08T00:00:00Z"))
                .isEmpty());
    }

    @Test
    void getPreviousCompletedPowerOutageLogExcludesIncompleteLogs() {
        final PowerOutageLog completed = log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z");
        repository.save(completed);
        repository.save(new PowerOutageLog(UUID.randomUUID(), 1L,
                Instant.parse("2025-05-07T10:00:00Z"), null));

        assertEquals(Optional.of(completed),
                repository.getPreviousCompletedPowerOutageLog(1L, Instant.parse("2025-05-08T00:00:00Z")));
    }

    @Test
    void getWithinRangeExcludesIncompleteLogs() {
        repository.save(new PowerOutageLog(UUID.randomUUID(), 1L,
                Instant.parse("2025-05-06T10:00:00Z"), null));

        assertTrue(repository.getWithinRange(RANGE_START, RANGE_END).isEmpty());
    }

    private static PowerOutageLog log(final long userId, final String electricityOut, final String electricityIn) {
        return new PowerOutageLog(
                UUID.randomUUID(),
                userId,
                Instant.parse(electricityOut),
                Instant.parse(electricityIn));
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
