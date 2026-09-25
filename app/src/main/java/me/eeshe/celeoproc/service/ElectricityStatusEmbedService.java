package me.eeshe.celeoproc.service;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Sends the electricity status embed to Discord channels.
 */
public interface ElectricityStatusEmbedService {
    String BUTTON_OUT_ID = "electricity_status_embed_button_out";
    String BUTTON_IN_ID = "electricity_status_embed_button_in";

    /**
     * Sends the electricity status embed to the given channel.
     *
     * @param channel channel the embed is posted in
     * @return {@code true} when the embed was sent successfully
     */
    boolean sendElectricityStatusEmbed(TextChannel channel);

    /**
     * Deletes the stored electricity status embed associated with the given
     * message.
     *
     * @param messageId id of the message the embed belongs to
     */
    void deleteElectricityStatusEmbed(long messageId);
}
