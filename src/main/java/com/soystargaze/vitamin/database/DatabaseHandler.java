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
import java.util.*;

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

            String createChestContents = """
            CREATE TABLE IF NOT EXISTS chest_contents (
              chest_id VARCHAR(36) NOT NULL,
              slot INTEGER NOT NULL,
              item_data TEXT NOT NULL,
              PRIMARY KEY (chest_id, slot)
            );
            """;
            stmt.executeUpdate(createChestContents);

            String createContainerBackups = """
            CREATE TABLE IF NOT EXISTS container_backups (
              chest_id VARCHAR(36) PRIMARY KEY,
              player_uuid VARCHAR(36) NOT NULL,
              player_name VARCHAR(32) NOT NULL,
              container_type VARCHAR(32) NOT NULL,
              pickup_timestamp BIGINT NOT NULL,
              restored BOOLEAN DEFAULT FALSE,
              world_name VARCHAR(64),
              x_coord INTEGER,
              y_coord INTEGER,
              z_coord INTEGER
            );
            """;
            stmt.executeUpdate(createContainerBackups);

            String createVaultReactivations = """
            CREATE TABLE IF NOT EXISTS vault_reactivations (
              vault_world VARCHAR(100) NOT NULL,
              vault_x INT NOT NULL,
              vault_y INT NOT NULL,
              vault_z INT NOT NULL,
              player_id VARCHAR(36) NOT NULL,
              opening_count INT NOT NULL,
              last_opening_time BIGINT NOT NULL,
              PRIMARY KEY (vault_world, vault_x, vault_y, vault_z, player_id)
             );
            """;
            stmt.executeUpdate(createVaultReactivations);

            String createWaystones = """
                CREATE TABLE IF NOT EXISTS waystones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world VARCHAR(100) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    creator VARCHAR(36) NOT NULL,
                    is_public BOOLEAN DEFAULT TRUE,
                    icon_data TEXT DEFAULT NULL
                );
                """;
            stmt.executeUpdate(createWaystones);

            String createWaystonePermissions = """
                CREATE TABLE IF NOT EXISTS waystone_permissions (
                    waystone_id INTEGER NOT NULL,
                    player_id VARCHAR(36) NOT NULL,
                    PRIMARY KEY (waystone_id, player_id),
                    FOREIGN KEY (waystone_id) REFERENCES waystones(id) ON DELETE CASCADE
                );
                """;
            stmt.executeUpdate(createWaystonePermissions);

            String createRegistrations = """
                CREATE TABLE IF NOT EXISTS waystone_registrations (
                    waystone_id INTEGER NOT NULL,
                    player_id VARCHAR(36) NOT NULL,
                    PRIMARY KEY (waystone_id, player_id),
                    FOREIGN KEY (waystone_id) REFERENCES waystones(id) ON DELETE CASCADE
                );
                """;
            stmt.executeUpdate(createRegistrations);

            TextHandler.get().logTranslated("database.tables.success");
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.tables.error", e);
        }
    }

    public static void updateWaystoneVisibility(int waystoneId, boolean isPublic) {
        String sql = "UPDATE waystones SET is_public = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isPublic);
            ps.setInt(2, waystoneId);
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static void addWaystonePermission(int waystoneId, UUID playerId) {
        String sql = "INSERT OR IGNORE INTO waystone_permissions (waystone_id, player_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waystoneId);
            ps.setString(2, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static void removeWaystonePermission(int waystoneId, UUID playerId) {
        String sql = "DELETE FROM waystone_permissions WHERE waystone_id = ? AND player_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waystoneId);
            ps.setString(2, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static Set<UUID> getWaystonePermissions(int waystoneId) {
        Set<UUID> permissions = new HashSet<>();
        String sql = "SELECT player_id FROM waystone_permissions WHERE waystone_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waystoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.add(UUID.fromString(rs.getString("player_id")));
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.query_error", e);
        }
        return permissions;
    }

    public static boolean getWaystoneVisibility(int waystoneId) {
        String sql = "SELECT is_public FROM waystones WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waystoneId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_public");
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.query_error", e);
        }
        return true;
    }

    public static void clearWaystones() {
        String sql = "DELETE FROM waystones";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static List<WaystoneData> loadWaystones() {
        List<WaystoneData> waystones = new ArrayList<>();
        String sql = "SELECT id, world, x, y, z, name, creator, COALESCE(is_public, 1) as is_public, icon_data FROM waystones";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String worldName = rs.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                String name = rs.getString("name");
                UUID creator = UUID.fromString(rs.getString("creator"));
                boolean isPublic = rs.getBoolean("is_public");
                String iconData = rs.getString("icon_data");
                Location location = new Location(world, x, y, z);
                waystones.add(new WaystoneData(id, location, name, creator, isPublic, iconData));
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.query_error", e);
        }
        return waystones;
    }

    public static int saveWaystone(WaystoneData waystone) {
        String sql = "INSERT INTO waystones (world, x, y, z, name, creator, is_public, icon_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, waystone.location().getWorld().getName());
            ps.setDouble(2, waystone.location().getX());
            ps.setDouble(3, waystone.location().getY());
            ps.setDouble(4, waystone.location().getZ());
            ps.setString(5, waystone.name());
            ps.setString(6, waystone.creator().toString());
            ps.setBoolean(7, waystone.isPublic());
            ps.setString(8, waystone.iconData());
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
        return -1;
    }

    public static void removeWaystone(int waystoneId) {
        String sql = "DELETE FROM waystones WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waystoneId);
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static void updateWaystoneName(int id, String name) {
        String sql = "UPDATE waystones SET name = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static void updateWaystoneIcon(int waystoneId, String iconData) {
        String sql = "UPDATE waystones SET icon_data = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, iconData);
            ps.setInt(2, waystoneId);
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static String getWaystoneIcon(int waystoneId) {
        String sql = "SELECT icon_data FROM waystones WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waystoneId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("icon_data");
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.query_error", e);
        }
        return null;
    }

    public static void registerPlayerToWaystone(int waystoneId, UUID playerId) {
        String sql = "INSERT INTO waystone_registrations (waystone_id, player_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waystoneId);
            ps.setString(2, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
        }
    }

    public static Set<UUID> getRegisteredPlayers(int waystoneId) {
        Set<UUID> players = new HashSet<>();
        String sql = "SELECT player_id FROM waystone_registrations WHERE waystone_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waystoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(UUID.fromString(rs.getString("player_id")));
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.query_error", e);
        }
        return players;
    }

    public record WaystoneData(int id, Location location, String name, UUID creator, boolean isPublic, String iconData) {
    }

    public static ReactivationData getReactivationData(Location vaultLoc, UUID playerId) {
        String sql = "SELECT opening_count, last_opening_time FROM vault_reactivations WHERE vault_world = ? AND vault_x = ? AND vault_y = ? AND vault_z = ? AND player_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vaultLoc.getWorld().getName());
            ps.setInt(2, vaultLoc.getBlockX());
            ps.setInt(3, vaultLoc.getBlockY());
            ps.setInt(4, vaultLoc.getBlockZ());
            ps.setString(5, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int openingCount = rs.getInt("opening_count");
                    long lastOpeningTime = rs.getLong("last_opening_time");
                    return new ReactivationData(openingCount, lastOpeningTime);
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.query_error", e);
        }
        return null;
    }

    public static void saveReactivationData(Location vaultLoc, UUID playerId, ReactivationData data) {
        String sql = """
        REPLACE INTO vault_reactivations
            (vault_world, vault_x, vault_y, vault_z, player_id, opening_count, last_opening_time)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vaultLoc.getWorld().getName());
            ps.setInt(2, vaultLoc.getBlockX());
            ps.setInt(3, vaultLoc.getBlockY());
            ps.setInt(4, vaultLoc.getBlockZ());
            ps.setString(5, playerId.toString());
            ps.setInt(6, data.openingCount());
            ps.setLong(7, data.lastOpeningTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.update_error", e);
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