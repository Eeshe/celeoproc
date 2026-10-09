package me.eeshe.celeoproc.repository.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.repository.PowerOutageLogRepository;

public final class PowerOutageLogRepositoryImpl implements PowerOutageLogRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowerOutageLogRepositoryImpl.class);

    private static final String TABLE = "power_outage_log";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_ELECTRICITY_OUT = "electricity_out";
    private static final String COLUMN_ELECTRICITY_IN = "electricity_in";

    private static final String SELECT_COLUMNS = "%s, %s, %s, %s".formatted(
            COLUMN_ID,
            COLUMN_USER_ID,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_IN);

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS %s (
                %s UUID PRIMARY KEY,
                %s BIGINT NOT NULL,
                %s TIMESTAMPTZ NOT NULL,
                %s TIMESTAMPTZ NOT NULL
            )""".formatted(
            TABLE,
            COLUMN_ID,
            COLUMN_USER_ID,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_IN);

    private static final String CREATE_RANGE_INDEX_SQL = "CREATE INDEX IF NOT EXISTS %s_%s_%s_idx ON %s (%s, %s)"
            .formatted(TABLE, COLUMN_ELECTRICITY_OUT, COLUMN_ELECTRICITY_IN, TABLE,
                    COLUMN_ELECTRICITY_OUT, COLUMN_ELECTRICITY_IN);

    private static final String SAVE_SQL = """
            INSERT INTO %s (%s)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (%s) DO UPDATE SET
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s""".formatted(
            TABLE,
            SELECT_COLUMNS,
            COLUMN_ID,
            COLUMN_USER_ID, COLUMN_USER_ID,
            COLUMN_ELECTRICITY_OUT, COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_IN, COLUMN_ELECTRICITY_IN);

    private static final String GET_BY_ID_SQL = "SELECT %s FROM %s WHERE %s = ?".formatted(
            SELECT_COLUMNS,
            TABLE,
            COLUMN_ID);

    private static final String DELETE_SQL = "DELETE FROM %s WHERE %s = ?".formatted(
            TABLE,
            COLUMN_ID);

    private static final String GET_LOGS_SQL = """
            SELECT %s FROM %s
            WHERE %s <= ? AND %s >= ?
            ORDER BY %s""".formatted(
            SELECT_COLUMNS,
            TABLE,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_IN,
            COLUMN_ELECTRICITY_OUT);

    private final Database database;

    public PowerOutageLogRepositoryImpl(final Database database) {
        this.database = Objects.requireNonNull(database, "Database must not be null");
    }

    @Override
    public void initialize() throws SQLException {
        LOGGER.info("Initializing '{}' table...", TABLE);
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL);
                PreparedStatement indexStatement = connection.prepareStatement(CREATE_RANGE_INDEX_SQL)) {
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
    public void save(final PowerOutageLog powerOutageLog) {
        Objects.requireNonNull(powerOutageLog, "PowerOutageLog must not be null");

        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SAVE_SQL)) {
            statement.setObject(1, powerOutageLog.id());
            statement.setLong(2, powerOutageLog.userId());
            JdbcTypeMapper.setInstant(statement, 3, powerOutageLog.electricityOut());
            JdbcTypeMapper.setInstant(statement, 4, powerOutageLog.electricityIn());

            statement.executeUpdate();
        } catch (final SQLException exception) {
            LOGGER.error("Failed to save power outage log '{}'", powerOutageLog.id(), exception);
        }
    }

    @Override
    public Optional<PowerOutageLog> getById(final UUID id) {
        Objects.requireNonNull(id, "Id must not be null");

        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(GET_BY_ID_SQL)) {
            statement.setObject(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to load power outage log '{}'", id, exception);
        }
        return Optional.empty();
    }

    @Override
    public void delete(final UUID id) {
        Objects.requireNonNull(id, "Id must not be null");

        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setObject(1, id);

            statement.executeUpdate();
        } catch (final SQLException exception) {
            LOGGER.error("Failed to delete power outage log '{}'", id, exception);
        }
    }

    @Override
    public List<PowerOutageLog> getWithinRange(final Instant rangeStart, final Instant rangeEnd) {
        Objects.requireNonNull(rangeStart, "Range start must not be null");
        Objects.requireNonNull(rangeEnd, "Range end must not be null");

        final List<PowerOutageLog> powerOutageLogs = new ArrayList<>();
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(GET_LOGS_SQL)) {
            JdbcTypeMapper.setInstant(statement, 1, rangeEnd);
            JdbcTypeMapper.setInstant(statement, 2, rangeStart);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    powerOutageLogs.add(mapRow(resultSet));
                }
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to load power outage logs between '{}' and '{}'", rangeStart, rangeEnd, exception);
            return List.of();
        }
        return powerOutageLogs;
    }

    private PowerOutageLog mapRow(final ResultSet resultSet) throws SQLException {
        return new PowerOutageLog(
                resultSet.getObject(COLUMN_ID, UUID.class),
                resultSet.getLong(COLUMN_USER_ID),
                JdbcTypeMapper.getInstant(resultSet, COLUMN_ELECTRICITY_OUT),
                JdbcTypeMapper.getInstant(resultSet, COLUMN_ELECTRICITY_IN));
    }
}
