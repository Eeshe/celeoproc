package me.eeshe.celeoproc.service;

import java.time.Duration;

/**
 * Tracks the electricity status of Discord users.
 */
public interface UserElectricityStatusService {

    /**
     * Stores the moment the given user's electricity came back.
     *
     * @param userId   Discord user id
     * @param nickname nickname to use when no status is stored yet
     */
    void setUserElectricityIn(long userId, String nickname);

    /**
     * Stores the moment the given user's electricity went out.
     *
     * @param userId   Discord user id
     * @param nickname nickname to use when no status is stored yet
     * @param duration unused for now, reserved for the electricity in estimate
     */
    void setUserElectricityOut(long userId, String nickname, Duration duration);
}
