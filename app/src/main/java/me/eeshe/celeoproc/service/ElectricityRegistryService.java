package me.eeshe.celeoproc.service;

import java.time.Instant;

/**
 * Sends electricity status updates to the registry channels.
 */
public interface ElectricityRegistryService {

    /**
     * Announces that the given user no longer has electricity.
     *
     * @param userId              id of the user whose status changed
     * @param nickname            nickname of the user
     * @param previousElectricityIn moment the user's electricity last came back, or
     *                              {@code null} when it was never recorded
     */
    void sendElectricityOut(long userId, String nickname, Instant previousElectricityIn);

    /**
     * Announces that the given user has electricity back.
     *
     * @param userId         id of the user whose status changed
     * @param nickname       nickname of the user
     * @param electricityOut moment the user's electricity went out
     * @param electricityIn  moment the user's electricity came back
     */
    void sendElectricityIn(long userId, String nickname, Instant electricityOut, Instant electricityIn);
}
