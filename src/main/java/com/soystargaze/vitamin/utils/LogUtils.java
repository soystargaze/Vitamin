package com.soystargaze.vitamin.utils;

import com.soystargaze.vitamin.Vitamin;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtils {

    private static File logFile;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void init(JavaPlugin plugin) {
        File dataFolder = new File(plugin.getDataFolder(), "Data");
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                plugin.getLogger().severe("Could not create plugin data folder: " + dataFolder.getAbsolutePath());
                return;
            }
        }

        logFile = new File(new File(dataFolder, "vitamin-carry-on-registry.log").getAbsolutePath());
        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    plugin.getLogger().severe("Could not create log file: " + logFile.getAbsolutePath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create log file: " + e.getMessage());
            }
        }
    }

    public static void logContainerPickup(String playerName, String containerType, String containerId, Location location) {
        String locationStr = formatLocation(location);
        log("CONTAINER_PICKUP", playerName, containerType, containerId, locationStr);
    }

    private static String formatLocation(Location location) {
        return location.getWorld().getName() + ":" +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    public static void logEntityPickup(String playerName, String entityType, Location location) {
        String locationStr = formatLocation(location);
        log("ENTITY_PICKUP", playerName, entityType, locationStr);
    }

    public static void logEntityDrop(String playerName, String entityType, Location location) {
        String locationStr = formatLocation(location);
        log("ENTITY_DROP", playerName, entityType, locationStr);
    }

    public static void logRestoration(String adminName, String containerType, String chestId) {
        log("RESTORATION", adminName, containerType, chestId);
    }

    private static void log(String action, String... details) {
        if (logFile == null) {
            Vitamin.getInstance().getLogger().severe("Log file is not initialized.");
            return;
        }
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] %s: %s", timestamp, action, String.join(" | ", details));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            Vitamin.getInstance().getLogger().severe("Could not write to log file: " + e.getMessage());
        }
    }
}