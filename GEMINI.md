# Vitamin+ (Vanilla Enhanced)

## Project Overview
**Vitamin+** is a Minecraft plugin designed for **PaperMC** and **Spigot** (1.21+) that enhances vanilla gameplay with 32 modular "power-ups." Its primary goal is to provide new mechanics (e.g., auto-tools, armor trims, carry-on, vein mining) without breaking the core vanilla experience.

### Main Technologies
- **Language:** Java 21 (uses records, switch expressions, etc.)
- **Build Tool:** Gradle (with `com.gradleup.shadow` for fat JAR generation)
- **Frameworks/APIs:**
  - [Paper API](https://papermc.io/) (1.21.10-R0.1-SNAPSHOT)
  - [Adventure (Kyori)](https://docs.advntr.dev/) for rich text components and MiniMessage
  - [HikariCP](https://github.com/brettwooldridge/HikariCP) for database connection pooling (SQLite, MySQL, MariaDB, PostgreSQL)
  - [bStats](https://bstats.org/) for plugin metrics
  - Soft dependencies: WorldGuard, Lands, Lootin, GriefPrevention, Vault, PlaceholderAPI, AdvancedEnchantments

### Architecture
- **Main Class:** `com.soystargaze.vitamin.Vitamin` (manages initialization, config, database, and modules)
- **Module System:** Each feature is a self-contained `Listener` class in `com.soystargaze.vitamin.modules.core`.
- **ModuleManager:** Handles instantiation and registration of modules based on `config.yml`.
- **DatabaseHandler:** Manages persistent storage for player-specific module states, death locations, and more.
- **TextHandler/TranslationHandler:** Manages multi-language support through YAML files in the `Translations/` folder.
- **VersionAdapter:** Provides abstraction for different Minecraft versions (1.21.1, 1.21.4, 1.21.10).

## Building and Running
### Build Commands
- **Build Fat JAR:** `./gradlew shadowJar` (JAR will be in `build/libs/`)
- **Clean Project:** `./gradlew clean`
- **Full Build:** `./gradlew build` (depends on `shadowJar`)

### Installation
1.  Ensure you have **Java 21+** and a **PaperMC/Spigot 1.21+** server.
2.  Drop the `Vitamin-X.Y.Z.jar` into the `plugins/` folder.
3.  Restart the server to generate default configurations.
4.  Configure `config.yml` as needed and use `/vitamin reload` to apply changes.

## Development Conventions
- **Module Design:** New features should be implemented as a new class in `com.soystargaze.vitamin.modules.core` and registered in `ModuleManager.java` using `ModuleDef`.
- **Permissions:** Every module should check for `vitamin.module.<name>` permission.
- **Player-Specific Toggles:** Modules should respect player-specific state stored via `DatabaseHandler.isModuleEnabledForPlayer()`.
- **Text & Colors:** Use `TextHandler.get().logTranslated()` or similar for messages. Prefer Kyori's MiniMessage for rich text formatting.
- **Async Execution:** Use `AsyncExecutor` for non-blocking operations (e.g., database queries).
- **Style:** Adhere to modern Java 21 idioms (e.g., records for data holders, switch expressions for mapping).

## Key Files
- `src/main/resources/config.yml`: Central configuration for all modules and plugin settings.
- `src/main/resources/plugin.yml`: Plugin metadata and command/permission definitions.
- `src/main/java/com/soystargaze/vitamin/Vitamin.java`: Main entry point.
- `src/main/java/com/soystargaze/vitamin/modules/ModuleManager.java`: Module registry.
- `src/main/java/com/soystargaze/vitamin/database/DatabaseHandler.java`: Database logic.
- `src/main/resources/Translations/`: Folder containing localized message files.
