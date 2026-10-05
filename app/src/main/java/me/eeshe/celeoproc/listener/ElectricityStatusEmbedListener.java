package me.eeshe.celeoproc.listener;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import me.eeshe.celeoproc.service.UserElectricityStatusService;
import me.eeshe.celeoproc.util.DurationParser;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;

public final class ElectricityStatusEmbedListener extends ListenerAdapter {
    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final UserElectricityStatusService userElectricityStatusService;
    private final RegistryChannelService registryChannelService;
    private final MessageService messageService;
    private final AppSettings appSettings;

    public ElectricityStatusEmbedListener(
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final UserElectricityStatusService userElectricityStatusService,
            final RegistryChannelService registryChannelService,
            final MessageService messageService,
            final AppSettings appSettings) {
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        this.userElectricityStatusService = Objects.requireNonNull(userElectricityStatusService,
                "UserElectricityStatusService must not be null");
        this.registryChannelService = Objects.requireNonNull(registryChannelService,
                "RegistryChannelService must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
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
        final String nickname = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getUser().getName();

        if (ElectricityStatusEmbedService.ELECTRICITY_OUT_BUTTON_ID.equals(componentId)) {
            if (userElectricityStatusService.hasNoElectricity(userId)) {
                event.reply(messageService.get(Message.ELECTRICITY_STATUS_ALREADY_NO_ELECTRICITY))
                        .setEphemeral(true)
                        .queue();
                return;
            }
            event.replyModal(buildElectricityOutModal()).queue();
            return;
        } else if (ElectricityStatusEmbedService.ELECTRICITY_IN_BUTTON_ID.equals(componentId)) {
            if (!userElectricityStatusService.setUserElectricityIn(userId, nickname, event.getMessageIdLong())) {
                event.reply(messageService.get(Message.ELECTRICITY_STATUS_ALREADY_HAS_ELECTRICITY))
                        .setEphemeral(true)
                        .queue();
                return;
            }
        } else {
            return;
        }
        event.deferEdit().queue();
    }

    @Override
    public void onModalInteraction(final ModalInteractionEvent event) {
        if (!ElectricityStatusEmbedService.ELECTRICITY_OUT_MODAL_ID.equals(event.getModalId())) {
            return;
        }
        final long userId = event.getUser().getIdLong();
        final String nickname = event.getMember().getEffectiveName();

        final Duration estimate = parseEstimate(event);
        if (estimate == null) {
            return;
        }
        final net.dv8tion.jda.api.entities.Message embedMessage = event.getMessage();
        if (embedMessage == null) {
            return;
        }
        if (!userElectricityStatusService.setUserElectricityOut(userId, nickname, estimate, embedMessage.getIdLong())) {
            event.reply(messageService.get(Message.ELECTRICITY_STATUS_ALREADY_NO_ELECTRICITY))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        event.deferEdit().queue();
    }

    /**
     * Reads and parses the estimate field of the outage modal, falling back to the
     * configured default when the field was left empty.
     *
     * @param event modal interaction to read
     * @return parsed estimate, or {@code null} when the input was invalid and an
     *         ephemeral error was already sent
     */
    private Duration parseEstimate(final ModalInteractionEvent event) {
        final ModalMapping mapping = event.getValue(ElectricityStatusEmbedService.ELECTRICITY_IN_ESTIMATE_INPUT_ID);
        final String input = mapping == null ? null : mapping.getAsString();
        if (input == null || input.isBlank()) {
            return appSettings.getDefaultElectricityOutEstimate();
        }

        try {
            return DurationParser.parse(input);
        } catch (final DateTimeParseException exception) {
            event.reply(messageService.get(Message.ELECTRICITY_STATUS_INVALID_DURATION,
                    Map.of("input", input.trim())))
                    .setEphemeral(true)
                    .queue();
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
}
