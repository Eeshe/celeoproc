package me.eeshe.celeoproc.repository;

import java.time.Instant;
import java.util.List;

import me.eeshe.celeoproc.model.PowerOutageLog;

/**
 * Persists {@link PowerOutageLog} entities.
 */
public interface PowerOutageLogRepository extends Repository {

    /**
     * Inserts the given power outage log.
     *
     * @param powerOutageLog log to persist
     */
    void save(PowerOutageLog powerOutageLog);

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
