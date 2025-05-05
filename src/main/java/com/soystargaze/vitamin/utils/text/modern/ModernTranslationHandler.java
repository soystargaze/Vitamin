package com.soystargaze.vitamin.utils.text.modern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModernTranslationHandler {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Map<String, String> translations = new HashMap<>();
    private static int loadedKeys = 0;
    private static String activeLanguage = "en_us";

    public static void loadTranslations(JavaPlugin plugin, String language) {
        File translationsFolder = new File(plugin.getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create the translations folder.");
            return;
        }

        File langFile = new File(translationsFolder, language + ".yml");
        if (!langFile.exists()) {
            try {
                if (langFile.createNewFile()) {
                    String resourcePath = "Translations/" + language + ".yml";
                    if (plugin.getResource(resourcePath) != null) {
                        plugin.saveResource(resourcePath, false);
                        plugin.getLogger().info("Default translation file '" + language + ".yml' created.");
                    } else {
                        plugin.getLogger().warning("Default resource not found for '" + language + ".yml'.");
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating translation file: " + e.getMessage());
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(langFile);
        translations.clear();
        loadedKeys = 0;
        for (String key : cfg.getKeys(true)) {
            if (cfg.isString(key)) {
                translations.put(key, cfg.getString(key));
                loadedKeys++;
            }
        }
        activeLanguage = language;
    }

    public static void clearTranslations() {
        translations.clear();
        loadedKeys = 0;
    }

    public static int getLoadedTranslationsCount() {
        return loadedKeys;
    }

    public static boolean isLanguageAvailable(String language) {
        File langFile = new File(
                JavaPlugin.getProvidingPlugin(ModernTranslationHandler.class)
                        .getDataFolder(),
                "Translations/" + language + ".yml"
        );
        return langFile.exists();
    }

    public static void setActiveLanguage(String language) {
        if (isLanguageAvailable(language)) {
            activeLanguage = language;
        }
    }

    public static String getActiveLanguage() {
        return activeLanguage;
    }

    public static String get(String key) {
        return translations.getOrDefault(key, "Translation not found: " + key + "!");
    }

    private static String replaceArgs(String template, Object... args) {
        for (int i = 0; i < args.length; i++) {
            String value = String.valueOf(args[i]);
            template = template.replace("{" + i + "}", value);
        }
        return template;
    }

    public static Component getComponent(String key, Object... args) {
        String raw = get(key);
        raw = replaceArgs(raw, args);
        return MINI.deserialize(raw);
    }

    public static Component getPlayerComponent(String key, Object... args) {
        String prefix = translations.getOrDefault(
                "plugin.prefix",
                "<gray>[</gray><gradient:#FFA500:#FFFF00>Vitamin</gradient><color:#FFA500>+</color><gray>]</gray> "
        );
        String dynamicColor = translations.getOrDefault(
                "plugin.dynamic_color",
                "<color:#FFA500>"
        );
        String template = translations.getOrDefault(key, "Translation not found: " + key + "!");

        for (int i = 0; i < args.length; i++) {
            String argStr = String.valueOf(args[i]);
            String coloredArg = dynamicColor + argStr + "</color>";
            template = template.replace("{" + i + "}", coloredArg);
        }

        return MINI.deserialize(prefix + template);
    }

    public static Component getLogComponent(String key, Object... args) {
        String prefix = translations.getOrDefault(
                "plugin.prefix",
                "<gray>[</gray><gradient:#FFA500:#FFFF00>Vitamin</gradient><color:#FFA500>+</color><gray>]</gray> "
        );
        String template = translations.getOrDefault(key, "Translation not found: " + key + "!");
        String dynamicColor = translations.getOrDefault("plugin.dynamic_color", "<color:#FFA500>");

        for (int i = 0; i < args.length; i++) {
            String argStr = String.valueOf(args[i]);
            String coloredArg = dynamicColor + argStr + "</color>";
            template = template.replace("{" + i + "}", coloredArg);
        }

        return MINI.deserialize(prefix + template);
    }

    public static void registerTemporaryTranslation(String key, String message) {
        translations.putIfAbsent(key, message);
    }
}