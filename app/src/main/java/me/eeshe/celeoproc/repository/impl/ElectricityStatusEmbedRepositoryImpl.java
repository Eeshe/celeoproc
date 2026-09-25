package me.eeshe.celeoproc.repository.impl;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.repository.ElectricityStatusEmbedRepository;

public final class ElectricityStatusEmbedRepositoryImpl implements ElectricityStatusEmbedRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityStatusEmbedRepositoryImpl.class);

    private static final String TABLE = "electricity_status_embed";
    private static final String COLUMN_MESSAGE_ID = "message_id";
    private static final String COLUMN_GUILD_ID = "guild_id";
    private static final String COLUMN_CHANNEL_ID = "channel_id";
    private static final String COLUMN_PARTICIPANT_USER_IDS = "participant_user_ids";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    private static final String ARRAY_TYPE = "bigint";

    private static final String SELECT_COLUMNS = "%s, %s, %s, %s, %s".formatted(
            COLUMN_MESSAGE_ID,
            COLUMN_GUILD_ID,
            COLUMN_CHANNEL_ID,
            COLUMN_PARTICIPANT_USER_IDS,
            COLUMN_UPDATED_AT);

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS %s (
                %s BIGINT PRIMARY KEY,
                %s BIGINT NOT NULL,
                %s BIGINT NOT NULL,
                %s BIGINT[] NOT NULL DEFAULT '{}',
                %s TIMESTAMPTZ NOT NULL
            )""".formatted(
            TABLE,
            COLUMN_MESSAGE_ID,
            COLUMN_GUILD_ID,
            COLUMN_CHANNEL_ID,
            COLUMN_PARTICIPANT_USER_IDS,
            COLUMN_UPDATED_AT);

    private static final String SAVE_SQL = """
            INSERT INTO %s (%s)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (%s) DO UPDATE SET
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s""".formatted(
            TABLE,
            SELECT_COLUMNS,
            COLUMN_MESSAGE_ID,
            COLUMN_GUILD_ID,
            COLUMN_GUILD_ID,
            COLUMN_CHANNEL_ID,
            COLUMN_CHANNEL_ID,
            COLUMN_PARTICIPANT_USER_IDS,
            COLUMN_PARTICIPANT_USER_IDS,
            COLUMN_UPDATED_AT,
            COLUMN_UPDATED_AT);

    private static final String GET_SQL = "SELECT %s FROM %s WHERE %s = ?".formatted(
            SELECT_COLUMNS,
            TABLE,
            COLUMN_MESSAGE_ID);

    private static final String GET_ALL_SQL = "SELECT %s FROM %s".formatted(
            SELECT_COLUMNS,
            TABLE);

    private static final String DELETE_SQL = "DELETE FROM %s WHERE %s = ?".formatted(
            TABLE,
            COLUMN_MESSAGE_ID);

    private final Database database;

    public ElectricityStatusEmbedRepositoryImpl(final Database database) {
        this.database = Objects.requireNonNull(database, "Database must not be null");
    }

    @Override
    public void initialize() throws SQLException {
        LOGGER.info("Initializing '{}' table...", TABLE);
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL)) {
            statement.executeUpdate();
        }
        LOGGER.info("Successfully initialized '{}' table", TABLE);
    }

    @Override
    public void shutdown() throws SQLException {
        LOGGER.info("Shutting down '{}' repository", TABLE);
    }

    @Override
    public void save(final ElectricityStatusEmbed statusEmbed) {
        Objects.requireNonNull(statusEmbed, "ElectricityStatusEmbed must not be null");

        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SAVE_SQL)) {
            statement.setLong(1, statusEmbed.getMessageId());
            statement.setLong(2, statusEmbed.getGuildId());
            statement.setLong(3, statusEmbed.getChannelId());

            final Array participants = connection.createArrayOf(
                    ARRAY_TYPE,
                    statusEmbed.getParticipantUserIds().toArray());
            try {
                statement.setArray(4, participants);
                statement.setObject(5, statusEmbed.getUpdatedAt().atOffset(ZoneOffset.UTC));
                statement.executeUpdate();
            } finally {
                participants.free();
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to save embed for message '{}'", statusEmbed.getMessageId(), exception);
        }
    }

    @Override
    public Optional<ElectricityStatusEmbed> get(final long messageId) {
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(GET_SQL)) {
            statement.setLong(1, messageId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(resultSet));
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to load embed for message '{}'", messageId, exception);
            return Optional.empty();
        }
    }

    @Override
    public List<ElectricityStatusEmbed> getAll() {
        final List<ElectricityStatusEmbed> statusEmbeds = new ArrayList<>();
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(GET_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                statusEmbeds.add(mapRow(resultSet));
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to load embeds", exception);
            return List.of();
        }
        return statusEmbeds;
    }

    @Override
    public void delete(final long messageId) {
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, messageId);
            statement.executeUpdate();
        } catch (final SQLException exception) {
            LOGGER.error("Failed to delete embed for message '{}'", messageId, exception);
        }
    }

    private ElectricityStatusEmbed mapRow(final ResultSet resultSet) throws SQLException {
        return new ElectricityStatusEmbed(
                resultSet.getLong(COLUMN_MESSAGE_ID),
                resultSet.getLong(COLUMN_GUILD_ID),
                resultSet.getLong(COLUMN_CHANNEL_ID),
                getParticipantUserIds(resultSet, COLUMN_PARTICIPANT_USER_IDS),
                JdbcTypeMapper.getInstant(resultSet, COLUMN_UPDATED_AT));
    }

    private static List<Long> getParticipantUserIds(final ResultSet resultSet, final String column)
            throws SQLException {
        final Array array = resultSet.getArray(column);
        if (array == null) {
            return List.of();
        }
        try {
            final Object raw = array.getArray();
            if (raw instanceof Long[] userIds) {
                return List.of(userIds);
            }
            return List.of();
        } finally {
            array.free();
        }
    }
}
