package me.eeshe.celeoproc.service.impl;

import java.util.Map;
import java.util.Objects;

import me.eeshe.celeoproc.config.AppMessages;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.service.MessageService;

public final class MessageServiceImpl implements MessageService {
    private final AppMessages appMessages;

    public MessageServiceImpl(final AppMessages appMessages) {
        Objects.requireNonNull(appMessages, "AppMessages must not be null");

        this.appMessages = appMessages;
    }

    @Override
    public String get(final Message message) {
        return appMessages.getMessage(message);
    }

    @Override
    public String get(final Message message, final Map<String, String> placeholders) {
        return appMessages.getMessage(message, placeholders);
    }
}
