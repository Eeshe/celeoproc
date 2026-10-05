package me.eeshe.celeoproc.repository.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.repository.RegistryChannelRepository;

public final class RegistryChannelRepositoryImpl implements RegistryChannelRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryChannelRepositoryImpl.class);

    private static final String TABLE = "registry_channel";
    private static final String COLUMN_EMBED_ID = "embed_id";
    private static final String COLUMN_CHANNEL_ID = "channel_id";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS %s (
                %s BIGINT PRIMARY KEY,
                %s BIGINT NOT NULL
            )""".formatted(
            TABLE,
            COLUMN_EMBED_ID,
            COLUMN_CHANNEL_ID);

    private static final String CREATE_CHANNEL_ID_INDEX_SQL = "CREATE INDEX IF NOT EXISTS %s_%s_idx ON %s (%s)"
            .formatted(TABLE, COLUMN_CHANNEL_ID, TABLE, COLUMN_CHANNEL_ID);

    private static final String SAVE_SQL = """
            INSERT INTO %s (%s, %s)
            VALUES (?, ?)
            ON CONFLICT (%s) DO UPDATE SET
                %s = EXCLUDED.%s""".formatted(
            TABLE,
            COLUMN_EMBED_ID,
            COLUMN_CHANNEL_ID,
            COLUMN_EMBED_ID,
            COLUMN_CHANNEL_ID,
            COLUMN_CHANNEL_ID);

    private static final String DELETE_SQL = "DELETE FROM %s WHERE %s = ?".formatted(
            TABLE,
            COLUMN_EMBED_ID);

    private static final String GET_BY_EMBED_SQL = "SELECT %s FROM %s WHERE %s = ?".formatted(
            COLUMN_CHANNEL_ID,
            TABLE,
            COLUMN_EMBED_ID);

    private static final String GET_BY_CHANNEL_SQL = "SELECT %s FROM %s WHERE %s = ?".formatted(
            COLUMN_EMBED_ID,
            TABLE,
            COLUMN_CHANNEL_ID);

    private final Database database;

    public RegistryChannelRepositoryImpl(final Database database) {
        this.database = Objects.requireNonNull(database, "Database must not be null");
    }

    @Override
    public void initialize() throws SQLException {
        LOGGER.info("Initializing '{}' table...", TABLE);
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL);
                PreparedStatement indexStatement = connection.prepareStatement(CREATE_CHANNEL_ID_INDEX_SQL)) {
            statement.executeUpdate();
            indexStatement.executeUpdate();
        }
        LOGGER.info("Successfully initialized '{}' table", TABLE);
    }

    @Override
    public void shutdown() throws SQLException {
        LOGGER.info("Shutting down '{}' repository", TABLE);
    }

    @Override
    public void save(final long embedId, final long channelId) {
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SAVE_SQL)) {
            statement.setLong(1, embedId);
            statement.setLong(2, channelId);
            statement.executeUpdate();
        } catch (final SQLException exception) {
            LOGGER.error("Failed to save registry channel for embed '{}'", embedId, exception);
        }
    }

    @Override
    public void delete(final long embedId) {
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, embedId);
            statement.executeUpdate();
        } catch (final SQLException exception) {
            LOGGER.error("Failed to delete registry channel for embed '{}'", embedId, exception);
        }
    }

    @Override
    public List<Long> getByEmbedId(final long embedId) {
        return queryIds(GET_BY_EMBED_SQL, embedId, "load registry channels for embed '%d'".formatted(embedId));
    }

    @Override
    public List<Long> getByChannelId(final long channelId) {
        return queryIds(GET_BY_CHANNEL_SQL, channelId,
                "load registry embeds for channel '%d'".formatted(channelId));
    }

    private List<Long> queryIds(final String sql, final long parameter, final String action) {
        final List<Long> ids = new ArrayList<>();
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parameter);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to {}", action, exception);
            return List.of();
        }
        return ids;
    }
}
