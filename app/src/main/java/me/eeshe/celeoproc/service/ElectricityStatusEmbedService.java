package me.eeshe.celeoproc.service;

import java.util.List;
import java.util.Optional;

import me.eeshe.celeoproc.model.ElectricityStatusEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Sends the electricity status embed to Discord channels.
 */
public interface ElectricityStatusEmbedService {
    String ELECTRICITY_OUT_BUTTON_ID = "electricity_status_embed_button_out";
    String ELECTRICITY_IN_BUTTON_ID = "electricity_status_embed_button_in";
    String ELECTRICITY_OUT_MODAL_ID = "electricity_status_embed_out_modal";
    String ELECTRICITY_IN_ESTIMATE_INPUT_ID = "electricity_status_embed_out_modal_input";

    /**
     * Sends the electricity status embed to the given channel.
     *
     * @param channel channel the embed is posted in
     * @return {@code true} when the embed was sent successfully
     */
    boolean sendElectricityStatusEmbed(TextChannel channel);

    /**
     * @param messageId id of the Discord message the embed belongs to
     * @return jump url pointing at the stored embed's message, or an empty optional
     *         when no stored embed exists for the given message
     */
    Optional<String> getJumpUrl(long messageId);

    /**
     * @param userId id of a participating Discord user
     * @return every stored embed that lists the user as a participant
     */
    List<ElectricityStatusEmbed> getByParticipantId(long userId);

    /**
     * @param guildId id of the Discord guild the embeds belong to
     * @return every stored embed that belongs to the given guild
     */
    List<ElectricityStatusEmbed> getByGuildId(long guildId);

    /**
     * Rebuilds the embed from its registered participants and edits the
     * already-posted Discord message.
     *
     * @param statusEmbed stored embed to refresh
     * @return {@code true} when the edit was dispatched
     */
    boolean updateElectricityStatusEmbed(ElectricityStatusEmbed statusEmbed);

    /**
     * Deletes the stored electricity status embed associated with the given
     * message.
     *
     * @param messageId id of the message the embed belongs to
     */
    void deleteElectricityStatusEmbed(long messageId);
}
