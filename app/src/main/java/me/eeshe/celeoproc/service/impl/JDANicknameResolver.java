package me.eeshe.celeoproc.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.service.NicknameResolver;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

/**
 * Resolves nicknames through JDA, preferring the guild nickname and falling
 * back
 * to the global username.
 */
public final class JDANicknameResolver implements NicknameResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(JDANicknameResolver.class);

    private final JDA bot;

    public JDANicknameResolver(final JDA bot) {
        this.bot = Objects.requireNonNull(bot, "JDA must not be null");
    }

    @Override
    public Map<Long, String> resolveNicknames(final long guildId, final Collection<Long> userIds) {
        final Map<Long, String> nicknames = new HashMap<>();
        for (final long userId : userIds) {
            nicknames.put(userId, resolveNickname(guildId, userId));
        }
        return nicknames;
    }

    @Override
    public String resolveNickname(final long guildId, final long userId) {
        final String guildNickname = resolveGuildNickname(guildId, userId);
        if (guildNickname != null && !guildNickname.isBlank()) {
            return guildNickname;
        }
        final String username = resolveUsername(userId);
        if (username != null && !username.isBlank()) {
            return username;
        }
        return String.valueOf(userId);
    }

    private String resolveGuildNickname(final long guildId, final long userId) {
        final Guild guild = bot.getGuildById(guildId);
        if (guild == null) {
            return null;
        }
        try {
            final Member member = guild.retrieveMemberById(userId).complete();
            return member == null ? null : member.getEffectiveName();
        } catch (final RuntimeException exception) {
            LOGGER.debug("Failed to retrieve member '{}' in guild '{}'", userId, guildId, exception);
            return null;
        }
    }

    private String resolveUsername(final long userId) {
        try {
            final User user = bot.retrieveUserById(userId).complete();
            return user == null ? null : user.getName();
        } catch (final RuntimeException exception) {
            LOGGER.debug("Failed to retrieve user '{}'", userId, exception);
            return null;
        }
    }
}
