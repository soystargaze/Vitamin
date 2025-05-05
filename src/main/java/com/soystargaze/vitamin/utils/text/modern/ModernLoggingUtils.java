package com.soystargaze.vitamin.utils.text.modern;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ModernLoggingUtils {

    public static Component getMessage(String key, Object... args) {
        return ModernTranslationHandler.getPlayerComponent(key, args);
    }

    public static void logTranslated(String key, Object... args) {
        Component c = ModernTranslationHandler.getLogComponent(key, args);
        Bukkit.getConsoleSender().sendMessage(c);
    }

    public static void sendMessage(Player player, String key, Object... args) {
        Component c = ModernTranslationHandler.getPlayerComponent(key, args);
        player.sendMessage(c);
    }

    public static void sendAndLog(Player player, String key, Object... args) {
        Component c = ModernTranslationHandler.getLogComponent(key, args);
        Bukkit.getConsoleSender().sendMessage(c);
        player.sendMessage(c);
    }
}