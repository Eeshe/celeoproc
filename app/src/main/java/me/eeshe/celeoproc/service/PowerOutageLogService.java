package me.eeshe.celeoproc.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.model.PowerOutageStats;

/**
 * Tracks ended power outages reported by Discord users.
 */
public interface PowerOutageLogService {

    /**
     * Creates a log for a power outage that just started and persists it
     * immediately, leaving the electricity in moment {@code null}. The id is
     * generated automatically.
     *
     * @param userId         Discord user id the outage belongs to
     * @param electricityOut moment the user's electricity went out
     * @return the persisted incomplete log
     */
    PowerOutageLog startPowerOutage(long userId, Instant electricityOut);

    /**
     * @param userId Discord user id the outage belongs to
     * @return the newest stored log of the user whose electricity has not come
     *         back yet, or an empty optional when none exists
     */
    Optional<PowerOutageLog> getIncompletePowerOutageLog(long userId);

    /**
     * @param userId        Discord user id the outage belongs to
     * @param beforeInstant exclusive upper bound for the log's electricity out
     * @return the newest completed log of the user that started before the given
     *         instant, or an empty optional when none exists
     */
    Optional<PowerOutageLog> getPreviousCompletedPowerOutageLog(long userId, Instant beforeInstant);

    /**
     * Overwrites the stored log with the same id, persisting all of its fields.
     *
     * @param log log to update
     * @return the updated log
     */
    PowerOutageLog update(PowerOutageLog log);

    /**
     * @param id id of the log to look up
     * @return the stored log with the given id, or an empty optional when none
     *         exists
     */
    Optional<PowerOutageLog> getById(UUID id);

    /**
     * Deletes the given log.
     *
     * @param log log to delete
     * @return {@code true} when a stored log was deleted
     */
    boolean delete(PowerOutageLog log);

    /**
     * Overwrites the timestamps of an existing log. Both inputs are interpreted in
     * the timezone configured in {@code AppSettings}.
     *
     * @param log            log to update
     * @param electricityOut new moment the user's electricity went out
     * @param electricityIn  new moment the user's electricity came back
     * @return the updated log
     */
    PowerOutageLog updatePowerOutage(PowerOutageLog log, LocalDateTime electricityOut, LocalDateTime electricityIn);

    /**
     * Builds the configurable message placeholders describing the given log, so
     * the command and listener layers do not need to resolve nicknames or format
     * timestamps themselves.
     *
     * @param log     log to describe
     * @param guildId guild the log is being displayed in
     * @return placeholders for {@code nickname}, {@code electricity_out},
     *         {@code electricity_in}, {@code power_outage_time} and {@code log_id}
     */
    Map<String, String> buildLogPlaceholders(PowerOutageLog log, long guildId);

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
