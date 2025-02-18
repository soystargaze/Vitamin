package com.erosmari.vitamin.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LoggingUtils {

    public static void logTranslated(String key, Object... args) {
        Component translatedMessage = TranslationHandler.getLogMessage(key, args);

        Bukkit.getConsoleSender().sendMessage(translatedMessage);
    }

    public static void sendAndLog(Player player, String key, Object... args) {
        Component message = TranslationHandler.getPlayerMessage(key, args);

        player.sendMessage(message);
        logTranslated(key, args);
    }

    public static void sendMessage(Player player, String key, Object... args) {
        Component message = TranslationHandler.getPlayerMessage(key, args);

        player.sendMessage(message);
    }
}