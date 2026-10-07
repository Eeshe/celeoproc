package me.eeshe.celeoproc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.command.BotCommand;
import me.eeshe.celeoproc.config.AppMessages;
import me.eeshe.celeoproc.config.AppSecrets;
import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.JsonConfigLoader;
import me.eeshe.celeoproc.database.Database;
import me.eeshe.celeoproc.database.impl.PostgreSQLDatabase;
import me.eeshe.celeoproc.listener.CommandListener;
import me.eeshe.celeoproc.listener.ElectricityStatusEmbedListener;
import me.eeshe.celeoproc.registry.CommandRegistry;
import me.eeshe.celeoproc.registry.impl.CommandRegistryImpl;
import me.eeshe.celeoproc.repository.ElectricityStatusEmbedRepository;
import me.eeshe.celeoproc.repository.PowerOutageLogRepository;
import me.eeshe.celeoproc.repository.RegistryChannelRepository;
import me.eeshe.celeoproc.repository.Repository;
import me.eeshe.celeoproc.repository.UserElectricityStatusRepository;
import me.eeshe.celeoproc.repository.impl.ElectricityStatusEmbedRepositoryImpl;
import me.eeshe.celeoproc.repository.impl.PowerOutageLogRepositoryImpl;
import me.eeshe.celeoproc.repository.impl.RegistryChannelRepositoryImpl;
import me.eeshe.celeoproc.repository.impl.UserElectricityStatusRepositoryImpl;
import me.eeshe.celeoproc.scheduler.BotScheduler;
import me.eeshe.celeoproc.scheduler.impl.ElectricityStatusEmbedScheduler;
import me.eeshe.celeoproc.scheduler.impl.ReminderScheduler;
import me.eeshe.celeoproc.service.ElectricityRegistryService;
import me.eeshe.celeoproc.service.ElectricityStatusEmbedService;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.PowerOutageLogService;
import me.eeshe.celeoproc.service.RegistryChannelService;
import me.eeshe.celeoproc.service.UserElectricityStatusService;
import me.eeshe.celeoproc.service.impl.ElectricityRegistryServiceImpl;
import me.eeshe.celeoproc.service.impl.ElectricityStatusEmbedServiceImpl;
import me.eeshe.celeoproc.service.impl.MessageServiceImpl;
import me.eeshe.celeoproc.service.impl.PowerOutageLogServiceImpl;
import me.eeshe.celeoproc.service.impl.RegistryChannelServiceImpl;
import me.eeshe.celeoproc.service.impl.UserElectricityStatusServiceImpl;
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
    private ElectricityStatusEmbedService electricityStatusEmbedService;
    private ElectricityRegistryService electricityRegistryService;
    private PowerOutageLogService powerOutageLogService;
    private UserElectricityStatusService userElectricityStatusService;
    private RegistryChannelService registryChannelService;
    private CommandRegistry commandRegistry;

    private final List<JsonConfigLoader> configs;
    private final List<BotScheduler> botSchedulers = new ArrayList<>();

    private JDA bot;
    private Database database;
    private UserElectricityStatusRepository userElectricityStatusRepository;
    private ElectricityStatusEmbedRepository electricityStatusEmbedRepository;
    private RegistryChannelRepository registryChannelRepository;
    private PowerOutageLogRepository powerOutageLogRepository;

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
        initializeSchedulers();
        initializeRegistries();
        registerCommands();
        registerListeners();

        LOGGER.info("Bot is ready");
    }

    private void initializeServices() {
        this.messageService = new MessageServiceImpl(appMessages);
        this.electricityStatusEmbedService = new ElectricityStatusEmbedServiceImpl(electricityStatusEmbedRepository,
                userElectricityStatusRepository, messageService, appSettings, bot);
        this.registryChannelService = new RegistryChannelServiceImpl(registryChannelRepository,
                electricityStatusEmbedService);
        this.electricityRegistryService = new ElectricityRegistryServiceImpl(registryChannelService, messageService,
                bot);
        this.powerOutageLogService = new PowerOutageLogServiceImpl(powerOutageLogRepository,
                electricityStatusEmbedService, appSettings);
        this.userElectricityStatusService = new UserElectricityStatusServiceImpl(userElectricityStatusRepository,
                electricityStatusEmbedRepository, electricityStatusEmbedService, electricityRegistryService,
                powerOutageLogService, appSettings);
    }

    private void initializeSchedulers() {
        botSchedulers.add(new ElectricityStatusEmbedScheduler(
                appSettings, electricityStatusEmbedRepository, electricityStatusEmbedService));
        botSchedulers.add(new ReminderScheduler(
                userElectricityStatusService, electricityStatusEmbedService, messageService, bot));

        for (final BotScheduler scheduler : botSchedulers) {
            scheduler.start();
        }
    }

    private void initializeRegistries() {
        this.commandRegistry = new CommandRegistryImpl(messageService, electricityStatusEmbedService,
                registryChannelService, powerOutageLogService, this);
    }

    private void registerListeners() {
        bot.addEventListener(new CommandListener(commandRegistry));
        bot.addEventListener(new ElectricityStatusEmbedListener(electricityStatusEmbedService,
                userElectricityStatusService, registryChannelService, messageService, appSettings));
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

        this.registryChannelRepository = new RegistryChannelRepositoryImpl(database);
        registryChannelRepository.initialize();

        this.powerOutageLogRepository = new PowerOutageLogRepositoryImpl(database);
        powerOutageLogRepository.initialize();
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

    public ElectricityStatusEmbedService getElectricityStatusEmbedService() {
        return electricityStatusEmbedService;
    }

    public RegistryChannelService getRegistryChannelService() {
        return registryChannelService;
    }

    public ElectricityRegistryService getElectricityRegistryService() {
        return electricityRegistryService;
    }

    public UserElectricityStatusService getUserElectricityStatusService() {
        return userElectricityStatusService;
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

    public RegistryChannelRepository getRegistryChannelRepository() {
        return registryChannelRepository;
    }

    public PowerOutageLogService getPowerOutageLogService() {
        return powerOutageLogService;
    }

    public PowerOutageLogRepository getPowerOutageLogRepository() {
        return powerOutageLogRepository;
    }

    public void shutdown() {
        for (final BotScheduler scheduler : botSchedulers) {
            scheduler.shutdown();
        }
        if (bot != null) {
            bot.shutdown();
        }
        shutdownRepositories();
        shutdownDatabase();
    }

    private void shutdownRepositories() {
        shutdownRepository(userElectricityStatusRepository);
        shutdownRepository(electricityStatusEmbedRepository);
        shutdownRepository(registryChannelRepository);
        shutdownRepository(powerOutageLogRepository);
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
