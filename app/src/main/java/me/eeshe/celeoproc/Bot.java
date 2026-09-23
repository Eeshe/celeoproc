package me.eeshe.celeoproc;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.command.BotCommand;
import me.eeshe.celeoproc.command.CommandRegistry;
import me.eeshe.celeoproc.command.CommandRegistryImpl;
import me.eeshe.celeoproc.config.AppMessages;
import me.eeshe.celeoproc.config.AppSecrets;
import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.JsonConfigLoader;
import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.database.impl.PostgreSQLDatabase;
import me.eeshe.celeoproc.listener.CommandListener;
import me.eeshe.celeoproc.repository.ElectricityStatusEmbedRepository;
import me.eeshe.celeoproc.repository.Repository;
import me.eeshe.celeoproc.repository.UserElectricityStatusRepository;
import me.eeshe.celeoproc.repository.impl.ElectricityStatusEmbedRepositoryImpl;
import me.eeshe.celeoproc.repository.impl.UserElectricityStatusRepositoryImpl;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.impl.MessageServiceImpl;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public final class Bot {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);

    private final AppSettings appSettings;
    private final AppSecrets appSecrets;
    private final AppMessages appMessages;

    private MessageService messageService;
    private CommandRegistry commandRegistry;

    private final List<JsonConfigLoader> configs;

    private JDA bot;
    private Database database;
    private UserElectricityStatusRepository userElectricityStatusRepository;
    private ElectricityStatusEmbedRepository electricityStatusEmbedRepository;

    public Bot(final AppSettings appSettings, final AppSecrets appSecrets, final AppMessages appMessages) {
        Objects.requireNonNull(appSettings, "AppSettings must not be null");
        Objects.requireNonNull(appSecrets, "AppSecrets must not be null");
        Objects.requireNonNull(appMessages, "AppMessages must not be null");

        this.appSettings = appSettings;
        this.appSecrets = appSecrets;
        this.appMessages = appMessages;
        this.configs = List.of(appSettings, appMessages);
    }

    public void initialize() throws SQLException, InterruptedException {
        initializeDatabase();

        LOGGER.info("Starting Discord bot");
        this.bot = JDABuilder.createDefault(appSecrets.getDiscordBotToken()).build();
        bot.awaitReady();

        initializeServices();
        initializeRegistries();
        registerCommands();
        registerListeners();

        LOGGER.info("Bot is ready");
    }

    private void initializeServices() {
        this.messageService = new MessageServiceImpl(appMessages);
    }

    private void initializeRegistries() {
        this.commandRegistry = new CommandRegistryImpl(messageService, this);
    }

    private void registerListeners() {
        bot.addEventListener(new CommandListener(commandRegistry));
    }

    private void registerCommands() {
        registerGlobalCommands();
        registerGuildCommands();
    }

    private void registerGlobalCommands() {
        bot.updateCommands().addCommands(computeCommandData()).queue();
    }

    private void registerGuildCommands() {
        for (final long guildId : appSettings.getGuildIds()) {
            final Guild guild = bot.getGuildById(guildId);
            if (guild == null) {
                LOGGER.warn("Bot is not in guild '{}' (or it does not exist), skipping command registration",
                        guildId);
                continue;
            }
            guild.updateCommands().addCommands(computeCommandData()).queue();
        }
    }

    private List<CommandData> computeCommandData() {
        return commandRegistry.getCommands()
                .stream().map(BotCommand::createCommandData).toList();
    }

    private void initializeDatabase() throws SQLException {
        this.database = new PostgreSQLDatabase(appSecrets);

        database.connect();
        initializeRepositories();
    }

    private void initializeRepositories() throws SQLException {
        this.userElectricityStatusRepository = new UserElectricityStatusRepositoryImpl(database);
        userElectricityStatusRepository.initialize();

        this.electricityStatusEmbedRepository = new ElectricityStatusEmbedRepositoryImpl(database);
        electricityStatusEmbedRepository.initialize();
    }

    public AppSettings getAppSettings() {
        return appSettings;
    }

    public AppMessages getAppMessages() {
        return appMessages;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public List<JsonConfigLoader> getConfigs() {
        return configs;
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public JDA getBot() {
        return bot;
    }

    public Database getDatabase() {
        return database;
    }

    public UserElectricityStatusRepository getUserElectricityStatusRepository() {
        return userElectricityStatusRepository;
    }

    public ElectricityStatusEmbedRepository getElectricityStatusEmbedRepository() {
        return electricityStatusEmbedRepository;
    }

    public void shutdown() {
        if (bot != null) {
            bot.shutdown();
        }
        shutdownRepositories();
        shutdownDatabase();
    }

    private void shutdownRepositories() {
        shutdownRepository(userElectricityStatusRepository);
        shutdownRepository(electricityStatusEmbedRepository);
    }

    private void shutdownRepository(final Repository repository) {
        if (repository == null) {
            return;
        }
        try {
            repository.shutdown();
        } catch (SQLException exception) {
            LOGGER.error("Failed to shut down repository", exception);
        }
    }

    private void shutdownDatabase() {
        if (database == null) {
            return;
        }
        try {
            database.disconnect();
        } catch (SQLException exception) {
            LOGGER.error("Failed to disconnect from database", exception);
        }
    }
}
