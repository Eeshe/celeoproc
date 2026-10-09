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
     * Inserts the given power outage log, or overwrites the stored log when one
     * already exists with the same id.
     *
     * @param powerOutageLog log to persist
     */
    void save(PowerOutageLog powerOutageLog);

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
