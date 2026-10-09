package me.eeshe.celeoproc.listener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.service.ElectricityRegistryService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.PowerOutageLogService;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;

/**
 * Handles the interactive components used to manage already stored power outage
 * logs: the edit and delete buttons attached to the registry messages, the edit
 * modal and the delete confirmation button.
 */
public final class PowerOutageManagementListener extends ListenerAdapter {
    public static final String EDIT_BUTTON_ID_PREFIX = "power_outage_edit";
    public static final String DELETE_BUTTON_ID_PREFIX = "power_outage_delete";
    public static final String DELETE_CONFIRM_BUTTON_ID_PREFIX = "power_outage_delete_confirm";

    public static final String EDIT_MODAL_ID_PREFIX = "power_outage_edit_modal";

    public static final String ELECTRICITY_OUT_INPUT_ID = "power_outage_electricity_out_input";
    public static final String ELECTRICITY_IN_INPUT_ID = "power_outage_electricity_in_input";

    public static final DateTimeFormatter MODAL_DATE_TIME_DISPLAY_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd hh:mm a")
            .toFormatter();
    public static final DateTimeFormatter MODAL_INPUT_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-M-d h:mm")
            .optionalStart()
            .appendLiteral(' ')
            .optionalEnd()
            .appendPattern("a")
            .toFormatter();
    public static final DateTimeFormatter MODAL_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("h:mm")
            .optionalStart()
            .appendLiteral(' ')
            .optionalEnd()
            .appendPattern("a")
            .toFormatter();

    private final PowerOutageLogService powerOutageLogService;
    private final MessageService messageService;
    private final AppSettings appSettings;
    private final ElectricityRegistryService electricityRegistryService;

    public PowerOutageManagementListener(
            final PowerOutageLogService powerOutageLogService,
            final MessageService messageService,
            final AppSettings appSettings,
            final ElectricityRegistryService electricityRegistryService) {
        this.powerOutageLogService = Objects.requireNonNull(powerOutageLogService,
                "PowerOutageLogService must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
        this.electricityRegistryService = Objects.requireNonNull(electricityRegistryService,
                "ElectricityRegistryService must not be null");
    }

    /**
     * @param id        id of the log the button belongs to
     * @param messageId id of the registry message the confirmation acts on
     * @return custom id encoding the log and message ids for the delete
     *         confirmation button
     */
    public static String computeDeleteConfirmationButtonId(final UUID id, final long messageId) {
        Objects.requireNonNull(id, "Id must not be null");

        return DELETE_CONFIRM_BUTTON_ID_PREFIX + ":" + id + ":" + messageId;
    }

    /**
     * @param id id of the log the button belongs to
     * @return custom id encoding the log id for the edit button
     */
    public static String computeEditButtonId(final UUID id) {
        Objects.requireNonNull(id, "Id must not be null");

        return EDIT_BUTTON_ID_PREFIX + ":" + id;
    }

    /**
     * @param id id of the log the button belongs to
     * @return custom id encoding the log id for the delete button
     */
    public static String computeDeleteButtonId(final UUID id) {
        Objects.requireNonNull(id, "Id must not be null");

        return DELETE_BUTTON_ID_PREFIX + ":" + id;
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent event) {
        final String componentId = event.getComponentId();
        if (componentId.startsWith(EDIT_BUTTON_ID_PREFIX + ":")) {
            handleEditButton(event);
            return;
        }
        if (componentId.startsWith(DELETE_BUTTON_ID_PREFIX + ":")) {
            handleDeleteButton(event);
            return;
        }
        if (componentId.startsWith(DELETE_CONFIRM_BUTTON_ID_PREFIX)) {
            handleDeleteConfirmationButton(event);
            return;
        }
    }

    /**
     * Opens the edit modal for the log encoded in the clicked edit button.
     *
     * @param event button interaction to respond to
     */
    private void handleEditButton(final ButtonInteractionEvent event) {
        final UUID id = parseLogId(event.getComponentId(), EDIT_BUTTON_ID_PREFIX);
        if (id == null) {
            return;
        }
        if (event.getGuild() == null) {
            event.reply(messageService.get(Message.GUILD_ONLY)).setEphemeral(true).queue();
            return;
        }
        final Optional<PowerOutageLog> logOptional = powerOutageLogService.getById(id);
        if (logOptional.isEmpty()) {
            replyLogNotFound(event, id);
            return;
        }
        event.replyModal(buildEditModal(logOptional.get())).queue();
    }

    /**
     * Replies ephemerally with the delete confirmation for the log encoded in the
     * clicked delete button.
     *
     * @param event button interaction to respond to
     */
    private void handleDeleteButton(final ButtonInteractionEvent event) {
        final UUID id = parseLogId(event.getComponentId(), DELETE_BUTTON_ID_PREFIX);
        if (id == null) {
            return;
        }
        final Guild guild = event.getGuild();
        if (guild == null) {
            event.reply(messageService.get(Message.GUILD_ONLY)).setEphemeral(true).queue();
            return;
        }
        final Optional<PowerOutageLog> logOptional = powerOutageLogService.getById(id);
        if (logOptional.isEmpty()) {
            replyLogNotFound(event, id);
            return;
        }
        final PowerOutageLog log = logOptional.get();
        final Button confirmButton = Button.danger(
                computeDeleteConfirmationButtonId(log.id(), event.getMessageIdLong()),
                messageService.get(Message.DELETE_POWER_OUTAGE_CONFIRM_BUTTON));
        event.reply(messageService.get(Message.DELETE_POWER_OUTAGE_CONFIRM,
                powerOutageLogService.buildLogPlaceholders(log, guild.getIdLong())))
                .setEphemeral(true)
                .addComponents(ActionRow.of(confirmButton))
                .queue();
    }

    private void handleDeleteConfirmationButton(final ButtonInteractionEvent event) {
        final DeleteConfirmation confirmation = parseDeleteConfirmationId(event.getComponentId());
        if (confirmation == null) {
            return;
        }
        final Optional<PowerOutageLog> powerOutageLog = powerOutageLogService.getById(confirmation.logId());
        if (powerOutageLog.isEmpty() || !powerOutageLogService.delete(powerOutageLog.get())) {
            deleteEphemeralAndReply(event, messageService.get(Message.POWER_OUTAGE_LOG_NOT_FOUND,
                    Map.of("log_id", confirmation.logId().toString())));
            return;
        }
        electricityRegistryService.deleteElectricityIn(event.getChannel(), confirmation.messageId());
        deleteEphemeralAndReply(event, messageService.get(Message.DELETE_POWER_OUTAGE_SUCCESS,
                Map.of("log_id", confirmation.logId().toString())));
    }

    /**
     * Deletes the ephemeral confirmation panel this component interaction belongs
     * to
     * and answers it with an ephemeral follow-up message.
     *
     * <p>
     * Uses
     * {@link net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback#deferEdit()}
     * so the interaction's original response resolves to the confirmation message,
     * which is then removed through
     * {@link net.dv8tion.jda.api.interactions.InteractionHook#deleteOriginal()}.
     *
     * @param event   button interaction to acknowledge
     * @param message message to send as the ephemeral follow-up
     */
    private void deleteEphemeralAndReply(final ButtonInteractionEvent event, final String message) {
        event.deferEdit().queue(hook -> {
            hook.deleteOriginal().queue();
            hook.sendMessage(message).setEphemeral(true).queue();
        });
    }

    private void replyLogNotFound(final ButtonInteractionEvent event, final UUID id) {
        event.reply(messageService.get(Message.POWER_OUTAGE_LOG_NOT_FOUND, Map.of("log_id", id.toString())))
                .setEphemeral(true)
                .queue();
    }

    private Modal buildEditModal(final PowerOutageLog log) {
        final ZoneId zone = appSettings.getTimezone();
        final TextInput electricityOutInput = TextInput
                .create(ELECTRICITY_OUT_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder(log.electricityOut().atZone(zone).format(MODAL_DATE_TIME_DISPLAY_FORMATTER))
                .setRequired(false)
                .build();
        final TextInput electricityInInput = TextInput
                .create(ELECTRICITY_IN_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder(log.electricityIn().atZone(zone).format(MODAL_DATE_TIME_DISPLAY_FORMATTER))
                .setRequired(false)
                .build();

        final TextDisplay description = TextDisplay
                .of(messageService.get(Message.EDIT_POWER_OUTAGE_MODAL_DESCRIPTION));

        return Modal
                .create(computeEditModalId(log.id()),
                        messageService.get(Message.EDIT_POWER_OUTAGE_MODAL_TITLE))
                .addComponents(
                        description,
                        Label.of(messageService.get(Message.EDIT_POWER_OUTAGE_OUT_LABEL), electricityOutInput),
                        Label.of(messageService.get(Message.EDIT_POWER_OUTAGE_IN_LABEL), electricityInInput))
                .build();
    }

    /**
     * @param id id of the log the modal belongs to
     * @return custom id encoding the log id for the edit modal
     */
    private String computeEditModalId(final UUID id) {
        Objects.requireNonNull(id, "Id must not be null");

        return EDIT_MODAL_ID_PREFIX + ":" + id;
    }

    @Override
    public void onModalInteraction(final ModalInteractionEvent event) {
        final UUID logId = parseLogId(event.getModalId(), EDIT_MODAL_ID_PREFIX);
        if (logId == null) {
            return;
        }
        final Optional<PowerOutageLog> logOptional = powerOutageLogService.getById(logId);
        if (logOptional.isEmpty()) {
            event.reply(messageService.get(Message.POWER_OUTAGE_LOG_NOT_FOUND, Map.of("log_id", logId.toString())))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        final PowerOutageLog powerOutageLog = logOptional.get();

        final String rawElectricityOut = readValue(event, ELECTRICITY_OUT_INPUT_ID);
        final String rawElectricityIn = readValue(event, ELECTRICITY_IN_INPUT_ID);
        if (rawElectricityOut.isBlank() && rawElectricityIn.isBlank()) {
            event.reply(messageService.get(Message.EDIT_POWER_OUTAGE_MISSING_FIELDS))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final ZoneId timezone = appSettings.getTimezone();
        final LocalDateTime electricityOut = resolveDate(
                event,
                rawElectricityOut,
                timezone,
                powerOutageLog.electricityOut());
        if (electricityOut == null) {
            return;
        }
        final LocalDateTime electricityIn = resolveDate(
                event,
                rawElectricityIn,
                timezone,
                powerOutageLog.electricityIn());
        if (electricityIn == null) {
            return;
        }
        if (!electricityIn.isAfter(electricityOut)) {
            event.reply(messageService.get(Message.EDIT_POWER_OUTAGE_INVALID_RANGE))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        final PowerOutageLog updatedLog = powerOutageLogService.updatePowerOutage(
                powerOutageLog,
                electricityOut,
                electricityIn);
        event.reply(messageService.get(Message.EDIT_POWER_OUTAGE_SUCCESS,
                Map.of("log_id", updatedLog.id().toString())))
                .setEphemeral(true)
                .queue();
        final net.dv8tion.jda.api.entities.Message originalMessage = event.getMessage();
        if (originalMessage != null) {
            electricityRegistryService.editElectricityIn(originalMessage, updatedLog);
        }
    }

    private String readValue(final ModalInteractionEvent event, final String inputId) {
        final ModalMapping mapping = event.getValue(inputId);

        return mapping == null ? "" : mapping.getAsString().trim();
    }

    /**
     * Resolves a modal date input. A blank input reuses the original instant in the
     * configured timezone, while an unparseable one sends an ephemeral error and
     * returns {@code null}.
     *
     * @param event    modal interaction to reply to on invalid input
     * @param raw      raw input value
     * @param timezone configured timezone
     * @param original original instant to reuse when the input is blank
     * @return the resolved date, or {@code null} when the input was invalid
     */
    private LocalDateTime resolveDate(
            final ModalInteractionEvent event,
            final String raw,
            final ZoneId timezone,
            final Instant original) {
        if (raw.isBlank()) {
            return original.atZone(timezone).toLocalDateTime();
        }
        final LocalDateTime parsedDateTime = parseDateTime(raw);
        if (parsedDateTime != null) {
            return parsedDateTime;
        }
        final LocalTime parsedTime = parseTime(raw);
        if (parsedTime != null) {
            return original.atZone(timezone).toLocalDate().atTime(parsedTime);
        }
        replyInvalidDate(event, raw);
        return null;
    }

    private LocalDateTime parseDateTime(final String input) {
        try {
            return LocalDateTime.parse(input, MODAL_INPUT_DATE_FORMATTER);
        } catch (final DateTimeParseException exception) {
            return null;
        }
    }

    private LocalTime parseTime(final String input) {
        try {
            return LocalTime.parse(input, MODAL_TIME_FORMATTER);
        } catch (final DateTimeParseException exception) {
            return null;
        }
    }

    private void replyInvalidDate(final ModalInteractionEvent event, final String input) {
        event.reply(messageService.get(Message.EDIT_POWER_OUTAGE_INVALID_DATE, Map.of("input", input)))
                .setEphemeral(true)
                .queue();
    }

    /**
     * Extracts the log id encoded in a custom id with the shape
     * {@code prefix:uuid}.
     *
     * @param customId custom id to parse
     * @param prefix   expected prefix
     * @return the encoded id, or {@code null} when the custom id does not match
     */
    private UUID parseLogId(final String customId, final String prefix) {
        final String fullPrefix = prefix + ":";
        if (customId == null || !customId.startsWith(fullPrefix)) {
            return null;
        }
        try {
            return UUID.fromString(customId.replace(fullPrefix, ""));
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * Parses a delete confirmation custom id with the shape
     * {@code prefix:logId:messageId}.
     *
     * @param customId custom id to parse
     * @return the encoded ids, or {@code null} when the custom id does not match
     */
    private DeleteConfirmation parseDeleteConfirmationId(final String customId) {
        final String fullPrefix = DELETE_CONFIRM_BUTTON_ID_PREFIX + ":";
        if (customId == null || !customId.startsWith(fullPrefix)) {
            return null;
        }
        final String[] parts = customId.substring(fullPrefix.length()).split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new DeleteConfirmation(UUID.fromString(parts[0]), Long.parseLong(parts[1]));
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private record DeleteConfirmation(UUID logId, long messageId) {
    }
}
