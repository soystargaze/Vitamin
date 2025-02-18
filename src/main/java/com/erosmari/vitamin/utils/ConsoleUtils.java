package com.erosmari.vitamin.utils;

import org.bukkit.plugin.java.JavaPlugin;

import static com.erosmari.vitamin.utils.TranslationHandler.loadedKeys;

@SuppressWarnings("ALL")
public class ConsoleUtils {

    public static void displayAsciiArt(JavaPlugin plugin) {

        final String LOCAL_TEST_MESSAGE_KEY = "plugin.logo";
        TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "\n" +
                "__     ___       _     _       \n" +
                "\\ \\   / (_)_   _(_) __| |  _   \n" +
                " \\ \\ / /| \\ \\ / / |/ _` |_| |_ \n" +
                "  \\ V / | |\\ V /| | (_| |_   _|\n" +
                "   \\_/  |_| \\_/ |_|\\__,_| |_|  " +
                "\n");
        LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY);
    }

    public static void displaySuccessMessage(JavaPlugin plugin) {

        LoggingUtils.logTranslated("plugin.separator");
        LoggingUtils.logTranslated("plugin.name");
        LoggingUtils.logTranslated("plugin.version", plugin.getPluginMeta().getVersion());
        LoggingUtils.logTranslated("plugin.author", plugin.getPluginMeta().getAuthors().getFirst());
        LoggingUtils.logTranslated("plugin.separator");
        LoggingUtils.logTranslated("plugin.enabled");
        LoggingUtils.logTranslated("plugin.language_loaded", TranslationHandler.getActiveLanguage(), loadedKeys);
        LoggingUtils.logTranslated("items.registered");
        LoggingUtils.logTranslated("commands.registered");
        LoggingUtils.logTranslated("events.registered");
        LoggingUtils.logTranslated("plugin.separator");
    }
}