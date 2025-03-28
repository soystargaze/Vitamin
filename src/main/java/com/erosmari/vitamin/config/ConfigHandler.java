package com.erosmari.vitamin.config;

import com.erosmari.vitamin.Vitamin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigHandler {

    private static FileConfiguration config;
    private static String language;

    public static void setup(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        language = config.getString("language", "en_us");
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static void reload() {
        JavaPlugin plugin = Vitamin.getInstance();
        plugin.reloadConfig();
        config = getConfig();
    }

    public static String getLanguage() {
        return language;
    }

    public static int getInt(String path, int def) {
        return config.getInt(path, def);
    }
}