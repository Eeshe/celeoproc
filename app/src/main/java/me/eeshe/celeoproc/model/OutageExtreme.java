package me.eeshe.celeoproc.model;

import java.time.Duration;
import java.util.Objects;

/**
 * A single power outage extreme (longest or shortest) paired with the nickname
 * of the user it belongs to.
 *
 * @param nickname nickname of the user that suffered the outage
 * @param duration duration of the outage
 */
public record OutageExtreme(
        String nickname,
        Duration duration) {

    public OutageExtreme {
        Objects.requireNonNull(nickname, "Nickname must not be null");
        Objects.requireNonNull(duration, "Duration must not be null");
    }
}
