package me.eeshe.celeoproc.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted record of a power outage that has already ended.
 *
 * <p>
 * A log is only created when a user marks their electricity back in, using the
 * moment the electricity went out and the moment it came back. The id is a
 * randomly generated {@link UUID} that is human readable enough to be shown in
 * the registry messages.
 *
 * @param id             unique, automatically generated id of the outage
 * @param userId         Discord user id the outage belongs to
 * @param electricityOut moment the user's electricity went out
 * @param electricityIn  moment the user's electricity came back
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
        Objects.requireNonNull(electricityIn, "Electricity in must not be null");
    }
}
