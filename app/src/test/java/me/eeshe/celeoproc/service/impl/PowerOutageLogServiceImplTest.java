package me.eeshe.celeoproc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.repository.PowerOutageLogRepository;

/**
 * Verifies that the service translates inclusive calendar days into the
 * timezone-aware {@link Instant} range handed to the repository.
 */
class PowerOutageLogServiceImplTest {

    private StubPowerOutageLogRepository repository;
    private PowerOutageLogServiceImpl service;

    @BeforeEach
    void setUp() {
        final AppSettings appSettings = new AppSettings();
        appSettings.load();

        repository = new StubPowerOutageLogRepository();
        service = new PowerOutageLogServiceImpl(repository, appSettings);
    }

    @Test
    void getWithinRangeSpansStartOfStartDayToEndOfEndDay() {
        final LocalDate start = LocalDate.of(2025, 5, 5);
        final LocalDate end = LocalDate.of(2025, 5, 10);

        service.getWithinRange(start, end);

        // America/New_York is UTC-4 during this range (EDT).
        assertEquals(Instant.parse("2025-05-05T04:00:00Z"), repository.rangeStart);
        assertEquals(Instant.parse("2025-05-11T03:59:59.999999999Z"), repository.rangeEnd);
    }

    @Test
    void getWithinRangeHandlesDaylightSavingTransition() {
        final LocalDate start = LocalDate.of(2025, 3, 9);
        final LocalDate end = LocalDate.of(2025, 3, 9);

        service.getWithinRange(start, end);

        // 2025-03-09 starts in EST (UTC-5) and ends in EDT (UTC-4).
        assertEquals(Instant.parse("2025-03-09T05:00:00Z"), repository.rangeStart);
        assertEquals(Instant.parse("2025-03-10T03:59:59.999999999Z"), repository.rangeEnd);
    }

    @Test
    void getWithinRangeReturnsRepositoryResult() {
        final PowerOutageLog log = new PowerOutageLog(
                UUID.randomUUID(),
                1L,
                Instant.parse("2025-05-06T10:00:00Z"),
                Instant.parse("2025-05-06T12:00:00Z"));
        repository.result = List.of(log);

        final List<PowerOutageLog> result = service.getWithinRange(LocalDate.of(2025, 5, 5), LocalDate.of(2025, 5, 10));

        assertEquals(List.of(log), result);
    }

    @Test
    void getWithinRangeRejectsNullDates() {
        assertThrows(NullPointerException.class, () -> service.getWithinRange(null, LocalDate.of(2025, 5, 10)));
        assertThrows(NullPointerException.class, () -> service.getWithinRange(LocalDate.of(2025, 5, 5), null));
    }

    /**
     * Hand-written stub because the project does not depend on a mocking library.
     */
    private static final class StubPowerOutageLogRepository implements PowerOutageLogRepository {
        private Instant rangeStart;
        private Instant rangeEnd;
        private List<PowerOutageLog> result = List.of();

        @Override
        public void initialize() throws SQLException {
        }

        @Override
        public void shutdown() throws SQLException {
        }

        @Override
        public void save(final PowerOutageLog powerOutageLog) {
        }

        @Override
        public List<PowerOutageLog> getWithinRange(final Instant rangeStart, final Instant rangeEnd) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            return result;
        }
    }
}
