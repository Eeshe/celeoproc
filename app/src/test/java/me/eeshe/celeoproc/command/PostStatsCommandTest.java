package me.eeshe.celeoproc.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Verifies the default date handling of the optional stats range.
 */
class PostStatsCommandTest {

    @Test
    void firstDayOfMonthReturnsDayOne() {
        assertEquals(LocalDate.of(2026, 1, 1),
                PostStatsCommand.firstDayOfMonth(LocalDate.of(2026, 1, 18)));
    }

    @Test
    void lastDayOfMonthHandlesLeapFebruary() {
        assertEquals(LocalDate.of(2024, 2, 29),
                PostStatsCommand.lastDayOfMonth(LocalDate.of(2024, 2, 10)));
    }

    @Test
    void blankInputUsesTheFallback() {
        final LocalDate fallback = LocalDate.of(2026, 3, 1);

        assertEquals(fallback, PostStatsCommand.resolveDate("", fallback));
        assertEquals(fallback, PostStatsCommand.resolveDate("   ", fallback));
        assertEquals(fallback, PostStatsCommand.resolveDate(null, fallback));
    }

    @Test
    void explicitInputIsParsed() {
        assertEquals(LocalDate.of(2026, 4, 12),
                PostStatsCommand.resolveDate("2026-04-12", LocalDate.of(2026, 3, 1)));
    }

    @Test
    void invalidInputReturnsNull() {
        assertNull(PostStatsCommand.resolveDate("not-a-date", LocalDate.of(2026, 3, 1)));
    }

    @Test
    void defaultedRangeStaysValid() {
        final LocalDate today = LocalDate.of(2026, 4, 18);
        final LocalDate startDate = PostStatsCommand.firstDayOfMonth(today);
        final LocalDate endDate = PostStatsCommand.lastDayOfMonth(today);

        assertFalse(startDate.isAfter(endDate));
    }
}
