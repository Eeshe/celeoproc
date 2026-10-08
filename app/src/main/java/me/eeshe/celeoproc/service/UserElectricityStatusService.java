package me.eeshe.celeoproc.service;

import java.time.Duration;
import java.util.List;

import me.eeshe.celeoproc.model.UserElectricityStatus;

/**
 * Tracks the electricity status of Discord users.
 */
public interface UserElectricityStatusService {

    /**
     * Stores the moment the given user's electricity came back.
     *
     * @param userId         Discord user id
     * @param embedMessageId id of the message whose buttons were used
     * @return {@code true} when the status was updated or the user was newly
     *         registered in the embed, {@code false} when the user already has
     *         electricity and was already registered
     */
    boolean setUserElectricityIn(long userId, long embedMessageId);

    /**
     * Stores the moment the given user's electricity went out.
     *
     * @param userId                Discord user id
     * @param electricityInEstimate expected duration until the electricity comes
     *                              back
     * @param embedMessageId        id of the message whose buttons were used
     * @return {@code true} when the status was updated or the user was newly
     *         registered in the embed, {@code false} when the user already has
     *         no electricity and was already registered
     */
    boolean setUserElectricityOut(long userId, Duration electricityInEstimate, long embedMessageId);

    /**
     * @param userId Discord user id
     * @return {@code true} when the user has a stored status without electricity
     */
    boolean hasNoElectricity(long userId);

    /**
     * @return every user without electricity whose last reminder is due
     */
    List<UserElectricityStatus> getPendingReminders();

    /**
     * Marks the given user's reminder as sent and persists the change.
     *
     * @param status status whose reminder was sent
     */
    void markReminderSent(UserElectricityStatus status);
}
