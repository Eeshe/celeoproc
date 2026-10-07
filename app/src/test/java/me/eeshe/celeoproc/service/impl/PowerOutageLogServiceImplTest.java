package me.eeshe.celeoproc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.PowerOutageStats;
import me.eeshe.celeoproc.repository.PowerOutageLogRepository;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Verifies that the service translates inclusive calendar days into the
 * timezone-aware {@link Instant} range handed to the repository and partitions
 * the logs by the guild's electricity status embed participants.
 */
class PowerOutageLogServiceImplTest {

    private static final long GUILD_ID = 100L;

    private StubPowerOutageLogRepository repository;
    private StubElectricityStatusEmbedService electricityStatusEmbedService;
    private PowerOutageLogServiceImpl service;

    @BeforeEach
    void setUp() {
        final AppSettings appSettings = new AppSettings();
        appSettings.load();

        repository = new StubPowerOutageLogRepository();
        electricityStatusEmbedService = new StubElectricityStatusEmbedService();
        service = new PowerOutageLogServiceImpl(repository, electricityStatusEmbedService, appSettings);
    }

    @Test
    void getWithinRangeSpansStartOfStartDayToEndOfEndDay() {
        final LocalDate start = LocalDate.of(2025, 5, 5);
        final LocalDate end = LocalDate.of(2025, 5, 10);

        service.getWithinRange(GUILD_ID, start, end);

        // America/New_York is UTC-4 during this range (EDT).
        assertEquals(Instant.parse("2025-05-05T04:00:00Z"), repository.rangeStart);
        assertEquals(Instant.parse("2025-05-11T03:59:59.999999999Z"), repository.rangeEnd);
    }

    @Test
    void getWithinRangeHandlesDaylightSavingTransition() {
        final LocalDate start = LocalDate.of(2025, 3, 9);
        final LocalDate end = LocalDate.of(2025, 3, 9);

        service.getWithinRange(GUILD_ID, start, end);

        // 2025-03-09 starts in EST (UTC-5) and ends in EDT (UTC-4).
        assertEquals(Instant.parse("2025-03-09T05:00:00Z"), repository.rangeStart);
        assertEquals(Instant.parse("2025-03-10T03:59:59.999999999Z"), repository.rangeEnd);
    }

    @Test
    void getWithinRangeFiltersParticipantsAcrossEveryGuildEmbed() {
        electricityStatusEmbedService.addEmbed(guildEmbed(GUILD_ID, 1L, 2L));
        electricityStatusEmbedService.addEmbed(guildEmbed(GUILD_ID, 3L));
        final PowerOutageLog participantOne = log(1L);
        final PowerOutageLog participantThree = log(3L);
        final PowerOutageLog outsider = log(99L);
        repository.result = List.of(participantOne, outsider, participantThree);

        final PowerOutageStats stats = service.getWithinRange(GUILD_ID, LocalDate.of(2025, 5, 5),
                LocalDate.of(2025, 5, 10));

        assertEquals(List.of(participantOne, participantThree), stats.guildLogs());
        assertEquals(List.of(participantOne, outsider, participantThree), stats.globalLogs());
    }

    @Test
    void getWithinRangeIgnoresOtherGuildsEmbeds() {
        electricityStatusEmbedService.addEmbed(guildEmbed(GUILD_ID, 1L));
        electricityStatusEmbedService.addEmbed(guildEmbed(200L, 42L));
        final PowerOutageLog guildParticipant = log(1L);
        final PowerOutageLog otherGuildParticipant = log(42L);
        repository.result = List.of(guildParticipant, otherGuildParticipant);

        final PowerOutageStats stats = service.getWithinRange(GUILD_ID, LocalDate.of(2025, 5, 5),
                LocalDate.of(2025, 5, 10));

        assertEquals(List.of(guildParticipant), stats.guildLogs());
        assertEquals(List.of(guildParticipant, otherGuildParticipant), stats.globalLogs());
    }

    @Test
    void getWithinRangeWithoutEmbedsKeepsGlobalLogsOnly() {
        final PowerOutageLog log = log(1L);
        repository.result = List.of(log);

        final PowerOutageStats stats = service.getWithinRange(GUILD_ID, LocalDate.of(2025, 5, 5),
                LocalDate.of(2025, 5, 10));

        assertTrue(stats.guildLogs().isEmpty());
        assertEquals(List.of(log), stats.globalLogs());
    }

    @Test
    void getWithinRangeRejectsNullDates() {
        assertThrows(NullPointerException.class,
                () -> service.getWithinRange(GUILD_ID, null, LocalDate.of(2025, 5, 10)));
        assertThrows(NullPointerException.class,
                () -> service.getWithinRange(GUILD_ID, LocalDate.of(2025, 5, 5), null));
    }

    private static PowerOutageLog log(final long userId) {
        return new PowerOutageLog(
                UUID.randomUUID(),
                userId,
                Instant.parse("2025-05-06T10:00:00Z"),
                Instant.parse("2025-05-06T12:00:00Z"));
    }

    private static ElectricityStatusEmbed guildEmbed(final long guildId, final Long... participantUserIds) {
        return new ElectricityStatusEmbed(
                1L,
                guildId,
                1L,
                List.of(participantUserIds),
                Instant.parse("2025-05-01T00:00:00Z"));
    }

    /**
     * Hand-written stubs because the project does not depend on a mocking library.
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

    private static final class StubElectricityStatusEmbedService implements ElectricityStatusEmbedService {
        private final List<ElectricityStatusEmbed> embeds = new ArrayList<>();

        private void addEmbed(final ElectricityStatusEmbed embed) {
            embeds.add(embed);
        }

        @Override
        public boolean sendElectricityStatusEmbed(final TextChannel channel) {
            return true;
        }

        @Override
        public Optional<String> getJumpUrl(final long messageId) {
            return Optional.empty();
        }

        @Override
        public List<ElectricityStatusEmbed> getByParticipantId(final long userId) {
            return embeds.stream()
                    .filter(embed -> embed.getParticipantUserIds().contains(userId))
                    .toList();
        }

        @Override
        public List<ElectricityStatusEmbed> getByGuildId(final long guildId) {
            return embeds.stream()
                    .filter(embed -> embed.getGuildId() == guildId)
                    .toList();
        }

        @Override
        public boolean updateElectricityStatusEmbed(final ElectricityStatusEmbed statusEmbed) {
            return true;
        }

        @Override
        public void deleteElectricityStatusEmbed(final long messageId) {
        }
    }
}
