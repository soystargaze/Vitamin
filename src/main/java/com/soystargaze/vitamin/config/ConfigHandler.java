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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigHandler {

    private static FileConfiguration config;
    private static String language;

    public static void setup(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                plugin.getLogger().severe("Could not create plugin data folder at " +
                        dataFolder.getAbsolutePath());
                return;
            }
        }

        File configFile = new File(dataFolder, "config.yml");

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
        Map<String, Object> userValues = new HashMap<>();

        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null) {
            plugin.getLogger().severe("Default config.yml not found in plugin JAR!");
            return;
        }
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
        );

        for (String key : defaultConfig.getKeys(true)) {
            if (oldConfig.contains(key)) {
                userValues.put(key, oldConfig.get(key));
            }
        }

        plugin.saveResource("config.yml", true);

        FileConfiguration mergedConfig = YamlConfiguration.loadConfiguration(configFile);
        for (Map.Entry<String, Object> entry : userValues.entrySet()) {
            mergedConfig.set(entry.getKey(), entry.getValue());
        }

        try {
            mergedConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving config.yml: " + e.getMessage());
        }

        config = mergedConfig;
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

    public static String getString(String path, String def) {
        return config.getString(path, def);
    }

    public static List<String> getStringList(String path) {
        return config.getStringList(path);
    }
}