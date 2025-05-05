package com.soystargaze.vitamin.database;

import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DatabaseHandler {

    private static HikariDataSource dataSource;

    public static void initialize(JavaPlugin plugin) {
        if (dataSource != null) {
            TextHandler.get().logTranslated("database.already_initialized");
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
            TextHandler.get().logTranslated("database.init_error", e);
            throw new IllegalStateException("Database initialization failed.", e);
        }
    }

    private static void initializeSQLite(JavaPlugin plugin) throws SQLException {
        File dbFolder = new File(plugin.getDataFolder(), "Data");
        if (!dbFolder.exists() && !dbFolder.mkdirs()) {
            throw new SQLException("Could not create folder: " + dbFolder.getAbsolutePath());
        }

        String dbFilePath = new File(dbFolder, "vitamin.db").getAbsolutePath();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFilePath);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("Vitamin-SQLite");

        dataSource = new HikariDataSource(hikariConfig);
        TextHandler.get().logTranslated("database.sqlite.success");
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
        TextHandler.get().logTranslated("database.mysql.success");
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
        TextHandler.get().logTranslated("database.mariadb.success");
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
        TextHandler.get().logTranslated("database.postgresql.success");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("The connection pool has not been initialized.");
        }
        return dataSource.getConnection();
    }

    private static void createTables() {
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            String createModules = "CREATE TABLE IF NOT EXISTS player_modules (" +
                    "player_id VARCHAR(36) NOT NULL," +
                    "module_key VARCHAR(100) NOT NULL," +
                    "enabled BOOLEAN NOT NULL," +
                    "PRIMARY KEY (player_id, module_key)" +
                    ");";
            stmt.executeUpdate(createModules);

            String createDeaths = "CREATE TABLE IF NOT EXISTS player_deaths (" +
                    "player_id   VARCHAR(36) PRIMARY KEY," +
                    "world       VARCHAR(100) NOT NULL," +
                    "x           DOUBLE       NOT NULL," +
                    "y           DOUBLE       NOT NULL," +
                    "z           DOUBLE       NOT NULL," +
                    "yaw         FLOAT        NOT NULL," +
                    "pitch       FLOAT        NOT NULL" +
                    ");";
            stmt.executeUpdate(createDeaths);

            String createMaps = """
                CREATE TABLE IF NOT EXISTS player_death_maps (
                  player_id VARCHAR(36) NOT NULL,
                  map_id    SMALLINT     NOT NULL,
                  PRIMARY KEY(player_id, map_id)
                );
                """;
            stmt.executeUpdate(createMaps);

            TextHandler.get().logTranslated("database.tables.success");
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.tables.error", e);
        }
    }

    public static List<Short> getDeathMapIds() {
        List<Short> result = new ArrayList<>();
        String sql = "SELECT map_id FROM player_death_maps";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getShort("map_id"));
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.query_error", e);
        }
        return result;
    }

    public static void saveDeathMapId(UUID playerId, short mapId) {
        String sql = """
            REPLACE INTO player_death_maps (player_id, map_id)
            VALUES (?, ?);
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setShort(2, mapId);
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.deathmap.save_error", e);
        }
    }

    public static void saveDeathLocation(UUID playerId, Location loc) {
        String sql = """
            REPLACE INTO player_deaths
              (player_id, world, x, y, z, yaw, pitch)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, Objects.requireNonNull(loc.getWorld()).getName());
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6,  loc.getYaw());
            ps.setFloat(7,  loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.death_location.save_error", e);
        }
    }

    public static Location getDeathLocation(UUID playerId) {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM player_deaths WHERE player_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    World world = Bukkit.getWorld(rs.getString("world"));
                    if (world == null) return null;
                    double x     = rs.getDouble("x");
                    double y     = rs.getDouble("y");
                    double z     = rs.getDouble("z");
                    float  yaw   = rs.getFloat("yaw");
                    float  pitch = rs.getFloat("pitch");
                    return new Location(world, x, y, z, yaw, pitch);
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.query_error", e);
        }
        return null;
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
            TextHandler.get().logTranslated("database.query_error", e);
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
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            TextHandler.get().logTranslated("database.close.success");
        }
    }
}