package com.soystargaze.vitamin.utils.text;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MessageUtils {

    public static Component getMessage(String key, Object... args) {
        return TranslationHandler.getPlayerComponent(key, args);
    }

    public static void logTranslated(String key, Object... args) {
        Component c = TranslationHandler.getLogComponent(key, args);
        Bukkit.getConsoleSender().sendMessage(c);
    }

    public static void sendMessage(Player player, String key, Object... args) {
        Component c = TranslationHandler.getPlayerComponent(key, args);
        player.sendMessage(c);
    }

    public static void sendAndLog(Player player, String key, Object... args) {
        Component c = TranslationHandler.getLogComponent(key, args);
        Bukkit.getConsoleSender().sendMessage(c);
        player.sendMessage(c);
    }
}