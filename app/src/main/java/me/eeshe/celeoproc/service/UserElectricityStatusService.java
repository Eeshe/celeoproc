package me.eeshe.celeoproc.service;

import java.time.Duration;

/**
 * Tracks the electricity status of Discord users.
 */
public interface UserElectricityStatusService {

    /**
     * Stores the moment the given user's electricity came back.
     *
     * @param userId         Discord user id
     * @param nickname       nickname to use when no status is stored yet
     * @param embedMessageId id of the message whose buttons were used
     * @return {@code true} when the status was updated or the user was newly
     *         registered in the embed, {@code false} when the user already has
     *         electricity and was already registered
     */
    boolean setUserElectricityIn(long userId, String nickname, long embedMessageId);

    /**
     * Stores the moment the given user's electricity went out.
     *
     * @param userId                Discord user id
     * @param nickname              nickname to use when no status is stored yet
     * @param electricityInEstimate expected duration until the electricity comes
     *                              back
     * @param embedMessageId        id of the message whose buttons were used
     * @return {@code true} when the status was updated or the user was newly
     *         registered in the embed, {@code false} when the user already has
     *         no electricity and was already registered
     */
    boolean setUserElectricityOut(long userId, String nickname, Duration electricityInEstimate, long embedMessageId);

    /**
     * @param userId Discord user id
     * @return {@code true} when the user has a stored status without electricity
     */
    boolean hasNoElectricity(long userId);
}
