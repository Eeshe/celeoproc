package me.eeshe.celeoproc.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;

/**
 * Repository test backed by a real PostgreSQL container so the production SQL
 * dialect (BIGINT[] participants, {@code ON CONFLICT}, index DDL) is exercised.
 */
class ElectricityStatusEmbedRepositoryImplTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:18.6"));

    private static final long GUILD_A = 1L;
    private static final long GUILD_B = 2L;

    private static Database database;
    private static ElectricityStatusEmbedRepositoryImpl repository;

    static {
        // Docker Engine 29 requires API >= 1.40, but the docker-java client shaded
        // inside Testcontainers defaults to 1.32.
        System.setProperty("api.version", "1.44");
    }

    @BeforeAll
    static void startContainer() throws SQLException {
        POSTGRES.start();
        database = new DriverManagerDatabase(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        repository = new ElectricityStatusEmbedRepositoryImpl(database);
        repository.initialize();
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @BeforeEach
    void clearTable() throws SQLException {
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection
                        .prepareStatement("DELETE FROM electricity_status_embed")) {
            statement.executeUpdate();
        }
    }

    @Test
    void getByGuildIdReturnsOnlyThatGuildsEmbeds() {
        repository.save(embed(10L, GUILD_A, List.of(1L, 2L)));
        repository.save(embed(11L, GUILD_A, List.of(3L)));
        repository.save(embed(12L, GUILD_B, List.of(4L)));

        final List<ElectricityStatusEmbed> result = repository.getByGuildId(GUILD_A);

        assertEquals(Set.of(10L, 11L), messageIds(result));
    }

    @Test
    void getByGuildIdReturnsEmptyWhenGuildHasNoEmbeds() {
        repository.save(embed(10L, GUILD_A, List.of(1L)));

        assertTrue(repository.getByGuildId(GUILD_B).isEmpty());
    }

    @Test
    void saveUpdatesExistingEmbed() {
        repository.save(embed(10L, GUILD_A, List.of(1L)));
        repository.save(embed(10L, GUILD_A, List.of(1L, 2L)));

        final ElectricityStatusEmbed stored = repository.get(10L).orElseThrow();
        assertEquals(List.of(1L, 2L), stored.getParticipantUserIds());
        assertEquals(Set.of(10L), messageIds(repository.getByGuildId(GUILD_A)));
    }

    private static Set<Long> messageIds(final List<ElectricityStatusEmbed> embeds) {
        return embeds.stream().map(ElectricityStatusEmbed::getMessageId).collect(Collectors.toSet());
    }

    private static ElectricityStatusEmbed embed(final long messageId, final long guildId, final List<Long> userIds) {
        return new ElectricityStatusEmbed(messageId, guildId, 1L, userIds, Instant.parse("2025-05-01T00:00:00Z"));
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
