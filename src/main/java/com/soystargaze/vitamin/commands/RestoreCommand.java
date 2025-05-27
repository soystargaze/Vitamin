package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation", "UnstableApiUsage"})
public class RestoreCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final NamespacedKey restoreKey;

    public RestoreCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.restoreKey = new NamespacedKey(plugin, "restore_id");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!(sender instanceof Player admin)) {
            sendToSender(sender, "commands.player_only");
            return true;
        }

        if (!admin.hasPermission("vitamin.use.restore")) {
            TextHandler.get().sendMessage(admin, "commands.no_permission");
            return true;
        }

        if (args.length != 1) {
            TextHandler.get().sendMessage(admin, "commands.restore.usage");
            return true;
        }

        String targetPlayerName = args[0];
        openRestoreInventory(admin, targetPlayerName);
        return true;
    }

    private void openRestoreInventory(Player admin, String targetPlayerName) {
        List<ContainerBackup> backups = getPlayerBackups(targetPlayerName);

        if (backups.isEmpty()) {
            TextHandler.get().sendMessage(admin, "commands.restore.no_backups", targetPlayerName);
            return;
        }

        String rawTitle = ConfigHandler.getString("gui.restore.title",
                "&6Restore: %player% &7(%count% containers)");
        String title = rawTitle
                .replace("%player%", targetPlayerName)
                .replace("%count%", String.valueOf(backups.size()));
        title = org.bukkit.ChatColor.translateAlternateColorCodes('&', title);

        int size = ConfigHandler.getInt("gui.restore.size", 54);
        Inventory restoreInv = Bukkit.createInventory(null, size, title);

        String datePattern = ConfigHandler.getString("gui.restore.date-format",
                "dd/MM/yyyy HH:mm");
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);

        for (int i = 0; i < Math.min(backups.size(), size); i++) {
            ContainerBackup backup = backups.get(i);
            ItemStack displayItem = createRestoreDisplayItem(backup, dateFormat);
            restoreInv.setItem(i, displayItem);
        }

        admin.openInventory(restoreInv);
        TextHandler.get().sendMessage(admin, "commands.restore.opened", targetPlayerName, backups.size());
    }

    private ItemStack createRestoreDisplayItem(ContainerBackup backup, SimpleDateFormat dateFormat) {
        Material containerMaterial = Material.valueOf(backup.containerType);
        ItemStack displayItem = new ItemStack(containerMaterial);
        ItemMeta meta = displayItem.getItemMeta();

        List<ItemStack> previewItems = getContainerPreview(backup.chestId);

        String containerName = containerMaterial.name().toLowerCase().replace("_", " ");
        String displayNameFormat = ConfigHandler.getString("gui.restore.item.display-name",
                "&e%container_name% &7#%short_id%");
        String displayName = displayNameFormat
                .replace("%container_name%", Character.toUpperCase(containerName.charAt(0)) + containerName.substring(1))
                .replace("%short_id%", backup.chestId.substring(0, 8));
        displayName = org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName);
        meta.setDisplayName(displayName);

        List<String> loreTemplate = ConfigHandler.getStringList("gui.restore.item.lore");
        if (loreTemplate.isEmpty()) {
            loreTemplate = Arrays.asList(
                    "&7▸ ID: &f%short_id%...",
                    "&7▸ Picked up: &f%pickup_date%",
                    "&7▸ Location: &f%world% (%x%, %y%, %z%)",
                    "&7▸ Restored: %restored_status%",
                    "",
                    "&7Contents preview:",
                    "%contents%",
                    "",
                    "&e▶ Click to give a copy to your inventory"
            );
        }

        List<String> lore = new ArrayList<>();
        String restoredYes = ConfigHandler.getString("gui.restore.item.restored-yes", "&aYes");
        String restoredNo = ConfigHandler.getString("gui.restore.item.restored-no", "&cNo");
        String restoredText = backup.restored ? restoredYes : restoredNo;

        for (String line : loreTemplate) {
            if (line.contains("%contents%")) {
                if (previewItems.isEmpty()) {
                    String emptyText = ConfigHandler.getString("gui.restore.item.contents.empty",
                            "  &8• Empty");
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', emptyText));
                } else {
                    int maxPreview = ConfigHandler.getInt("gui.restore.item.contents.max-preview", 5);
                    String itemFormat = ConfigHandler.getConfig().getString("gui.restore.item.contents.item-format",
                            "  &8• &f%amount%x &7%item_name%");
                    String moreItemsFormat = ConfigHandler.getString("gui.restore.item.contents.more-format",
                            "  &8• ... and %remaining% more items");

                    int shown = 0;
                    for (ItemStack item : previewItems) {
                        if (shown >= maxPreview) {
                            int remaining = previewItems.size() - maxPreview;
                            String moreLine = moreItemsFormat.replace("%remaining%", String.valueOf(remaining));
                            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', moreLine));
                            break;
                        }
                        String itemName = getItemDisplayName(item);
                        String itemLine = itemFormat
                                .replace("%amount%", String.valueOf(item.getAmount()))
                                .replace("%item_name%", itemName);
                        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', itemLine));
                        shown++;
                    }
                }
            } else {
                String processedLine = line
                        .replace("%short_id%", backup.chestId.substring(0, 8))
                        .replace("%pickup_date%", dateFormat.format(new Date(backup.pickupTimestamp)))
                        .replace("%world%", backup.worldName)
                        .replace("%x%", String.valueOf(backup.x))
                        .replace("%y%", String.valueOf(backup.y))
                        .replace("%z%", String.valueOf(backup.z))
                        .replace("%restored_status%", restoredText);
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', processedLine));
            }
        }

        meta.setLore(lore);

        meta.getPersistentDataContainer().set(restoreKey, PersistentDataType.STRING, backup.chestId);

        displayItem.setItemMeta(meta);
        return displayItem;
    }

    private List<ItemStack> getContainerPreview(String chestId) {
        List<ItemStack> items = new ArrayList<>();
        String sql = "SELECT item_data FROM chest_contents WHERE chest_id = ? ORDER BY slot LIMIT 10";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, chestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String itemData = rs.getString("item_data");
                    ItemStack item = deserializeItemStack(itemData);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.restore.preview_error", e);
        }

        return items;
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            return displayName.replaceAll("§[0-9a-fk-or]", "");
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }

    private List<ContainerBackup> getPlayerBackups(String playerName) {
        List<ContainerBackup> backups = new ArrayList<>();
        String sql = """
            SELECT chest_id, player_uuid, player_name, container_type, pickup_timestamp,\s
                   restored, world_name, x_coord, y_coord, z_coord\s
            FROM container_backups\s
            WHERE player_name = ?\s
            ORDER BY pickup_timestamp DESC
           \s""";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ContainerBackup backup = new ContainerBackup();
                    backup.chestId = rs.getString("chest_id");
                    backup.playerUuid = rs.getString("player_uuid");
                    backup.playerName = rs.getString("player_name");
                    backup.containerType = rs.getString("container_type");
                    backup.pickupTimestamp = rs.getLong("pickup_timestamp");
                    backup.restored = rs.getBoolean("restored");
                    backup.worldName = rs.getString("world_name");
                    backup.x = rs.getInt("x_coord");
                    backup.y = rs.getInt("y_coord");
                    backup.z = rs.getInt("z_coord");
                    backups.add(backup);
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.restore.load_error", e);
        }

        return backups;
    }

    public ItemStack createRestoreContainer(String chestId) {
        ContainerBackup backup = getBackupInfo(chestId);
        if (backup == null) return null;

        Material containerType = Material.valueOf(backup.containerType);
        ItemStack containerItem = new ItemStack(containerType);

        if (!(containerItem.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta meta)) {
            return null;
        }

        org.bukkit.block.Container tempContainer;
        try {
            if (containerType == Material.CHEST || containerType == Material.TRAPPED_CHEST) {
                tempContainer = (org.bukkit.block.Container) Material.CHEST.createBlockData().createBlockState();
            } else {
                tempContainer = (org.bukkit.block.Container) containerType.createBlockData().createBlockState();
            }
        } catch (Exception e) {
            TextHandler.get().logTranslated("database.restore.create_error", e);
            return null;
        }

        loadChestContentsToContainer(chestId, tempContainer);

        meta.setBlockState(tempContainer);

        String containerName = containerType.name().toLowerCase().replace("_", " ");
        String displayNameFormat = ConfigHandler.getString("gui.restore.restored-item.display-name",
                "&6Restored %container_name%");
        String displayName = displayNameFormat
                .replace("%container_name%", Character.toUpperCase(containerName.charAt(0)) + containerName.substring(1));
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                ConfigHandler.getString("gui.restore.restored-item.date-format", "dd/MM/yyyy HH:mm"));

        List<String> loreTemplate = ConfigHandler.getStringList("gui.restore.restored-item.lore");
        if (loreTemplate.isEmpty()) {
            loreTemplate = Arrays.asList(
                    "&7▸ Original owner: &f%owner%",
                    "&7▸ Backup ID: &f%short_id%...",
                    "&7▸ Picked up: &f%pickup_date%",
                    "&7▸ Original location: &f%world% (%x%, %y%, %z%)",
                    "",
                    "&e▶ This is a restored copy from backup"
            );
        }

        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            String processedLine = line
                    .replace("%owner%", backup.playerName)
                    .replace("%short_id%", backup.chestId.substring(0, 8))
                    .replace("%pickup_date%", dateFormat.format(new Date(backup.pickupTimestamp)))
                    .replace("%world%", backup.worldName)
                    .replace("%x%", String.valueOf(backup.x))
                    .replace("%y%", String.valueOf(backup.y))
                    .replace("%z%", String.valueOf(backup.z));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', processedLine));
        }
        meta.setLore(lore);

        NamespacedKey chestIdKey = new NamespacedKey(plugin, "chest_id");
        NamespacedKey storedBlockKey = new NamespacedKey(plugin, "stored_block");

        meta.getPersistentDataContainer().set(chestIdKey, PersistentDataType.STRING, chestId);
        meta.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, containerType.name());

        containerItem.setItemMeta(meta);

        markAsRestored(chestId);

        return containerItem;
    }

    private void loadChestContentsToContainer(String chestId, org.bukkit.block.Container container) {
        String sql = "SELECT slot, item_data FROM chest_contents WHERE chest_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, chestId);
            try (ResultSet rs = ps.executeQuery()) {
                org.bukkit.inventory.Inventory inventory = container.getInventory();

                while (rs.next()) {
                    int slot = rs.getInt("slot");
                    String itemData = rs.getString("item_data");
                    ItemStack item = deserializeItemStack(itemData);

                    if (item != null && slot < inventory.getSize()) {
                        inventory.setItem(slot, item);
                    }
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.chest.load_error", e);
        }
    }

    private ContainerBackup getBackupInfo(String chestId) {
        String sql = """
            SELECT chest_id, player_uuid, player_name, container_type, pickup_timestamp,\s
                   restored, world_name, x_coord, y_coord, z_coord\s
            FROM container_backups\s
            WHERE chest_id = ?
           \s""";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, chestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ContainerBackup backup = new ContainerBackup();
                    backup.chestId = rs.getString("chest_id");
                    backup.playerUuid = rs.getString("player_uuid");
                    backup.playerName = rs.getString("player_name");
                    backup.containerType = rs.getString("container_type");
                    backup.pickupTimestamp = rs.getLong("pickup_timestamp");
                    backup.restored = rs.getBoolean("restored");
                    backup.worldName = rs.getString("world_name");
                    backup.x = rs.getInt("x_coord");
                    backup.y = rs.getInt("y_coord");
                    backup.z = rs.getInt("z_coord");
                    return backup;
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.restore.info_error", e);
        }

        return null;
    }

    private void markAsRestored(String chestId) {
        String sql = "UPDATE container_backups SET restored = TRUE WHERE chest_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, chestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.restore.mark_error", e);
        }
    }

    private ItemStack deserializeItemStack(String itemData) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(itemData);
            return config.getItemStack("item");
        } catch (Exception e) {
            TextHandler.get().logTranslated("database.chest.deserialize_error", e);
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void sendToSender(CommandSender sender, String key, Object... args) {
        if (sender instanceof Player player) {
            TextHandler.get().sendMessage(player, key, args);
        } else {
            Object msg = TextHandler.get().getMessage(key, args);
            String legacyMessage;

            if (msg instanceof Component comp) {
                legacyMessage = LegacyComponentSerializer.legacyAmpersand().serialize(comp);
            } else {
                legacyMessage = msg.toString();
            }

            sender.sendMessage(legacyMessage);
        }
    }

    public static class ContainerBackup {
        public String chestId;
        public String playerUuid;
        public String playerName;
        public String containerType;
        public long pickupTimestamp;
        public boolean restored;
        public String worldName;
        public int x, y, z;
    }
}

