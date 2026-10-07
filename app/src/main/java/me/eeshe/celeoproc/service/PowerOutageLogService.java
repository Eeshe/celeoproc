package me.eeshe.celeoproc.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import me.eeshe.celeoproc.model.PowerOutageLog;

/**
 * Tracks ended power outages reported by Discord users.
 */
public interface PowerOutageLogService {

    /**
     * Creates a log for an already ended power outage and persists it
     * immediately. The id is generated automatically.
     *
     * @param userId         Discord user id the outage belongs to
     * @param electricityOut moment the user's electricity went out
     * @param electricityIn  moment the user's electricity came back
     * @return the persisted log
     */
    PowerOutageLog logPowerOutage(long userId, Instant electricityOut, Instant electricityIn);

    /**
     * @param rangeStart inclusive start calendar day
     * @param rangeEnd   inclusive end calendar day
     * @return every log whose outage interval overlaps the given range. Both days
     *         are interpreted in the timezone configured in {@code AppSettings},
     *         spanning from the start of {@code rangeStart} to the end of
     *         {@code rangeEnd}
     */
    List<PowerOutageLog> getWithinRange(LocalDate rangeStart, LocalDate rangeEnd);
}
