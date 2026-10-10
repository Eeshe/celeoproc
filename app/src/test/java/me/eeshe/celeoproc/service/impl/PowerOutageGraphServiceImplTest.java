package me.eeshe.celeoproc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import me.eeshe.celeoproc.config.AppMessages;
import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.support.StubNicknameResolver;

/**
 * Verifies that the graph service writes a per-user and a combined PNG per
 * month, and that the generated files are cleaned up afterwards.
 */
class PowerOutageGraphServiceImplTest {

    private AppSettings appSettings;
    private StubNicknameResolver nicknameResolver;
    private PowerOutageGraphServiceImpl service;

    @TempDir
    Path baseDir;

    @BeforeEach
    void setUp() {
        appSettings = new AppSettings();
        appSettings.load();

        final AppMessages appMessages = new AppMessages();
        appMessages.load();
        final MessageService messageService = new MessageServiceImpl(appMessages);

        nicknameResolver = new StubNicknameResolver();
        service = new PowerOutageGraphServiceImpl(appSettings, nicknameResolver, messageService, baseDir);
    }

    @Test
    void generateGraphsWritesOnePerUserAndOneCombinedPerMonth() throws IOException {
        nicknameResolver.setName(1L, "Alice");
        nicknameResolver.setName(2L, "Bob");

        final List<PowerOutageLog> logs = List.of(
                log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z"),
                log(1L, "2025-05-07T10:00:00Z", "2025-05-07T10:30:00Z"),
                log(2L, "2025-05-08T10:00:00Z", "2025-05-08T11:00:00Z"));

        final List<Path> graphs = service.generateGraphs(100L, logs);

        assertEquals(Set.of("1_2025-05.png", "2_2025-05.png", "combined_2025-05.png"),
                fileNames(graphs));
        assertAllExistAndNonEmpty(graphs);
    }

    @Test
    void generateGraphsSplitsOutagesCrossingMonthBoundaries() throws IOException {
        final ZoneId timezone = appSettings.getTimezone();
        final Instant electricityOut = LocalDateTime.of(2025, 5, 31, 23, 0).atZone(timezone).toInstant();
        final Instant electricityIn = LocalDateTime.of(2025, 6, 1, 1, 0).atZone(timezone).toInstant();

        final List<Path> graphs = service.generateGraphs(100L,
                List.of(new PowerOutageLog(UUID.randomUUID(), 1L, electricityOut, electricityIn)));

        assertEquals(Set.of("1_2025-05.png", "1_2025-06.png", "combined_2025-05.png", "combined_2025-06.png"),
                fileNames(graphs));
        assertAllExistAndNonEmpty(graphs);
    }

    @Test
    void generateGraphsWithNoLogsReturnsNothingAndCreatesNoDirectory() throws IOException {
        final List<Path> graphs = service.generateGraphs(100L, List.of());

        assertTrue(graphs.isEmpty());
        try (var entries = Files.list(baseDir)) {
            assertEquals(0, entries.count());
        }
    }

    @Test
    void deleteGraphsRemovesFilesAndTheirDirectory() {
        final List<Path> graphs = service.generateGraphs(100L,
                List.of(log(1L, "2025-05-06T10:00:00Z", "2025-05-06T12:00:00Z")));
        assertFalse(graphs.isEmpty());

        service.deleteGraphs(graphs);

        for (final Path graph : graphs) {
            assertFalse(Files.exists(graph));
        }
        assertTrue(graphs.stream().map(Path::getParent).noneMatch(Files::exists));
    }

    @Test
    void deleteGraphsWithEmptyListIsNoOp() {
        service.deleteGraphs(List.of());
    }

    private static void assertAllExistAndNonEmpty(final List<Path> graphs) throws IOException {
        for (final Path graph : graphs) {
            assertTrue(Files.exists(graph), graph + " should exist");
            assertTrue(Files.size(graph) > 0, graph + " should not be empty");
        }
    }

    private static Set<String> fileNames(final List<Path> graphs) {
        return graphs.stream().map(path -> path.getFileName().toString()).collect(Collectors.toSet());
    }

    private static PowerOutageLog log(final long userId, final String electricityOut, final String electricityIn) {
        return new PowerOutageLog(UUID.randomUUID(), userId, Instant.parse(electricityOut),
                Instant.parse(electricityIn));
    }
}
