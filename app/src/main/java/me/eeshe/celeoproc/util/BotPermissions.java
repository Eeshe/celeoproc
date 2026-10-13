package me.eeshe.celeoproc.util;

import java.util.Collection;
import java.util.Set;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

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
}
