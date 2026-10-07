package me.eeshe.celeoproc.service;

import java.time.Instant;
import java.time.LocalDate;

import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.PowerOutageStats;

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
     * @param guildId    id of the Discord guild the stats belong to. Logs are
     *                   matched against the participants of the guild's
     *                   electricity status embeds
     * @param rangeStart inclusive start calendar day
     * @param rangeEnd   inclusive end calendar day
     * @return the guild's logs and every log within the range. Both days are
     *         interpreted in the timezone configured in {@code AppSettings},
     *         spanning from the start of {@code rangeStart} to the end of
     *         {@code rangeEnd}
     */
    PowerOutageStats getWithinRange(long guildId, LocalDate rangeStart, LocalDate rangeEnd);
}
