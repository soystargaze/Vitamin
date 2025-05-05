package com.soystargaze.vitamin.utils.text;

import com.soystargaze.vitamin.utils.text.legacy.LegacyLoggingUtils;
import com.soystargaze.vitamin.utils.text.legacy.LegacyTranslationHandler;
import com.soystargaze.vitamin.utils.text.modern.ModernLoggingUtils;
import com.soystargaze.vitamin.utils.text.modern.ModernTranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TextHandler {

    private static TextHandler instance;
    private final boolean modern;

    private TextHandler(JavaPlugin plugin) {
        boolean modernDetected;
        try {
            Class.forName("com.destroystokyo.paper.event.player.PlayerJumpEvent");
            modernDetected = true;
        } catch (ClassNotFoundException e) {
            modernDetected = false;
        }
        this.modern = modernDetected;

        if (modern) {
            Bukkit.getLogger().info("[Vitamin] Using ModernTranslationHandler (Paper/fork detected)");
        } else {
            Bukkit.getLogger().info("[Vitamin] Using LegacyTranslationHandler");
        }
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new TextHandler(plugin);
        }
    }

    public static TextHandler get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "TextHandler has not been initialized."
            );
        }
        return instance;
    }

    /* ————————————————————————————————————
       registry and traduction
    —————————————————————————————————————*/

    public void loadTranslations(JavaPlugin plugin, String language) {
        if (modern) {
            ModernTranslationHandler.loadTranslations(plugin, language);
        } else {
            LegacyTranslationHandler.loadTranslations(plugin, language);
        }
    }

    public boolean isLanguageAvailable(String language) {
        if (modern) {
            return ModernTranslationHandler.isLanguageAvailable(language);
        } else {
            return LegacyTranslationHandler.isLanguageAvailable(language);
        }
    }

    public void setActiveLanguage(String language) {
        if (modern) {
            ModernTranslationHandler.setActiveLanguage(language);
        } else {
            LegacyTranslationHandler.setActiveLanguage(language);
        }
    }

    public String getActiveLanguage() {
        if (modern) {
            return ModernTranslationHandler.getActiveLanguage();
        } else {
            return LegacyTranslationHandler.getActiveLanguage();
        }
    }

    public void registerTemporaryTranslation(String key, String message) {
        if (modern) {
            ModernTranslationHandler.registerTemporaryTranslation(key, message);
        } else {
            LegacyTranslationHandler.registerTemporaryTranslation(key, message);
        }
    }

    public int getLoadedTranslationsCount() {
        if (modern) {
            return ModernTranslationHandler.getLoadedTranslationsCount();
        } else {
            return LegacyTranslationHandler.getLoadedTranslationsCount();
        }
    }
    /* ————————————————————————————————————
       logging
    —————————————————————————————————————*/

    public void sendMessage(Player player, String key, Object... args) {
        if (modern) {
            ModernLoggingUtils.sendMessage(player, key, args);
        } else {
            LegacyLoggingUtils.sendMessage(player, key, args);
        }
    }

    public void logTranslated(String key, Object... args) {
        if (modern) {
            ModernLoggingUtils.logTranslated(key, args);
        } else {
            LegacyLoggingUtils.logTranslated(key, args);
        }
    }

    public void sendAndLog(Player player, String key, Object... args) {
        if (modern) {
            ModernLoggingUtils.sendAndLog(player, key, args);
        } else {
            LegacyLoggingUtils.sendAndLog(player, key, args);
        }
    }

    public Object getMessage(String key, Object... args) {
        if (modern) {
            return ModernLoggingUtils.getMessage(key, args);
        } else {
            return LegacyLoggingUtils.getMessage(key, args);
        }
    }
}