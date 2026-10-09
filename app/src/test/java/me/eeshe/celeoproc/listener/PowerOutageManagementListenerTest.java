package me.eeshe.celeoproc.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Verifies that the edit modal date format accepts the AM/PM marker both with
 * and without a separating space, while still formatting with a space.
 */
class PowerOutageManagementListenerTest {

    @Test
    void parsesAmPmWithSpace() {
        assertEquals(LocalDateTime.of(2025, 5, 6, 12, 10),
                LocalDateTime.parse("2025-05-06 12:10 PM", PowerOutageManagementListener.MODAL_DATE_FORMATTER));
        assertEquals(LocalDateTime.of(2025, 5, 6, 0, 10),
                LocalDateTime.parse("2025-05-06 12:10 AM", PowerOutageManagementListener.MODAL_DATE_FORMATTER));
    }

    @Test
    void parsesAmPmWithoutSpace() {
        assertEquals(LocalDateTime.of(2025, 5, 6, 12, 10),
                LocalDateTime.parse("2025-05-06 12:10PM", PowerOutageManagementListener.MODAL_DATE_FORMATTER));
        assertEquals(LocalDateTime.of(2025, 5, 6, 0, 10),
                LocalDateTime.parse("2025-05-06 12:10AM", PowerOutageManagementListener.MODAL_DATE_FORMATTER));
    }

    @Test
    void parsesAmPmCaseInsensitively() {
        assertEquals(LocalDateTime.of(2025, 5, 6, 12, 10),
                LocalDateTime.parse("2025-05-06 12:10pm", PowerOutageManagementListener.MODAL_DATE_FORMATTER));
    }

    @Test
    void formatsWithSeparatingSpace() {
        assertEquals("2025-05-06 02:30 PM",
                LocalDateTime.of(2025, 5, 6, 14, 30).format(PowerOutageManagementListener.MODAL_DATE_FORMATTER));
    }
}
