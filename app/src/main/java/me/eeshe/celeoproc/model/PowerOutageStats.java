package me.eeshe.celeoproc.model;

import java.util.List;
import java.util.Objects;

/**
 * Power outage stats collected within a date range.
 *
 * @param server     stats for the guild's electricity status embed participants
 * @param global     stats for every log within the range, regardless of guild
 * @param serverLogs raw logs backing the server stats, used to draw graphs
 */
public record PowerOutageStats(
        OutageStats server,
        OutageStats global,
        List<PowerOutageLog> serverLogs) {

    /**
     * Validates the required values and defensively copies the log list so the
     * record stays immutable.
     */
    public PowerOutageStats {
        Objects.requireNonNull(server, "Server stats must not be null");
        Objects.requireNonNull(global, "Global stats must not be null");
        Objects.requireNonNull(serverLogs, "Server logs must not be null");

        serverLogs = List.copyOf(serverLogs);
    }
}
