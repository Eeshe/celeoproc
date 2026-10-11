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
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;

/**
 * Handles the interactive components used to manage already stored power outage
 * logs: the edit and delete buttons attached to the registry messages, the edit
 * modal and the delete confirmation button.
 */
public final class PowerOutageManagementListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowerOutageManagementListener.class);

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
    private final ExecutorService interactionExecutor;

    public PowerOutageManagementListener(
            final PowerOutageLogService powerOutageLogService,
            final MessageService messageService,
            final AppSettings appSettings,
            final ElectricityRegistryService electricityRegistryService,
            final ExecutorService interactionExecutor) {
        this.powerOutageLogService = Objects.requireNonNull(powerOutageLogService,
                "PowerOutageLogService must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
        this.electricityRegistryService = Objects.requireNonNull(electricityRegistryService,
                "ElectricityRegistryService must not be null");
        this.interactionExecutor = Objects.requireNonNull(interactionExecutor,
                "ExecutorService must not be null");
    }

    /**
     * @param id id of the log the button belongs to
     * @return custom id encoding the log id for the delete
     *         confirmation button
     */
    public static String computeDeleteConfirmationButtonId(final UUID id) {
        Objects.requireNonNull(id, "Id must not be null");

        return DELETE_CONFIRM_BUTTON_ID_PREFIX + ":" + id;
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
                computeDeleteConfirmationButtonId(log.id()),
                messageService.get(Message.DELETE_POWER_OUTAGE_CONFIRM_BUTTON));
        event.reply(messageService.get(Message.DELETE_POWER_OUTAGE_CONFIRM,
                powerOutageLogService.buildLogPlaceholders(log, guild.getIdLong())))
                .setEphemeral(true)
                .addComponents(ActionRow.of(confirmButton))
                .queue();
    }

    private void handleDeleteConfirmationButton(final ButtonInteractionEvent event) {
        final UUID logId = parseLogId(event.getComponentId(), DELETE_CONFIRM_BUTTON_ID_PREFIX);
        if (logId == null) {
            return;
        }
        event.deferEdit().queue(hook -> interactionExecutor.execute(() -> deleteLog(hook, logId)));
    }

    /**
     * Deletes the log and its registry messages, reporting the outcome through the
     * deferred interaction's hook. Runs on the interaction executor so the
     * deferred acknowledgment is sent before the database work.
     *
     * @param hook  hook of the deferred interaction
     * @param logId id of the log to delete
     */
    private void deleteLog(final InteractionHook hook, final UUID logId) {
        try {
            final Optional<PowerOutageLog> powerOutageLog = powerOutageLogService.getById(logId);
            if (powerOutageLog.isEmpty()) {
                replyDeletionResult(hook, messageService.get(Message.POWER_OUTAGE_LOG_NOT_FOUND,
                        Map.of("log_id", logId.toString())));
                return;
            }
            final PowerOutageLog log = powerOutageLog.get();
            // Delete the Discord messages before the log, since deleting the log cascades
            // away the registry message rows the messages are resolved from.
            electricityRegistryService.deleteRegistryMessages(log);
            if (!powerOutageLogService.delete(log)) {
                replyDeletionResult(hook, messageService.get(Message.POWER_OUTAGE_LOG_NOT_FOUND,
                        Map.of("log_id", logId.toString())));
                return;
            }
            replyDeletionResult(hook, messageService.get(Message.DELETE_POWER_OUTAGE_SUCCESS,
                    Map.of("log_id", logId.toString())));
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to delete the power outage log '{}'", logId, exception);
            replyDeletionResult(hook, messageService.get(Message.GENERIC_ERROR));
        }
    }

    /**
     * Deletes the ephemeral confirmation panel this component interaction belongs
     * to and answers it with an ephemeral follow-up message.
     *
     * @param hook    hook of the deferred interaction
     * @param message message to send as the ephemeral follow-up
     */
    private void replyDeletionResult(final InteractionHook hook, final String message) {
        hook.deleteOriginal().queue(
                ignored -> {
                },
                throwable -> LOGGER.error("Failed to delete the power outage confirmation prompt", throwable));
        hook.sendMessage(message).setEphemeral(true).queue(
                ignored -> {
                },
                throwable -> LOGGER.error("Failed to send the power outage deletion result", throwable));
    }

    private void replyLogNotFound(final ButtonInteractionEvent event, final UUID id) {
        event.reply(messageService.get(Message.POWER_OUTAGE_LOG_NOT_FOUND, Map.of("log_id", id.toString())))
                .setEphemeral(true)
                .queue();
    }

    private Modal buildEditModal(final PowerOutageLog log) {
        final ZoneId timezone = appSettings.getTimezone();
        final TextInput electricityOutInput = TextInput
                .create(ELECTRICITY_OUT_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder(log.electricityOut().atZone(timezone).format(MODAL_DATE_TIME_DISPLAY_FORMATTER))
                .setRequired(false)
                .build();
        final TextInput electricityInInput = TextInput
                .create(ELECTRICITY_IN_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder(log.electricityIn().atZone(timezone).format(MODAL_DATE_TIME_DISPLAY_FORMATTER))
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
        final String rawElectricityOut = readValue(event, ELECTRICITY_OUT_INPUT_ID);
        final String rawElectricityIn = readValue(event, ELECTRICITY_IN_INPUT_ID);

        event.deferReply(true).queue(hook -> interactionExecutor.execute(
                () -> editLog(hook, logId, rawElectricityOut, rawElectricityIn)));
    }

    /**
     * Applies the modal's date inputs to the log and re-renders its registry
     * messages, reporting the outcome through the deferred interaction's hook.
     * Runs on the interaction executor so the deferred acknowledgment is sent
     * before the database work.
     *
     * @param hook               hook of the deferred interaction
     * @param logId              id of the log to edit
     * @param rawElectricityOut  trimmed raw electricity out input
     * @param rawElectricityIn   trimmed raw electricity in input
     */
    private void editLog(
            final InteractionHook hook,
            final UUID logId,
            final String rawElectricityOut,
            final String rawElectricityIn) {
        try {
            final Optional<PowerOutageLog> logOptional = powerOutageLogService.getById(logId);
            if (logOptional.isEmpty()) {
                replyEphemeral(hook, messageService.get(Message.POWER_OUTAGE_LOG_NOT_FOUND,
                        Map.of("log_id", logId.toString())));
                return;
            }
            final PowerOutageLog powerOutageLog = logOptional.get();
            if (rawElectricityOut.isBlank() && rawElectricityIn.isBlank()) {
                replyEphemeral(hook, messageService.get(Message.EDIT_POWER_OUTAGE_MISSING_FIELDS));
                return;
            }

            final ZoneId timezone = appSettings.getTimezone();
            final LocalDateTime electricityOut = resolveDate(rawElectricityOut, timezone,
                    powerOutageLog.electricityOut());
            if (electricityOut == null) {
                replyEphemeral(hook, messageService.get(Message.EDIT_POWER_OUTAGE_INVALID_DATE,
                        Map.of("input", rawElectricityOut)));
                return;
            }
            final LocalDateTime electricityIn = resolveDate(rawElectricityIn, timezone,
                    powerOutageLog.electricityIn());
            if (electricityIn == null) {
                replyEphemeral(hook, messageService.get(Message.EDIT_POWER_OUTAGE_INVALID_DATE,
                        Map.of("input", rawElectricityIn)));
                return;
            }
            if (!electricityIn.isAfter(electricityOut)) {
                replyEphemeral(hook, messageService.get(Message.EDIT_POWER_OUTAGE_INVALID_RANGE));
                return;
            }
            final PowerOutageLog updatedLog = powerOutageLogService.updatePowerOutage(
                    powerOutageLog,
                    electricityOut,
                    electricityIn);
            electricityRegistryService.editRegistryMessages(updatedLog);
            replyEphemeral(hook, messageService.get(Message.EDIT_POWER_OUTAGE_SUCCESS,
                    Map.of("log_id", updatedLog.id().toString())));
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to edit the power outage log '{}'", logId, exception);
            replyEphemeral(hook, messageService.get(Message.GENERIC_ERROR));
        }
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
                throwable -> LOGGER.error("Failed to send a power outage management reply", throwable));
    }

    private String readValue(final ModalInteractionEvent event, final String inputId) {
        final ModalMapping mapping = event.getValue(inputId);

        return mapping == null ? "" : mapping.getAsString().trim();
    }

    /**
     * Resolves a modal date input. A blank input reuses the original instant in the
     * configured timezone, while an unparseable one returns {@code null} so the
     * caller can report the error.
     *
     * @param raw      raw input value
     * @param timezone configured timezone
     * @param original original instant to reuse when the input is blank
     * @return the resolved date, or {@code null} when the input was invalid
     */
    private LocalDateTime resolveDate(
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
}
