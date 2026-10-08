package me.eeshe.celeoproc.model;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated power outage stats for a set of logs.
 *
 * <p>
 * The general totals are always present. The extremes, award winner and
 * per-user breakdown are only populated for the server scoped stats; the global
 * stats leave them {@code null} or empty.
 *
 * @param logAmount     total amount of logs in the set
 * @param totalTime     sum of every log's outage duration
 * @param averageTime   average outage duration
 * @param longestOutage longest single outage, or {@code null} when unknown
 * @param shortestOutage shortest single outage, or {@code null} when unknown
 * @param awardWinner   user with the most total outage time, or {@code null}
 * @param userStats     per-user breakdown, empty when not applicable
 */
public record OutageStats(
        int logAmount,
        Duration totalTime,
        Duration averageTime,
        OutageExtreme longestOutage,
        OutageExtreme shortestOutage,
        UserOutageStats awardWinner,
        List<UserOutageStats> userStats) {

    public OutageStats {
        if (logAmount < 0) {
            throw new IllegalArgumentException("Log amount must not be negative");
        }
        Objects.requireNonNull(totalTime, "Total time must not be null");
        Objects.requireNonNull(averageTime, "Average time must not be null");
        Objects.requireNonNull(userStats, "User stats must not be null");

        userStats = List.copyOf(userStats);
    }
}
