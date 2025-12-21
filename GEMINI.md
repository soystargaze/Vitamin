# Vitamin+ Project Context

## Overview
**Vitamin+** is a modular Minecraft plugin designed to enhance vanilla gameplay for Spigot and PaperMC servers (targeting version 1.21+). It introduces various "power-ups" or mechanics—such as armor trims with effects, auto tools, crop protection, and more—while maintaining the vanilla feel.

The project is built using **Java 21** and **Gradle**, employing a module-based architecture where features can be individually enabled or disabled via configuration.

## Architecture & Key Components

### Core Structure
*   **Main Entry Point:** `com.soystargaze.vitamin.Vitamin` - Handles plugin lifecycle (enable/disable), configuration loading, and module initialization.
*   **Modules System:**
    *   Located in `com.soystargaze.vitamin.modules`.
    *   Each feature is a separate module (e.g., `ArmorTrimModule`, `AutoToolModule`) inheriting from a common base (likely `ModuleDef` or similar).
    *   Modules are registered and managed by `ModuleManager`.
*   **Configuration:**
    *   `config.yml`: Main configuration file controlling module states and settings.
    *   `ConfigHandler.java`: Manages reading and writing to the config.
*   **Localization:**
    *   `Translations/`: Supports multiple languages (en_us, es_es, ja_jp, etc.).
    *   `TextHandler.java`: Manages message retrieval and formatting (using Adventure API).

### Key Technologies
*   **Platform:** PaperMC / Spigot (API 1.21+).
*   **Language:** Java 21.
*   **Build System:** Gradle (with ShadowJar for uber-jar creation).
*   **Libraries:**
    *   `Adventure`: For modern text components and minimessage formatting.
    *   `HikariCP`: For database connection pooling (if applicable).
    *   `bStats`: For plugin metrics.
    *   `RTag`: For tag/NBT handling.
*   **Integrations (Soft Dependencies):**
    *   WorldGuard, Lands, GriefPrevention (for claim protection).
    *   Vault, PlaceholderAPI.
    *   Lootin, AdvancedEnchantments.

## Building & Running

### Prerequisites
*   JDK 21 or higher.
*   Gradle (wrapper included).

### Build Commands
*   **Build Plugin:**
    ```bash
    ./gradlew build
    ```
    or specifically for the shadow jar (includes dependencies):
    ```bash
    ./gradlew shadowJar
    ```
*   **Output:** The compiled `.jar` file will be located in `build/libs/`.

## Development Conventions
*   **Modular Design:** New features should be implemented as self-contained modules extending the abstract module class.
*   **Configuration:** Every module should have a toggle in `config.yml` and ideally permissions associated with it.
*   **Style:** Standard Java naming conventions.
*   **Translation:** All player-facing text should be translatable via the `Translations` files.

## Directory Layout
*   `src/main/java/com/soystargaze/vitamin/`: Source code.
    *   `modules/`: Individual feature implementations.
    *   `commands/`: Command handling logic.
    *   `config/`: Configuration management.
    *   `integration/`: Handlers for third-party plugins (WorldGuard, etc.).
*   `src/main/resources/`:
    *   `plugin.yml`: Plugin descriptor.
    *   `config.yml`: Default configuration.
    *   `Translations/`: Locale files.

## Common Tasks
*   **Adding a Module:** Create a new class in `modules/core/` (or `paper/` if Paper-specific), extend the base module class, and register it in `ModuleManager`. Add default config values in `config.yml`.
*   **Updating Dependencies:** Check `build.gradle` for library versions.
