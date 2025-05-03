package com.soystargaze.vitamin.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import static com.soystargaze.vitamin.utils.TranslationHandler.loadedKeys;

@SuppressWarnings({"deprecation", "TextBlockMigration"})
public class ConsoleUtils {

    private static JavaPlugin plugin;

    public static void displayAsciiArt(JavaPlugin plugin) {
        ConsoleUtils.plugin = plugin;

        final String LOCAL_TEST_MESSAGE_KEY = "plugin.logo";
        TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "\n" +
                "__     ___ _                  _             \n" +
                "\\ \\   / (_) |_ __ _ _ __ ___ (_)_ __    _   \n" +
                " \\ \\ / /| | __/ _` | '_ ` _ \\| | '_ \\ _| |_ \n" +
                "  \\ V / | | || (_| | | | | | | | | | |_   _|\n" +
                "   \\_/  |_|\\__\\__,_|_| |_| |_|_|_| |_| |_|  ");
        LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY);
    }

    public static void displaySuccessMessage(JavaPlugin plugin) {

        LoggingUtils.logTranslated("plugin.separator");
        LoggingUtils.logTranslated("plugin.name");
        LoggingUtils.logTranslated("plugin.version", plugin.getDescription().getVersion());
        LoggingUtils.logTranslated("plugin.author", plugin.getDescription().getAuthors());
        LoggingUtils.logTranslated("plugin.separator");
        LoggingUtils.logTranslated("plugin.enabled");
        LoggingUtils.logTranslated("plugin.language_loaded", TranslationHandler.getActiveLanguage(), loadedKeys);
        LoggingUtils.logTranslated("items.registered");
        LoggingUtils.logTranslated("commands.registered");
        LoggingUtils.logTranslated("events.registered");
        String version = Bukkit.getVersion();
        if (version.contains("1.21.3") || version.contains("1.21.4")) {
            LoggingUtils.logTranslated("plugin.version_detected", "1.21.3/1.21.4");
        } else if (version.contains("1.21.1") || version.contains("1.21")) {
            LoggingUtils.logTranslated("plugin.version_detected", "1.21/1.21.1");
        } else {
            LoggingUtils.logTranslated("plugin.version_detected", "defaulting to 1.21/1.21.1");
        }
        LoggingUtils.logTranslated("plugin.separator");
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static void setPlugin(JavaPlugin plugin) {
        ConsoleUtils.plugin = plugin;
    }
}