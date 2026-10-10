package me.eeshe.celeoproc.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import me.eeshe.celeoproc.service.NicknameResolver;

/**
 * Test double for {@link NicknameResolver} that returns the names registered
 * through {@link #setName(long, String)} and falls back to the user id for
 * unknown users, mirroring {@code JDANicknameResolver}'s never-null contract.
 */
public final class StubNicknameResolver implements NicknameResolver {

    private final Map<Long, String> names = new HashMap<>();

    /**
     * Registers the nickname the resolver should return for the given user.
     *
     * @param userId   Discord user id
     * @param nickname nickname to return, or {@code null}/blank to reset to the
     *                 user id fallback
     */
    public void setName(final long userId, final String nickname) {
        names.put(userId, nickname);
    }

    @Override
    public String resolveNickname(final long guildId, final long userId) {
        final String nickname = names.get(userId);
        return nickname == null || nickname.isBlank() ? String.valueOf(userId) : nickname;
    }

    @Override
    public Map<Long, String> resolveNicknames(final long guildId, final Collection<Long> userIds) {
        final Map<Long, String> resolved = new HashMap<>();
        for (final Long userId : userIds) {
            resolved.put(userId, resolveNickname(guildId, userId));
        }
        return resolved;
    }
}
