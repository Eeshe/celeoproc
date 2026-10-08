package me.eeshe.celeoproc.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Aggregated power outage stats for a single user.
 *
 * @param userId       Discord user id the stats belong to
 * @param nickname     resolved nickname of the user
 * @param logAmount    amount of power outages the user registered
 * @param totalTime    sum of all the user's outage durations
 * @param averageTime  average outage duration
 * @param longestTime  longest outage duration
 * @param shortestTime shortest outage duration
 */
public record UserOutageStats(
        long userId,
        String nickname,
        int logAmount,
        Duration totalTime,
        Duration averageTime,
        Duration longestTime,
        Duration shortestTime) {

    public UserOutageStats {
        Objects.requireNonNull(nickname, "Nickname must not be null");
        Objects.requireNonNull(totalTime, "Total time must not be null");
        Objects.requireNonNull(averageTime, "Average time must not be null");
        Objects.requireNonNull(longestTime, "Longest time must not be null");
        Objects.requireNonNull(shortestTime, "Shortest time must not be null");
    }
}
