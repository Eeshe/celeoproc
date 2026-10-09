package me.eeshe.celeoproc.util;

import java.time.Instant;
import java.util.Objects;

/**
 * Formats instants as Discord timestamps.
 */
public final class TimestampFormatter {

    /**
     * Formats the given instant as a Discord short-time timestamp
     * ({@code <t:epoch:t>}).
     *
     * @param instant instant to format
     * @return formatted timestamp
     */
    public static String format(final Instant instant) {
        Objects.requireNonNull(instant, "Instant must not be null");

        return "<t:%d:t>".formatted(instant.getEpochSecond());
    }

    /**
     * Formats the given instant as a Discord relative timestamp
     * ({@code <t:epoch:R>}).
     *
     * @param instant instant to format
     * @return formatted relative timestamp
     */
    public static String formatRelative(final Instant instant) {
        Objects.requireNonNull(instant, "Instant must not be null");

        return "<t:%d:R>".formatted(instant.getEpochSecond());
    }
}
