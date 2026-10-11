# Tech Stack:
- Java (programming language)
- PostgreSQL (database, initialized through docker compose)

# General Structure
- A main bot class that contains all other instances such as configs, services, repositories, etc.
- AppSecrets, which holds the sensible configurations fetched from environment variables
- AppSettings, which holds configurations fetched from `settings.json` file
- AppMessages, which holds the configurable messages fetched from `messages.json` file

# Conventions
- Always use PreparedStatement for JDBC operations

# Useful documentation:

JDA (Discord Bot API): https://docs.jda.wiki/index.html

# Common Agent Roadblocks

- **Why does chart rendering crash with a `HeadlessException`?** JFreeChart
  renders through AWT, which needs `java.awt.headless=true` set *before* any
  AWT class loads. `CeleoprocApp.main` sets it first, and the Gradle `test`
  task sets it as a system property; any new entry point must do the same.
- **Where are the raw intervals behind the outage stats?** `PowerOutageStats`
  carries `serverLogs` (the server-scoped `PowerOutageLog`s), not just the
  aggregated `OutageStats`. `PowerOutageLogServiceImpl.getWithinRange` already
  filters them; stats alone hold no `Instant`/interval data, so graph or any
  time-based feature must use `serverLogs()`.
- **JFreeChart legend NPEs on a `null` series key.** `XYPlot.getLegendItems`
  calls `getSeriesKey(series).toString()`, so any dataset series key (used as
  the combined chart's user label) must be non-null. `NicknameResolver`
  implementations — including the shared test double — must therefore never
  return `null`; `JDANicknameResolver` falls back to the user id.
- **Why do interactions fail with `10062 Unknown interaction`?** JDA dispatches
  listeners on the gateway event thread, and Discord requires an
  acknowledgment within 3 seconds. Any handler that runs JDBC, a nickname
  lookup, or a `RestAction.submit()`/`join()` (e.g.
  `UserElectricityStatusServiceImpl` waiting on the electricity registry
  messages) must acknowledge first — `event.deferEdit()` / `event.replyModal()`
  — and offload the slow work to the executor `Bot` creates
  (`interactionExecutor`, virtual threads, shut down first in `Bot.shutdown()`).
  After deferring, validation errors are sent as ephemeral follow-ups through
  `event.getHook()` / the queued hook, not `event.reply(...)`.
- **Why must `ElectricityRegistryServiceImpl` snapshot registry rows before
  going async?** `editRegistryMessages`/`deleteRegistryMessages` read
  `powerOutageRegistryMessageService.getByPowerOutageLogId(...)` synchronously
  and only then submit the channel/nickname lookups and REST calls to
  `Bot.interactionExecutor`. The snapshot is mandatory because
  `power_outage_registry_message.power_outage_log_id` is
  `REFERENCES power_outage_log(id) ON DELETE CASCADE`:
  `PowerOutageManagementListener` deletes the log right after calling
  `deleteRegistryMessages`, so a fully asynchronous row read would race the
  cascade and silently skip the message deletions.
