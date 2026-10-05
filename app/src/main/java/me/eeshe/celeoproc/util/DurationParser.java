package me.eeshe.celeoproc.util;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses human readable durations in the {@code XhYmZs} format.
 *
 * <p>
 * Each unit is optional but at least one must be present, so values such as
 * {@code 6h}, {@code 30m}, {@code 1h30m} and {@code 1h30m20s} are accepted. A
 * bare number such as {@code 6} is not.
 */
public final class DurationParser {
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$",
            Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    /**
     * Parses the given {@code XhYmZs} value into a {@link Duration}.
     *
     * @param value value to parse
     * @throws DateTimeParseException when the value is null, blank, has no unit, or
     *                                is otherwise not a valid duration
     * @return parsed duration
     */
    public static Duration parse(final String value) {
        if (value == null || value.isBlank()) {
            throw new DateTimeParseException("Duration must not be blank", String.valueOf(value), 0);
        }

        final String trimmed = value.trim();
        final Matcher matcher = DURATION_PATTERN.matcher(trimmed);
        if (!matcher.matches() || isAllEmpty(matcher)) {
            throw new DateTimeParseException("Invalid duration '%s'".formatted(trimmed), trimmed, 0);
        }

        try {
            return Duration.ofHours(parseGroup(matcher, 1))
                    .plusMinutes(parseGroup(matcher, 2))
                    .plusSeconds(parseGroup(matcher, 3));
        } catch (final NumberFormatException | ArithmeticException exception) {
            throw new DateTimeParseException("Duration '%s' is out of range".formatted(trimmed), trimmed, 0,
                    exception);
        }
    }

    private static boolean isAllEmpty(final Matcher matcher) {
        return matcher.group(1) == null && matcher.group(2) == null && matcher.group(3) == null;
    }

    private static long parseGroup(final Matcher matcher, final int group) {
        final String value = matcher.group(group);
        return value == null ? 0L : Long.parseLong(value);
    }
}
