package com.soystargaze.vitamin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class LoggingUtils {

    public static void logTranslated(String key, Object... args) {
        Component translatedMessage = TranslationHandler.getLogMessage(key, args);
        String message = LegacyComponentSerializer.legacyAmpersand().serialize(translatedMessage);
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        Bukkit.getConsoleSender().sendMessage(formattedMessage);
    }

    public static void sendAndLog(Player player, String key, Object... args) {
        Component messageComponent = TranslationHandler.getPlayerMessage(key, args);
        String message = LegacyComponentSerializer.legacyAmpersand().serialize(messageComponent);
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        player.sendMessage(formattedMessage);
        logTranslated(key, args);
    }

    public static void sendMessage(Player player, String key, Object... args) {
        Component messageComponent = TranslationHandler.getPlayerMessage(key, args);
        String message = LegacyComponentSerializer.legacyAmpersand().serialize(messageComponent);
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        player.sendMessage(formattedMessage);
    }
}