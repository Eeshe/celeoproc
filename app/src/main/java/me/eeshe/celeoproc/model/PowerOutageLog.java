package me.eeshe.celeoproc.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted record of a power outage.
 *
 * <p>
 * A log is created when the user marks their electricity out, and it is
 * completed with the moment their electricity came back when they mark it in.
 * While the outage is ongoing {@link #electricityIn()} is {@code null}. The id
 * is a randomly generated {@link UUID} that is human readable enough to be
 * shown in the registry messages.
 *
 * @param id             unique, automatically generated id of the outage
 * @param userId         Discord user id the outage belongs to
 * @param electricityOut moment the user's electricity went out
 * @param electricityIn  moment the user's electricity came back, or
 *                       {@code null} while the outage is still ongoing
 */
public record PowerOutageLog(
        UUID id,
        long userId,
        Instant electricityOut,
        Instant electricityIn) {

    public PowerOutageLog {
        Objects.requireNonNull(id, "Id must not be null");
        Objects.requireNonNull(userId, "Id must not be null");
        Objects.requireNonNull(electricityOut, "Electricity out must not be null");
    }

    /**
     * @return {@code true} when the user already marked their electricity back in
     */
    public boolean hasElectricityIn() {
        return electricityIn != null;
    }
}
