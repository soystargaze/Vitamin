package com.erosmari.vitamin.database;

import com.erosmari.vitamin.config.ConfigHandler;
import com.erosmari.vitamin.utils.LoggingUtils;
import com.erosmari.vitamin.utils.TranslationHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseHandler {

    private static HikariDataSource dataSource;

    public static void initialize(JavaPlugin plugin) {
        if (dataSource != null) {
            LoggingUtils.logTranslated("database.already_initialized");
            return;
        }

        FileConfiguration config = ConfigHandler.getConfig();
        String type = config.getString("database.type", "sqlite").toLowerCase();

        try {
            switch (type) {
                case "mysql":
                    initializeMySQL(config);
                    break;
                case "mariadb":
                    initializeMariaDB(config);
                    break;
                case "postgresql":
                    initializePostgreSQL(config);
                    break;
                case "sqlite":
                    initializeSQLite(plugin);
                    break;
                default:
                    throw new IllegalArgumentException("Database type not supported: " + type);
            }
            createTables();
        } catch (Exception e) {
            LoggingUtils.logTranslated("database.init_error", e);
            throw new IllegalStateException("Database initialization failed.", e);
        }
    }

    private static void initializeSQLite(JavaPlugin plugin) throws SQLException {
        File dbFolder = new File(plugin.getDataFolder(), "Data");
        if (!dbFolder.exists() && !dbFolder.mkdirs()) {
            throw new SQLException(TranslationHandler.get("database.sqlite.error_directory") + dbFolder.getAbsolutePath());
        }

        String dbFilePath = new File(dbFolder, "vitamin.db").getAbsolutePath();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFilePath);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("Vitamin-SQLite");

        dataSource = new HikariDataSource(hikariConfig);
        LoggingUtils.logTranslated("database.sqlite.success");
    }

    private static void initializeMySQL(FileConfiguration config) {
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "vitamin");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("Vitamin-MySQL");

        dataSource = new HikariDataSource(hikariConfig);
        LoggingUtils.logTranslated("database.mysql.success");
    }

    private static void initializeMariaDB(FileConfiguration config) {
        String host = config.getString("database.mariadb.host", "localhost");
        int port = config.getInt("database.mariadb.port", 3306);
        String database = config.getString("database.mariadb.database", "vitamin");
        String username = config.getString("database.mariadb.username", "root");
        String password = config.getString("database.mariadb.password", "");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSSL=false");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("Vitamin-MariaDB");

        dataSource = new HikariDataSource(hikariConfig);
        LoggingUtils.logTranslated("database.mariadb.success");
    }

    private static void initializePostgreSQL(FileConfiguration config) {
        String host = config.getString("database.postgresql.host", "localhost");
        int port = config.getInt("database.postgresql.port", 5432);
        String database = config.getString("database.postgresql.database", "vitamin");
        String username = config.getString("database.postgresql.username", "postgres");
        String password = config.getString("database.postgresql.password", "");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("Vitamin-PostgreSQL");

        dataSource = new HikariDataSource(hikariConfig);
        LoggingUtils.logTranslated("database.postgresql.success");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("The connection pool has not been initialized.");
        }
        return dataSource.getConnection();
    }

    private static void createTables() {
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            String createTable = "CREATE TABLE IF NOT EXISTS player_modules (" +
                    "player_id VARCHAR(36) NOT NULL," +
                    "module_key VARCHAR(100) NOT NULL," +
                    "enabled BOOLEAN NOT NULL," +
                    "PRIMARY KEY (player_id, module_key)" +
                    ");";
            stmt.executeUpdate(createTable);
            LoggingUtils.logTranslated("database.tables.success");
        } catch (SQLException e) {
            LoggingUtils.logTranslated("database.tables.error", e);
        }
    }

    public static boolean isModuleEnabledForPlayer(UUID playerId, String moduleKey) {
        String sql = "SELECT enabled FROM player_modules WHERE player_id = ? AND module_key = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, moduleKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("enabled");
                }
            }
        } catch (SQLException e) {
            LoggingUtils.logTranslated("database.query_error", e);
        }
        return true;
    }

    public static void setModuleEnabledForPlayer(UUID playerId, String moduleKey, boolean enabled) {
        String sql = "REPLACE INTO player_modules (player_id, module_key, enabled) VALUES (?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, moduleKey);
            ps.setBoolean(3, enabled);
            ps.executeUpdate();
        } catch (SQLException e) {
            LoggingUtils.logTranslated("database.update_error", e);
        }
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            LoggingUtils.logTranslated("database.close.success");
        }
    }
}