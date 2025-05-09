package com.soystargaze.vitamin.config;

import com.soystargaze.vitamin.Vitamin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigHandler {

    private static FileConfiguration config;
    private static String language;

    public static void setup(JavaPlugin plugin) {
        plugin.saveDefaultConfig();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        InputStream resourceStream = plugin.getResource("config.yml");
        if (resourceStream == null) {
            plugin.getLogger().severe("Default config.yml not found in resources.");
            return;
        }
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resourceStream, StandardCharsets.UTF_8)
        );

        for (String key : defaultConfig.getKeys(true)) {
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
            }
        }

        try {
            userConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving config.yml: " + e.getMessage());
        }

        config = userConfig;

        language = config.getString("language", "en_us");
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static void reload() {
        JavaPlugin plugin = Vitamin.getInstance();
        plugin.reloadConfig();
        config = plugin.getConfig();
        language = config.getString("language", "en_us");
    }

    public static String getLanguage() {
        return language;
    }

    public static int getInt(String path, int def) {
        return config.getInt(path, def);
    }
}