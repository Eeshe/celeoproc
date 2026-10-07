package me.eeshe.celeoproc.model;

import java.util.List;
import java.util.Objects;

/**
 * Power outage logs collected for a guild within a date range.
 *
 * @param guildLogs  logs whose user is a participant of the guild's electricity
 *                   status embeds
 * @param globalLogs every log within the range, regardless of guild
 */
public record PowerOutageStats(
        List<PowerOutageLog> guildLogs,
        List<PowerOutageLog> globalLogs) {

    public PowerOutageStats {
        Objects.requireNonNull(guildLogs, "Guild logs must not be null");
        Objects.requireNonNull(globalLogs, "Global logs must not be null");

        guildLogs = List.copyOf(guildLogs);
        globalLogs = List.copyOf(globalLogs);
    }
}
