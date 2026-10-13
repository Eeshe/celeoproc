package me.eeshe.celeoproc.listener;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import me.eeshe.celeoproc.service.UserElectricityStatusService;
import me.eeshe.celeoproc.util.BotPermissions;
import me.eeshe.celeoproc.util.DurationParser;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;

public final class ElectricityStatusEmbedListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElectricityStatusEmbedListener.class);

    public static final String REMOVE_PARTICIPANT_MODAL_ID_PREFIX = "electricity_status_embed_remove_participant_modal";
    public static final String REMOVE_PARTICIPANT_USER_SELECT_ID = "electricity_status_embed_remove_participant_user_select";

    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final UserElectricityStatusService userElectricityStatusService;
    private final RegistryChannelService registryChannelService;
    private final MessageService messageService;
    private final AppSettings appSettings;
    private final ExecutorService interactionExecutor;

    public ElectricityStatusEmbedListener(
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final UserElectricityStatusService userElectricityStatusService,
            final RegistryChannelService registryChannelService,
            final MessageService messageService,
            final AppSettings appSettings,
            final ExecutorService interactionExecutor) {
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        this.userElectricityStatusService = Objects.requireNonNull(userElectricityStatusService,
                "UserElectricityStatusService must not be null");
        this.registryChannelService = Objects.requireNonNull(registryChannelService,
                "RegistryChannelService must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
        this.interactionExecutor = Objects.requireNonNull(interactionExecutor,
                "ExecutorService must not be null");
    }

    @Override
    public void onMessageDelete(final MessageDeleteEvent event) {
        final long messageId = event.getMessageIdLong();
        electricityStatusEmbedService.deleteElectricityStatusEmbed(messageId);
        registryChannelService.delete(messageId);
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent event) {
        final String componentId = event.getComponentId();
        final long userId = event.getUser().getIdLong();
        final long messageId = event.getMessageIdLong();
        if (ElectricityStatusEmbedService.OPT_OUT_BUTTON_ID.equals(componentId)) {
            event.deferEdit().queue(hook -> interactionExecutor.execute(
                    () -> handleOptOut(hook, userId, messageId)));
            return;
        }
        if (ElectricityStatusEmbedService.REMOVE_PARTICIPANT_BUTTON_ID.equals(componentId)) {
            handleRemoveParticipantButton(event);
            return;
        }
        if (electricityStatusEmbedService.exceedsParticipantLimit(userId, messageId)) {
            event.reply(messageService.get(Message.ELECTRICITY_STATUS_EMBED_PARTICIPANT_LIMIT_REACHED))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if (ElectricityStatusEmbedService.ELECTRICITY_OUT_BUTTON_ID.equals(componentId)) {
            if (userElectricityStatusService.hasNoElectricity(userId)) {
                event.reply(messageService.get(Message.ELECTRICITY_STATUS_ALREADY_NO_ELECTRICITY))
                        .setEphemeral(true)
                        .queue();
                return;
            }
            event.replyModal(buildElectricityOutModal()).queue();
            return;
        } else if (!ElectricityStatusEmbedService.ELECTRICITY_IN_BUTTON_ID.equals(componentId)) {
            return;
        }
        event.deferEdit().queue(hook -> interactionExecutor.execute(
                () -> handleElectricityIn(hook, userId, messageId)));
    }

    /**
     * Marks the user as having electricity back, reporting failures through the
     * deferred interaction's hook. Runs on the interaction executor so the
     * deferred acknowledgment is sent before the registry messages are awaited.
     *
     * @param hook      hook of the deferred interaction
     * @param userId    id of the user that pressed the button
     * @param messageId id of the embed message the button belongs to
     */
    private void handleElectricityIn(final InteractionHook hook, final long userId, final long messageId) {
        try {
            if (!userElectricityStatusService.setUserElectricityIn(userId, messageId)) {
                replyEphemeral(hook, messageService.get(Message.ELECTRICITY_STATUS_ALREADY_HAS_ELECTRICITY));
            }
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to handle the electricity in interaction of user '{}'", userId, exception);
            replyEphemeral(hook, messageService.get(Message.GENERIC_ERROR));
        }
    }

    /**
     * Removes the user from the embed's participants, reporting failures through
     * the deferred interaction's hook. Runs on the interaction executor so the
     * deferred acknowledgment is sent before the repository call.
     *
     * @param hook      hook of the deferred interaction
     * @param userId    id of the user that pressed the button
     * @param messageId id of the embed message the button belongs to
     */
    private void handleOptOut(final InteractionHook hook, final long userId, final long messageId) {
        try {
            if (!electricityStatusEmbedService.deleteParticipant(messageId, userId)) {
                replyEphemeral(hook, messageService.get(Message.ELECTRICITY_STATUS_EMBED_NOT_PARTICIPANT));
            }
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to handle the opt out interaction of user '{}'", userId, exception);
            replyEphemeral(hook, messageService.get(Message.GENERIC_ERROR));
        }
    }

    /**
     * Opens the remove-participant modal for the embed the button belongs to.
     * Requires the interacting member to hold the manage-server permission.
     *
     * @param event button interaction to respond to
     */
    private void handleRemoveParticipantButton(final ButtonInteractionEvent event) {
        if (!ensurePermission(event.getMember(), event)) {
            return;
        }
        event.replyModal(buildRemoveParticipantModal(event.getMessageIdLong())).queue();
    }

    private Modal buildRemoveParticipantModal(final long messageId) {
        final EntitySelectMenu userSelect = EntitySelectMenu
                .create(REMOVE_PARTICIPANT_USER_SELECT_ID, EntitySelectMenu.SelectTarget.USER)
                .setRequiredRange(1, 1)
                .build();
        return Modal
                .create(REMOVE_PARTICIPANT_MODAL_ID_PREFIX + ":" + messageId,
                        messageService.get(Message.ELECTRICITY_STATUS_EMBED_REMOVE_PARTICIPANT_MODAL_TITLE))
                .addComponents(Label.of(
                        messageService.get(Message.ELECTRICITY_STATUS_EMBED_REMOVE_PARTICIPANT_MODAL_LABEL),
                        userSelect))
                .build();
    }

    @Override
    public void onModalInteraction(final ModalInteractionEvent event) {
        final Long removeMessageId = parseEmbedId(event.getModalId(), REMOVE_PARTICIPANT_MODAL_ID_PREFIX);
        if (removeMessageId != null) {
            handleRemoveParticipantModal(event, removeMessageId);
            return;
        }
        if (!ElectricityStatusEmbedService.ELECTRICITY_OUT_MODAL_ID.equals(event.getModalId())) {
            return;
        }
        final net.dv8tion.jda.api.entities.Message embedMessage = event.getMessage();
        final long userId = event.getUser().getIdLong();
        final long embedMessageId = embedMessage == null ? -1L : embedMessage.getIdLong();
        final ModalMapping mapping = event.getValue(ElectricityStatusEmbedService.ELECTRICITY_IN_ESTIMATE_INPUT_ID);
        final String rawInput = mapping == null ? null : mapping.getAsString();

        event.deferEdit().queue(hook -> interactionExecutor.execute(
                () -> handleElectricityOut(hook, userId, embedMessageId, rawInput)));
    }

    /**
     * Handles the remove-participant modal, removing the selected user from the
     * embed encoded in the modal id. Re-checks the permission before deferring,
     * then offloads the repository work to the interaction executor.
     *
     * @param event           modal interaction to respond to
     * @param embedMessageId  id of the embed message the modal belongs to
     */
    private void handleRemoveParticipantModal(final ModalInteractionEvent event, final long embedMessageId) {
        if (!ensurePermission(event.getMember(), event)) {
            return;
        }
        final ModalMapping mapping = event.getValue(REMOVE_PARTICIPANT_USER_SELECT_ID);
        if (mapping == null || mapping.getAsLongList().isEmpty()) {
            event.reply(messageService.get(Message.GENERIC_ERROR)).setEphemeral(true).queue();
            return;
        }
        final long targetUserId = mapping.getAsLongList().get(0);
        event.deferEdit().queue(hook -> interactionExecutor.execute(
                () -> handleRemoveParticipant(hook, embedMessageId, targetUserId)));
    }

    /**
     * Removes the selected user from the embed, reporting failures through the
     * deferred interaction's hook. Runs on the interaction executor so the
     * deferred acknowledgment is sent before the repository call.
     *
     * @param hook         hook of the deferred interaction
     * @param messageId    id of the embed message the modal belongs to
     * @param targetUserId id of the user to remove from the embed
     */
    private void handleRemoveParticipant(
            final InteractionHook hook,
            final long messageId,
            final long targetUserId) {
        try {
            if (!electricityStatusEmbedService.deleteParticipant(messageId, targetUserId)) {
                replyEphemeral(hook, messageService.get(Message.ELECTRICITY_STATUS_EMBED_USER_NOT_PARTICIPANT,
                        Map.of("user", "<@" + targetUserId + ">")));
            }
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to remove user '{}' from embed '{}'", targetUserId, messageId, exception);
            replyEphemeral(hook, messageService.get(Message.GENERIC_ERROR));
        }
    }

    /**
     * Marks the user as having no electricity, reporting failures through the
     * deferred interaction's hook. Runs on the interaction executor so the
     * deferred acknowledgment is sent before the registry messages are awaited.
     *
     * @param hook           hook of the deferred interaction
     * @param userId         id of the user that submitted the modal
     * @param embedMessageId id of the embed message the modal was opened from, or
     *                       a negative value when the message could not be resolved
     * @param rawInput       raw estimate input of the modal
     */
    private void handleElectricityOut(
            final InteractionHook hook,
            final long userId,
            final long embedMessageId,
            final String rawInput) {
        try {
            if (embedMessageId < 0L) {
                replyEphemeral(hook, messageService.get(Message.GENERIC_ERROR));
                return;
            }
            final Duration estimate = parseEstimate(rawInput);
            if (estimate == null) {
                replyEphemeral(hook, messageService.get(Message.ELECTRICITY_STATUS_INVALID_DURATION,
                        Map.of("input", rawInput.trim())));
                return;
            }
            if (!userElectricityStatusService.setUserElectricityOut(userId, estimate, embedMessageId)) {
                replyEphemeral(hook, messageService.get(Message.ELECTRICITY_STATUS_ALREADY_NO_ELECTRICITY));
            }
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to handle the electricity out modal of user '{}'", userId, exception);
            replyEphemeral(hook, messageService.get(Message.GENERIC_ERROR));
        }
    }

    /**
     * Parses the estimate field of the outage modal, falling back to the
     * configured default when the field was left empty.
     *
     * @param rawInput raw estimate input, possibly {@code null} or blank
     * @return parsed estimate, or {@code null} when the input was invalid
     */
    private Duration parseEstimate(final String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return appSettings.getDefaultElectricityOutEstimate();
        }

        try {
            return DurationParser.parse(rawInput);
        } catch (final DateTimeParseException exception) {
            return null;
        }
    }

    private Modal buildElectricityOutModal() {
        final TextInput input = TextInput
                .create(ElectricityStatusEmbedService.ELECTRICITY_IN_ESTIMATE_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder(messageService.get(Message.ELECTRICITY_STATUS_EMBED_OUT_MODAL_PLACEHOLDER))
                .setRequired(false)
                .build();
        return Modal
                .create(
                        ElectricityStatusEmbedService.ELECTRICITY_OUT_MODAL_ID,
                        messageService.get(Message.ELECTRICITY_STATUS_EMBED_OUT_MODAL_TITLE))
                .addComponents(Label.of(
                        messageService.get(Message.ELECTRICITY_STATUS_EMBED_OUT_MODAL_LABEL),
                        input))
                .build();
    }

    /**
     * Extracts the embed message id encoded in a custom id shaped
     * {@code prefix:messageId}.
     *
     * @param customId custom id to parse
     * @param prefix   expected prefix
     * @return the encoded id, or {@code null} when the custom id does not match
     */
    private Long parseEmbedId(final String customId, final String prefix) {
        final String fullPrefix = prefix + ":";
        if (customId == null || !customId.startsWith(fullPrefix)) {
            return null;
        }
        try {
            return Long.parseLong(customId.substring(fullPrefix.length()));
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Verifies the interacting member holds the permission required to manage the
     * electricity status embeds, replying ephemerally with
     * {@link Message#NO_PERMISSION} when they do not.
     *
     * @param member member that triggered the interaction, may be {@code null}
     * @param event  callback used to answer the interaction
     * @return whether the member is allowed to proceed
     */
    private boolean ensurePermission(final Member member, final IReplyCallback event) {
        if (BotPermissions.isAllowed(member)) {
            return true;
        }
        event.reply(messageService.get(Message.NO_PERMISSION)).setEphemeral(true).queue();
        return false;
    }

    /**
     * Sends an ephemeral follow-up on a deferred interaction, logging failures
     * instead of propagating them on the interaction executor.
     *
     * @param hook    hook of the deferred interaction
     * @param message message to send
     */
    private void replyEphemeral(final InteractionHook hook, final String message) {
        hook.sendMessage(message).setEphemeral(true).queue(
                ignored -> {
                },
                throwable -> LOGGER.error("Failed to send an interaction error follow-up", throwable));
    }
}
