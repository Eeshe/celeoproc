package me.eeshe.celeoproc.repository.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Shared JDBC mapping helpers for types that the PostgreSQL driver does not
 * handle natively through
 * {@link java.sql.PreparedStatement#setObject(int, Object)}.
 */
final class JdbcTypeMapper {

    private JdbcTypeMapper() {
    }

    /**
     * Binds an {@link Instant} to a {@code TIMESTAMPTZ} parameter, or SQL
     * {@code NULL} when the instant is {@code null}.
     */
    static void setInstant(final PreparedStatement statement, final int index, final Instant instant)
            throws SQLException {
        if (instant == null) {
            statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
            return;
        }
        statement.setObject(index, instant.atOffset(ZoneOffset.UTC));
    }

    /**
     * Reads a {@code TIMESTAMPTZ} column as an {@link Instant}, or {@code null}
     * when the column is SQL {@code NULL}.
     */
    static Instant getInstant(final ResultSet resultSet, final String column) throws SQLException {
        final OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);

        return value == null ? null : value.toInstant();
    }
}
