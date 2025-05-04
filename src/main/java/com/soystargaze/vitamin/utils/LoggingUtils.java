package com.soystargaze.vitamin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public class LoggingUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private static String serialize(Component comp) {
        String withAmps = LEGACY_SERIALIZER.serialize(comp);
        return ChatColor.translateAlternateColorCodes('&', withAmps);
    }

    public static String getMessage(String key, Object... args) {
        Component c = TranslationHandler.getPlayerMessage(key, args);
        return serialize(c);
    }

    public static void logTranslated(String key, Object... args) {
        Component c = TranslationHandler.getLogMessage(key, args);
        Bukkit.getConsoleSender().sendMessage(serialize(c));
    }

    public static void sendMessage(Player player, String key, Object... args) {
        Component c = TranslationHandler.getPlayerMessage(key, args);
        player.sendMessage(serialize(c));
    }

    public static void sendAndLog(Player player, String key, Object... args) {
        Component c = TranslationHandler.getLogMessage(key, args);
        Bukkit.getConsoleSender().sendMessage(serialize(c));
        player.sendMessage(serialize(c));
    }
}