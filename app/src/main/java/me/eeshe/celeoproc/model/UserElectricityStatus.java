package me.eeshe.celeoproc.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private Instant electricityInEstimate;

    public UserElectricityStatus(
            final long userId,
            final String nickname,
            final Instant electricityIn,
            final Instant electricityOut,
            final Instant electricityInEstimate) {
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

    public Instant getElectricityInEstimate() {
        return electricityInEstimate;
    }

    public void setElectricityInEstimate(final Instant electricityInEstimate) {
        this.electricityInEstimate = electricityInEstimate;
    }

    /**
     * @return {@code true} when no electricity out has been recorded yet, or
     *         when the latest recorded event was electricity in
     */
    public boolean hasElectricity() {
        if (electricityOut == null) {
            return true;
        }
        if (electricityIn == null) {
            return false;
        }
        return electricityIn.isAfter(electricityOut);
    }

    /**
     * @param zone timezone the current day is resolved in
     * @return {@code true} when the last electricity out happened in the current
     *         day
     */
    public boolean hasElectricityOutToday(final ZoneId zone) {
        if (electricityOut == null) {
            return false;
        }
        return LocalDate.now(zone).equals(electricityOut.atZone(zone).toLocalDate());
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
