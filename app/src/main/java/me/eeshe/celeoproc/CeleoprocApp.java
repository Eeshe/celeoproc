package me.eeshe.celeoproc;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.AppMessages;
import me.eeshe.celeoproc.config.AppSecrets;
import me.eeshe.celeoproc.config.AppSettings;

public class CeleoprocApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(CeleoprocApp.class);

    public static void main(String[] args) throws InterruptedException, SQLException {
        final AppSecrets secrets = new AppSecrets();
        final AppSettings settings = new AppSettings();
        settings.load();

        final AppMessages messages = new AppMessages();
        messages.load();

        final Bot bot = new Bot(settings, secrets, messages);
        bot.initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdown, "bot-shutdown"));
    }
}
