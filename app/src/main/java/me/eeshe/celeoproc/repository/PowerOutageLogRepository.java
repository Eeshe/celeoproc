package me.eeshe.celeoproc.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import me.eeshe.celeoproc.model.PowerOutageLog;

/**
 * Persists {@link PowerOutageLog} entities.
 */
public interface PowerOutageLogRepository extends Repository {

    /**
     * Inserts the given power outage log. Logs are created when the outage starts,
     * so their electricity in moment may still be {@code null}.
     *
     * @param powerOutageLog log to persist
     */
    void save(PowerOutageLog powerOutageLog);

    /**
     * Overwrites the stored log with the same id, typically to fill in the
     * electricity in moment once the outage ends.
     *
     * @param powerOutageLog log to update
     */
    void update(PowerOutageLog powerOutageLog);

    /**
     * Returns the newest stored log of the given user whose electricity has not
     * come back yet.
     *
     * @param userId Discord user id the outage belongs to
     * @return the incomplete log, or an empty optional when none exists
     */
    Optional<PowerOutageLog> getIncompletePowerOutageLog(long userId);

    /**
     * Returns the newest completed log of the given user that started before the
     * given instant. Used to recompute the duration shown in the electricity out
     * registry message.
     *
     * @param userId        Discord user id the outage belongs to
     * @param beforeInstant exclusive upper bound for the log's electricity out
     * @return the previous completed log, or an empty optional when none exists
     */
    Optional<PowerOutageLog> getPreviousCompletedPowerOutageLog(long userId, Instant beforeInstant);

    /**
     * @param id id of the log to look up
     * @return the stored log with the given id, or an empty optional when none
     *         exists
     */
    Optional<PowerOutageLog> getById(UUID id);

    /**
     * Deletes the stored log with the given id if it exists.
     *
     * @param id id of the log to delete
     */
    void delete(UUID id);

    /**
     * Returns every log whose outage interval overlaps the given range, that is
     * logs whose electricity out is at or before {@code rangeEnd} and whose
     * electricity in is at or after {@code rangeStart}.
     *
     * @param rangeStart inclusive start of the range
     * @param rangeEnd   inclusive end of the range
     * @return matching logs ordered by electricity out
     */
    List<PowerOutageLog> getWithinRange(Instant rangeStart, Instant rangeEnd);
}
