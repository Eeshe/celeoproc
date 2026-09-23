package me.eeshe.celeoproc.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Persisted electricity status of a Discord user.
 *
 * <p>
 * The Discord user id and the last known nickname are always present. The
 * electricity timestamps and the estimate are only known once they have been
 * reported, so they may be {@code null}.
 */
public final class UserElectricityStatus {
    private final long userId;

    private String nickname;
    private Instant electricityIn;
    private Instant electricityOut;
    private Duration electricityInEstimate;

    public UserElectricityStatus(
            final long userId,
            final String nickname,
            final Instant electricityIn,
            final Instant electricityOut,
            final Duration electricityInEstimate) {
        this.userId = userId;
        this.nickname = Objects.requireNonNull(nickname, "Nickname must not be null");
        this.electricityIn = electricityIn;
        this.electricityOut = electricityOut;
        this.electricityInEstimate = electricityInEstimate;
    }

    public long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(final String nickname) {
        this.nickname = Objects.requireNonNull(nickname, "Nickname must not be null");
    }

    public Instant getElectricityIn() {
        return electricityIn;
    }

    public void setElectricityIn(final Instant electricityIn) {
        this.electricityIn = electricityIn;
    }

    public Instant getElectricityOut() {
        return electricityOut;
    }

    public void setElectricityOut(final Instant electricityOut) {
        this.electricityOut = electricityOut;
    }

    public Duration getElectricityInEstimate() {
        return electricityInEstimate;
    }

    public void setElectricityInEstimate(final Duration electricityInEstimate) {
        this.electricityInEstimate = electricityInEstimate;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UserElectricityStatus other)) {
            return false;
        }
        return userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(userId);
    }

    @Override
    public String toString() {
        return "UserElectricityStatus{userId=%d, nickname='%s', electricityIn=%s, electricityOut=%s, electricityInEstimate=%s}"
                .formatted(userId, nickname, electricityIn, electricityOut, electricityInEstimate);
    }
}
