package com.soystargaze.vitamin.utils;

import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings({"deprecation", "TextBlockMigration"})
public class ConsoleUtils {

    private static JavaPlugin plugin;

    public static void displayAsciiArt(JavaPlugin plugin) {
        ConsoleUtils.plugin = plugin;

        final String LOCAL_TEST_MESSAGE_KEY = "plugin.logo";
        TextHandler.get().registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "\n" +
                "__     ___ _                  _             \n" +
                "\\ \\   / (_) |_ __ _ _ __ ___ (_)_ __    _   \n" +
                " \\ \\ / /| | __/ _` | '_ ` _ \\| | '_ \\ _| |_ \n" +
                "  \\ V / | | || (_| | | | | | | | | | |_   _|\n" +
                "   \\_/  |_|\\__\\__,_|_| |_| |_|_|_| |_| |_|  ");
        TextHandler.get().logTranslated(LOCAL_TEST_MESSAGE_KEY);
    }

    public static void displaySuccessMessage(JavaPlugin plugin) {

        TextHandler.get().logTranslated("plugin.separator");
        TextHandler.get().logTranslated("plugin.name");
        TextHandler.get().logTranslated("plugin.version", plugin.getDescription().getVersion());
        TextHandler.get().logTranslated("plugin.author", plugin.getDescription().getAuthors());
        TextHandler.get().logTranslated("plugin.website", plugin.getDescription().getWebsite());
        TextHandler.get().logTranslated("plugin.separator");
        TextHandler.get().logTranslated("plugin.enabled");
        TextHandler.get().logTranslated("plugin.language_loaded", TextHandler.get().getActiveLanguage(), TextHandler.get().getLoadedTranslationsCount());
        TextHandler.get().logTranslated("items.registered");
        TextHandler.get().logTranslated("commands.registered");
        TextHandler.get().logTranslated("events.registered");
        String raw = Bukkit.getBukkitVersion();
        String version = raw.replaceFirst(".*?(\\d+\\.\\d+\\.\\d+).*", "$1");
        TextHandler.get().logTranslated("plugin.version_detected", version);
        TextHandler.get().logTranslated("plugin.separator");
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static void setPlugin(JavaPlugin plugin) {
        ConsoleUtils.plugin = plugin;
    }
}