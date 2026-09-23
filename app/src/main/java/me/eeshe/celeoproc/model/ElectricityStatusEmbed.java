package me.eeshe.celeoproc.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Persisted representation of an electricity status embed posted in a guild
 * channel.
 *
 * <p>
 * The embed tracks the users that currently participate in the embed, as
 * well as the moment the info was last refreshed.
 */
public final class ElectricityStatusEmbed {
    private final long messageId;
    private final long guildId;
    private final long channelId;
    private final List<Long> participantUserIds;

    private Instant updatedAt;

    public ElectricityStatusEmbed(
            final long messageId,
            final long guildId,
            final long channelId,
            final List<Long> participantUserIds) {
        this(messageId, guildId, channelId, participantUserIds, Instant.now());
    }

    public ElectricityStatusEmbed(
            final long messageId,
            final long guildId,
            final long channelId,
            final List<Long> participantUserIds,
            final Instant updatedAt) {
        this.messageId = messageId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.participantUserIds = normalizeParticipants(participantUserIds);
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    /**
     * Sets {@link #getUpdatedAt()} to {@link Instant#now()}. Intended to be
     * called by an external scheduler before persisting the embed.
     */
    public void refreshUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public long getMessageId() {
        return messageId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public List<Long> getParticipantUserIds() {
        return participantUserIds;
    }

    public void addParticipant(long participantId) {
        this.participantUserIds.add(participantId);
    }

    public void removeParticipant(long participantId) {
        this.participantUserIds.remove(participantId);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at must not be null");
    }

    private List<Long> normalizeParticipants(final List<Long> participantUserIds) {
        if (participantUserIds == null) {
            return List.of();
        }
        return List.copyOf(participantUserIds);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ElectricityStatusEmbed other)) {
            return false;
        }
        return messageId == other.messageId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(messageId);
    }

    @Override
    public String toString() {
        return "ElectricityStatusEmbed{messageId=%d, guildId=%d, channelId=%d, participantUserIds=%s, updatedAt=%s}"
                .formatted(messageId, guildId, channelId, participantUserIds, updatedAt);
    }
}
