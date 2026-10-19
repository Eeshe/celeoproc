package me.eeshe.celeoproc.util;

import java.util.Collection;
import java.util.Set;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Central definition of the permissions a member must hold to trigger the
 * bot's privileged actions, such as slash commands and the interactive
 * components they post.
 */
public final class BotPermissions {
    public static final Permission REQUIRED_PERMISSION = Permission.MANAGE_SERVER;

    private BotPermissions() {
    }

    /**
     * @param member member to check, may be {@code null} for interactions outside
     *               of a guild
     * @return whether the member holds the default required permission
     */
    public static boolean isAllowed(final Member member) {
        return isAllowed(member, Set.of(REQUIRED_PERMISSION));
    }

    /**
     * @param member              member to check, may be {@code null} for
     *                            interactions outside of a guild
     * @param requiredPermissions permissions the member must hold
     * @return whether the member holds every required permission
     */
    public static boolean isAllowed(final Member member, final Collection<Permission> requiredPermissions) {
        return member != null && member.hasPermission(requiredPermissions);
    }

    /**
     * @param channel channel the bot intends to post in
     * @return whether the bot can see the channel and send messages in it
     */
    public static boolean canSendMessages(final TextChannel channel) {
        if (channel == null) {
            return false;
        }
        try {
            return channel.canTalk();
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    /**
     * @param channel channel the bot intends to post an embed in
     * @return whether the bot can send an embed in the channel
     */
    public static boolean canSendEmbeds(final TextChannel channel) {
        if (channel == null) {
            return false;
        }
        try {
            return canSendMessages(channel)
                    && channel.getGuild().getSelfMember()
                            .hasPermission(channel, Permission.MESSAGE_EMBED_LINKS);
        } catch (final RuntimeException exception) {
            return false;
        }
    }

    /**
     * Editing a message shares the channel's send requirements. A read-only
     * channel can still reject the edit even when Discord only requires
     * {@code VIEW_CHANNEL} for editing the bot's own messages.
     *
     * @param channel channel the bot intends to edit a message in
     * @return whether the bot can edit one of its own messages in the channel
     */
    public static boolean canEditMessages(final TextChannel channel) {
        return canSendMessages(channel);
    }

    /**
     * @param channel channel the bot intends to delete one of its own messages in
     * @return whether the bot can see the channel and therefore delete its own
     *         message there
     */
    public static boolean canDeleteMessages(final TextChannel channel) {
        if (channel == null) {
            return false;
        }
        try {
            return channel.getGuild().getSelfMember()
                    .hasPermission(channel, Permission.VIEW_CHANNEL);
        } catch (final RuntimeException exception) {
            return false;
        }
    }
}
