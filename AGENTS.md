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
