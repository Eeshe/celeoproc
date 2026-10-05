package me.eeshe.celeoproc.repository.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.model.UserElectricityStatus;
import me.eeshe.celeoproc.repository.UserElectricityStatusRepository;

public final class UserElectricityStatusRepositoryImpl implements UserElectricityStatusRepository {
    private final Logger LOGGER = LoggerFactory.getLogger(UserElectricityStatusRepositoryImpl.class);

    private final String TABLE = "user_electricity_status";
    private final String COLUMN_USER_ID = "user_id";
    private final String COLUMN_NICKNAME = "nickname";
    private final String COLUMN_ELECTRICITY_IN = "electricity_in";
    private final String COLUMN_ELECTRICITY_OUT = "electricity_out";
    private final String COLUMN_ELECTRICITY_IN_ESTIMATE = "electricity_in_estimate";
    private final String COLUMN_LAST_REMINDER_AT = "last_reminder_at";

    private final String SELECT_COLUMNS = "%s, %s, %s, %s, %s, %s".formatted(
            COLUMN_USER_ID,
            COLUMN_NICKNAME,
            COLUMN_ELECTRICITY_IN,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_IN_ESTIMATE,
            COLUMN_LAST_REMINDER_AT);

    private final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS %s (
                %s BIGINT PRIMARY KEY,
                %s TEXT NOT NULL,
                %s TIMESTAMPTZ,
                %s TIMESTAMPTZ,
                %s TIMESTAMPTZ,
                %s TIMESTAMPTZ
            )""".formatted(
            TABLE,
            COLUMN_USER_ID,
            COLUMN_NICKNAME,
            COLUMN_ELECTRICITY_IN,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_IN_ESTIMATE,
            COLUMN_LAST_REMINDER_AT);

    private final String ADD_LAST_REMINDER_SQL = "ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s TIMESTAMPTZ"
            .formatted(TABLE, COLUMN_LAST_REMINDER_AT);

    private final String SAVE_SQL = """
            INSERT INTO %s (%s)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (%s) DO UPDATE SET
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s,
                %s = EXCLUDED.%s""".formatted(
            TABLE,
            SELECT_COLUMNS,
            COLUMN_USER_ID,
            COLUMN_NICKNAME,
            COLUMN_NICKNAME,
            COLUMN_ELECTRICITY_IN,
            COLUMN_ELECTRICITY_IN,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_IN_ESTIMATE,
            COLUMN_ELECTRICITY_IN_ESTIMATE,
            COLUMN_LAST_REMINDER_AT,
            COLUMN_LAST_REMINDER_AT);

    private final String GET_SQL = "SELECT %s FROM %s WHERE %s = ?".formatted(
            SELECT_COLUMNS,
            TABLE,
            COLUMN_USER_ID);

    private final String GET_PENDING_REMINDERS_SQL = """
            SELECT %s FROM %s
            WHERE %s IS NOT NULL
                AND (%s IS NULL OR %s <= %s)
                AND (%s IS NULL OR %s <= ?)""".formatted(
            SELECT_COLUMNS,
            TABLE,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_ELECTRICITY_IN,
            COLUMN_ELECTRICITY_IN,
            COLUMN_ELECTRICITY_OUT,
            COLUMN_LAST_REMINDER_AT,
            COLUMN_LAST_REMINDER_AT);

    private final String CREATE_LAST_REMINDER_INDEX_SQL = "CREATE INDEX IF NOT EXISTS %s_%s_idx ON %s (%s)"
            .formatted(TABLE, COLUMN_LAST_REMINDER_AT, TABLE, COLUMN_LAST_REMINDER_AT);

    private final Database database;

    public UserElectricityStatusRepositoryImpl(final Database database) {
        this.database = Objects.requireNonNull(database, "Database must not be null");
    }

    @Override
    public void initialize() throws SQLException {
        LOGGER.info("Initializing '{}' table...", TABLE);
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL);
                PreparedStatement addLastReminderStatement = connection
                        .prepareStatement(ADD_LAST_REMINDER_SQL);
                PreparedStatement lastReminderIndexStatement = connection
                        .prepareStatement(CREATE_LAST_REMINDER_INDEX_SQL)) {
            statement.executeUpdate();
            addLastReminderStatement.executeUpdate();
            lastReminderIndexStatement.executeUpdate();
        }
        LOGGER.info("Successfully initialized '{}' table", TABLE);
    }

    @Override
    public void shutdown() throws SQLException {
        LOGGER.info("Shutting down '{}' repository", TABLE);
    }

    @Override
    public void save(final UserElectricityStatus status) {
        Objects.requireNonNull(status, "UserElectricityStatus must not be null");

        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SAVE_SQL)) {
            statement.setLong(1, status.getUserId());
            statement.setString(2, status.getNickname());

            JdbcTypeMapper.setInstant(statement, 3, status.getElectricityIn());
            JdbcTypeMapper.setInstant(statement, 4, status.getElectricityOut());
            JdbcTypeMapper.setInstant(statement, 5, status.getElectricityInEstimate());
            JdbcTypeMapper.setInstant(statement, 6, status.getLastReminderAt());

            statement.executeUpdate();
        } catch (final SQLException exception) {
            LOGGER.error("Failed to save status for user '{}'", status.getUserId(), exception);
        }
    }

    @Override
    public Optional<UserElectricityStatus> get(final long userId) {
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(GET_SQL)) {
            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(resultSet));
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to load status for user '{}'", userId, exception);
            return Optional.empty();
        }
    }

    @Override
    public List<UserElectricityStatus> get(final Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        final Set<Long> uniqueIds = new LinkedHashSet<>(userIds.stream().filter(Objects::nonNull).toList());
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        final String placeholders = String.join(", ", Collections.nCopies(uniqueIds.size(), "?"));
        final String sql = "SELECT %s FROM %s WHERE %s IN (%s)"
                .formatted(SELECT_COLUMNS, TABLE, COLUMN_USER_ID, placeholders);

        final List<UserElectricityStatus> statuses = new ArrayList<>();
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (final Long userId : uniqueIds) {
                statement.setLong(index++, userId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    statuses.add(mapRow(resultSet));
                }
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to load statuses for {} user(s)", uniqueIds.size(), exception);
            return List.of();
        }
        return statuses;
    }

    @Override
    public List<UserElectricityStatus> getPendingReminders(final Instant lastReminderOrBefore) {
        Objects.requireNonNull(lastReminderOrBefore, "Last reminder must not be null");

        final List<UserElectricityStatus> statuses = new ArrayList<>();
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(GET_PENDING_REMINDERS_SQL)) {
            JdbcTypeMapper.setInstant(statement, 1, lastReminderOrBefore);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    statuses.add(mapRow(resultSet));
                }
            }
        } catch (final SQLException exception) {
            LOGGER.error("Failed to load pending reminders", exception);
            return List.of();
        }
        return statuses;
    }

    private UserElectricityStatus mapRow(final ResultSet resultSet) throws SQLException {
        final Instant electricityIn = JdbcTypeMapper.getInstant(resultSet, COLUMN_ELECTRICITY_IN);
        final Instant electricityOut = JdbcTypeMapper.getInstant(resultSet, COLUMN_ELECTRICITY_OUT);
        final Instant electricityInEstimate = JdbcTypeMapper.getInstant(resultSet, COLUMN_ELECTRICITY_IN_ESTIMATE);
        final Instant lastReminderAt = JdbcTypeMapper.getInstant(resultSet, COLUMN_LAST_REMINDER_AT);

        return new UserElectricityStatus(
                resultSet.getLong(COLUMN_USER_ID),
                resultSet.getString(COLUMN_NICKNAME),
                electricityIn,
                electricityOut,
                electricityInEstimate,
                lastReminderAt);
    }
}
