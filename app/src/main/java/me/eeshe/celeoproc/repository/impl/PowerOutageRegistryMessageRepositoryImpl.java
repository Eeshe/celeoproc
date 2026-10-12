package me.eeshe.celeoproc.repository.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.model.PowerOutageRegistryMessage;
import me.eeshe.celeoproc.repository.PowerOutageRegistryMessageRepository;

public final class PowerOutageRegistryMessageRepositoryImpl implements PowerOutageRegistryMessageRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowerOutageRegistryMessageRepositoryImpl.class);

    private static final String TABLE = "power_outage_registry_message";
    private static final String COLUMN_REGISTRY_OUT_MESSAGE_ID = "registry_out_message_id";
    private static final String COLUMN_REGISTRY_IN_MESSAGE_ID = "registry_in_message_id";
    private static final String COLUMN_POWER_OUTAGE_LOG_ID = "power_outage_log_id";
    private static final String COLUMN_CHANNEL_ID = "channel_id";

    private static final String SELECT_COLUMNS = "%s, %s, %s, %s".formatted(
            COLUMN_REGISTRY_OUT_MESSAGE_ID,
            COLUMN_REGISTRY_IN_MESSAGE_ID,
            COLUMN_POWER_OUTAGE_LOG_ID,
            COLUMN_CHANNEL_ID);

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS %s (
                %s BIGINT PRIMARY KEY,
                %s BIGINT,
                %s UUID NOT NULL REFERENCES power_outage_log(id) ON DELETE CASCADE,
                %s BIGINT NOT NULL
            )""".formatted(
            TABLE,
            COLUMN_REGISTRY_OUT_MESSAGE_ID,
            COLUMN_REGISTRY_IN_MESSAGE_ID,
            COLUMN_POWER_OUTAGE_LOG_ID,
            COLUMN_CHANNEL_ID);

    private static final String CREATE_LOG_ID_INDEX_SQL = "CREATE INDEX IF NOT EXISTS %s_%s_idx ON %s (%s)"
            .formatted(TABLE, COLUMN_POWER_OUTAGE_LOG_ID, TABLE, COLUMN_POWER_OUTAGE_LOG_ID);

    private static final String SAVE_SQL = """
            INSERT INTO %s (%s)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (%s) DO UPDATE SET
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s""".formatted(
            TABLE,
            SELECT_COLUMNS,
            COLUMN_REGISTRY_OUT_MESSAGE_ID,
            COLUMN_REGISTRY_IN_MESSAGE_ID, COLUMN_REGISTRY_IN_MESSAGE_ID,
            COLUMN_POWER_OUTAGE_LOG_ID, COLUMN_POWER_OUTAGE_LOG_ID,
            COLUMN_CHANNEL_ID, COLUMN_CHANNEL_ID);

    private static final String UPDATE_IN_MESSAGE_SQL = "UPDATE %s SET %s = ? WHERE %s = ?".formatted(
            TABLE,
            COLUMN_REGISTRY_IN_MESSAGE_ID,
            COLUMN_REGISTRY_OUT_MESSAGE_ID);

    private static final String GET_BY_LOG_ID_SQL = "SELECT %s FROM %s WHERE %s = ?".formatted(
            SELECT_COLUMNS,
            TABLE,
            COLUMN_POWER_OUTAGE_LOG_ID);

    private final Database database;

    public PowerOutageRegistryMessageRepositoryImpl(final Database database) {
        this.database = Objects.requireNonNull(database, "Database must not be null");
    }

    @Override
    public void initialize() throws SQLException {
        LOGGER.info("Initializing '{}' table...", TABLE);
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL);
                PreparedStatement indexStatement = connection.prepareStatement(CREATE_LOG_ID_INDEX_SQL)) {
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
    public void saveAll(final List<PowerOutageRegistryMessage> registryMessages) {
        if (registryMessages == null || registryMessages.isEmpty()) {
            return;
        }
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SAVE_SQL)) {
            for (final PowerOutageRegistryMessage registryMessage : registryMessages) {
                statement.setLong(1, registryMessage.registryOutMessageId());
                setNullableLong(statement, 2, registryMessage.registryInMessageId());
                statement.setObject(3, registryMessage.powerOutageLogId());
                statement.setLong(4, registryMessage.channelId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (final SQLException exception) {
            LOGGER.error("Failed to save {} power outage registry message(s)", registryMessages.size(), exception);
        }
    }

    @Override
    public void updateAll(final List<PowerOutageRegistryMessage> registryMessages) {
        if (registryMessages == null || registryMessages.isEmpty()) {
            return;
        }
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_IN_MESSAGE_SQL)) {
            for (final PowerOutageRegistryMessage registryMessage : registryMessages) {
                if (registryMessage.registryInMessageId() == null) {
                    continue;
                }
                statement.setLong(1, registryMessage.registryInMessageId());
                statement.setLong(2, registryMessage.registryOutMessageId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (final SQLException exception) {
            LOGGER.error("Failed to update {} power outage registry message(s)", registryMessages.size(), exception);
        }
    }

    @Override
    public List<PowerOutageRegistryMessage> getByPowerOutageLogId(final UUID powerOutageLogId) {
        Objects.requireNonNull(powerOutageLogId, "Power outage log id must not be null");

        final List<PowerOutageRegistryMessage> registryMessages = new ArrayList<>();
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(GET_BY_LOG_ID_SQL)) {
            statement.setObject(1, powerOutageLogId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    registryMessages.add(mapRow(resultSet));
                }
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to load registry messages for power outage log '{}'", powerOutageLogId, exception);
            return List.of();
        }
        return registryMessages;
    }

    private void setNullableLong(final PreparedStatement statement, final int index, final Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value);
    }

    private PowerOutageRegistryMessage mapRow(final ResultSet resultSet) throws SQLException {
        final long registryInMessageId = resultSet.getLong(COLUMN_REGISTRY_IN_MESSAGE_ID);
        final Long nullableRegistryInMessageId = resultSet.wasNull() ? null : registryInMessageId;

        return new PowerOutageRegistryMessage(
                resultSet.getLong(COLUMN_REGISTRY_OUT_MESSAGE_ID),
                nullableRegistryInMessageId,
                resultSet.getObject(COLUMN_POWER_OUTAGE_LOG_ID, UUID.class),
                resultSet.getLong(COLUMN_CHANNEL_ID));
    }
}
