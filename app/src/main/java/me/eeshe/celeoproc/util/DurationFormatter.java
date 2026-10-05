package me.eeshe.celeoproc.util;

import java.time.Duration;

/**
 * Formats durations in the compact {@code XdYhZm} format.
 */
public final class DurationFormatter {

    private DurationFormatter() {
    }

    /**
     * Formats the given duration as {@code XdYhZm}, omitting units whose value is
     * zero. Falls back to {@code 0m} for zero or negative durations.
     *
     * @param duration duration to format
     * @return formatted duration
     */
    public static String format(final Duration duration) {
        final long totalMinutes = Math.max(0L, duration.toMinutes());
        final long days = totalMinutes / (24 * 60);
        final long hours = (totalMinutes % (24 * 60)) / 60;
        final long minutes = totalMinutes % 60;

        final StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append('d');
        }
        if (hours > 0) {
            builder.append(hours).append('h');
        }
        if (minutes > 0 || builder.length() == 0) {
            builder.append(minutes).append('m');
        }
        return builder.toString();
    }
}
