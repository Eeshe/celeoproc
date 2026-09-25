package me.eeshe.celeoproc.listener;

import java.util.Objects;

import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.UserElectricityStatusService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class ElectricityStatusEmbedListener extends ListenerAdapter {
    private final ElectricityStatusEmbedService electricityStatusEmbedService;
    private final UserElectricityStatusService userElectricityStatusService;

    public ElectricityStatusEmbedListener(
            final ElectricityStatusEmbedService electricityStatusEmbedService,
            final UserElectricityStatusService userElectricityStatusService) {
        this.electricityStatusEmbedService = Objects.requireNonNull(electricityStatusEmbedService,
                "ElectricityStatusEmbedService must not be null");
        this.userElectricityStatusService = Objects.requireNonNull(userElectricityStatusService,
                "UserElectricityStatusService must not be null");
    }

    @Override
    public void onMessageDelete(final MessageDeleteEvent event) {
        electricityStatusEmbedService.deleteElectricityStatusEmbed(event.getMessageIdLong());
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent event) {
        final String componentId = event.getComponentId();
        final long userId = event.getUser().getIdLong();
        final String nickname = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getUser().getName();

        if (ElectricityStatusEmbedService.BUTTON_OUT_ID.equals(componentId)) {
            userElectricityStatusService.setUserElectricityOut(userId, nickname, null);
        } else if (ElectricityStatusEmbedService.BUTTON_IN_ID.equals(componentId)) {
            userElectricityStatusService.setUserElectricityIn(userId, nickname);
        } else {
            return;
        }
        event.deferEdit().queue();
    }
}
