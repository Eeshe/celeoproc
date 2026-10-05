package me.eeshe.celeoproc.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import me.eeshe.celeoproc.model.UserElectricityStatus;

/**
 * Persists {@link UserElectricityStatus} entities.
 */
public interface UserElectricityStatusRepository extends Repository {

    /**
     * Inserts the given status, or updates the existing row when the user id is
     * already present.
     *
     * @param status status to persist
     */
    void save(UserElectricityStatus status);

    /**
     * @param userId Discord user id
     * @return the stored status, or an empty optional when no row exists
     */
    Optional<UserElectricityStatus> get(long userId);

    /**
     * @param userIds Discord user ids to look up
     * @return the statuses that exist for the given ids; an empty list when the
     *         input is empty or {@code null}
     */
    List<UserElectricityStatus> get(Collection<Long> userIds);

    /**
     * @param lastReminderOrBefore instant to compare against each user's last
     *                             reminder
     * @return every user without electricity whose last reminder was sent at or
     *         before the given instant, including users that have never been
     *         reminded
     */
    List<UserElectricityStatus> getPendingReminders(Instant lastReminderOrBefore);
}
