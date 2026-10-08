package me.eeshe.celeoproc.service;

import java.util.Collection;
import java.util.Map;

/**
 * Resolves the display nickname of a Discord user.
 */
public interface NicknameResolver {

    /**
     * Resolves the nicknames of several users at once, preferring guild nicknames
     * and falling back to the global usernames.
     *
     * @param guildId guild the users belong to
     * @param userIds Discord user ids to resolve
     * @return a map containing an entry for every requested user id, never
     *         {@code null}
     */
    Map<Long, String> resolveNicknames(long guildId, Collection<Long> userIds);

    /**
     * Resolves the user's nickname, preferring the guild nickname and falling back
     * to the global username.
     *
     * @param guildId guild the user belongs to
     * @param userId  Discord user id
     * @return the resolved nickname, never {@code null}
     */
    String resolveNickname(long guildId, long userId);
}
