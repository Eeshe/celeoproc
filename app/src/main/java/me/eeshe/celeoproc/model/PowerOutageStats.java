package me.eeshe.celeoproc.model;

import java.util.Objects;

/**
 * Power outage stats collected within a date range.
 *
 * @param server stats for the guild's electricity status embed participants
 * @param global stats for every log within the range, regardless of guild
 */
public record PowerOutageStats(
        OutageStats server,
        OutageStats global) {

    public PowerOutageStats {
        Objects.requireNonNull(server, "Server stats must not be null");
        Objects.requireNonNull(global, "Global stats must not be null");
    }
}
