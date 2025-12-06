package com.soystargaze.vitamin.utils.text;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TextHandler {

    private static TextHandler instance;

    private TextHandler(JavaPlugin plugin) {
        Bukkit.getLogger().info("[Vitamin] Using TranslationHandler");
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
        TranslationHandler.loadTranslations(plugin, language);
    }

    public boolean isLanguageAvailable(String language) {
        return TranslationHandler.isLanguageAvailable(language);
    }

    public void setActiveLanguage(String language) {
        TranslationHandler.setActiveLanguage(language);
    }

    public String getActiveLanguage() {
        return TranslationHandler.getActiveLanguage();
    }

    public void registerTemporaryTranslation(String key, String message) {
        TranslationHandler.registerTemporaryTranslation(key, message);
    }

    public int getLoadedTranslationsCount() {
        return TranslationHandler.getLoadedTranslationsCount();
    }
    /* ————————————————————————————————————
       logging
    —————————————————————————————————————*/

    public void sendMessage(Player player, String key, Object... args) {
        MessageUtils.sendMessage(player, key, args);
    }

    public void logTranslated(String key, Object... args) {
        MessageUtils.logTranslated(key, args);
    }

    public void sendAndLog(Player player, String key, Object... args) {
        MessageUtils.sendAndLog(player, key, args);
    }

    public Object getMessage(String key, Object... args) {
        return MessageUtils.getMessage(key, args);
    }
}