package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WaystoneModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<Location, Waystone> waystones = new HashMap<>();
    private final Map<UUID, Location> pendingWaystones = new HashMap<>();
    private final boolean onlyCreatorCanBreak;

    public WaystoneModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.onlyCreatorCanBreak = plugin.getConfig().getBoolean("waystone.only_creator_can_break", false);
        loadWaystones();
    }

    private void loadWaystones() {
        List<DatabaseHandler.WaystoneData> dataList = DatabaseHandler.loadWaystones();
        for (DatabaseHandler.WaystoneData data : dataList) {
            Location loc = data.location();
            Waystone waystone = new Waystone(data.id(), loc, data.name(), data.creator());
            waystone.setRegisteredPlayers(DatabaseHandler.getRegisteredPlayers(data.id()));
            waystones.put(loc, waystone);
            ArmorStand hologram = (ArmorStand) loc.getWorld().spawnEntity(
                    loc.clone().add(0.5, 2.0, 0.5), EntityType.ARMOR_STAND);
            hologram.setInvisible(true);
            hologram.setGravity(false);
            hologram.setCustomName("§e" + data.name());
            hologram.setCustomNameVisible(true);
            hologram.setInvulnerable(true);
            waystone.setHologram(hologram);
        }
    }

    private void saveWaystone(Waystone waystone) {
        DatabaseHandler.WaystoneData data = new DatabaseHandler.WaystoneData(
                waystone.getId(), waystone.getLocation(), waystone.getName(), waystone.getCreator());
        int id = DatabaseHandler.saveWaystone(data);
        waystone.setId(id);
        for (UUID playerId : waystone.getRegisteredPlayers()) {
            DatabaseHandler.registerPlayerToWaystone(id, playerId);
        }
    }

    private void removeWaystone(Waystone waystone) {
        DatabaseHandler.removeWaystone(waystone.getId());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.waystone") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.waystone")) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() != Material.LODESTONE) return;

        Location loc = block.getLocation();
        Location below = loc.clone().subtract(0, 1, 0);
        Location above = loc.clone().add(0, 1, 0);

        if (below.getBlock().getType() == Material.LODESTONE) {
            pendingWaystones.put(player.getUniqueId(), below);
            player.sendMessage("§aPor favor, ingresa un nombre para tu waystone en el chat.");
        } else if (above.getBlock().getType() == Material.LODESTONE) {
            pendingWaystones.put(player.getUniqueId(), loc);
            player.sendMessage("§aPor favor, ingresa un nombre para tu waystone en el chat.");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!pendingWaystones.containsKey(playerId)) return;

        event.setCancelled(true);
        String name = event.getMessage().trim();
        Location loc = pendingWaystones.remove(playerId);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (waystones.containsKey(loc)) {
                player.sendMessage("§cYa existe una waystone en esta ubicación.");
                return;
            }

            ArmorStand hologram = (ArmorStand) loc.getWorld().spawnEntity(
                    loc.clone().add(0.5, 2.0, 0.5), EntityType.ARMOR_STAND);
            hologram.setInvisible(true);
            hologram.setGravity(false);
            hologram.setCustomName("§e" + name);
            hologram.setCustomNameVisible(true);
            hologram.setInvulnerable(true);

            Waystone waystone = new Waystone(-1, loc, name, playerId);
            waystone.registerPlayer(playerId);
            waystones.put(loc, waystone);
            saveWaystone(waystone);
            player.sendMessage("§aWaystone '" + name + "' creada con éxito!");
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.waystone") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.waystone")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() != Material.AIR) return;

        Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;
        if (loc == null) return;

        Waystone waystone = waystones.get(loc);
        if (waystone == null) {
            Location above = loc.clone().add(0, 1, 0);
            waystone = waystones.get(above);
            if (waystone == null) return;
            loc = above;
        }

        event.setCancelled(true);
        UUID playerId = player.getUniqueId();

        if (!waystone.isRegistered(playerId)) {
            waystone.registerPlayer(playerId);
            DatabaseHandler.registerPlayerToWaystone(waystone.getId(), playerId);
            player.sendMessage("§aWaystone '" + waystone.getName() + "' registrada!");
        }

        openWaystoneInventory(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        Waystone waystone = waystones.get(loc);

        if (waystone == null) {
            Location above = loc.clone().add(0, 1, 0);
            waystone = waystones.get(above);
            if (waystone == null) return;
            loc = above;
        }

        UUID playerId = player.getUniqueId();
        boolean isCreator = waystone.getCreator().equals(playerId);
        boolean isAdmin = player.hasPermission("vitamin.module.waystone.admin");
        boolean isOp = player.isOp();

        if (onlyCreatorCanBreak && !isCreator) {
            if (!(isAdmin && (isOp || waystone.isAdminCreated()))) {
                player.sendMessage("§cSolo el creador puede destruir esta waystone.");
                event.setCancelled(true);
                return;
            }
        }

        if (waystone.isAdminCreated() && !(isOp || isCreator)) {
            player.sendMessage("§cSolo un operador o el creador puede destruir esta waystone.");
            event.setCancelled(true);
            return;
        }

        waystones.remove(loc);
        waystone.getHologram().remove();
        removeWaystone(waystone);
        player.sendMessage("§cWaystone '" + waystone.getName() + "' destruida.");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("Waystones Registradas")) return;

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        String name = clickedItem.getItemMeta().getDisplayName().replace("§e", "");

        for (Waystone waystone : waystones.values()) {
            if (waystone.getName().equals(name) && waystone.isRegistered(player.getUniqueId())) {
                player.closeInventory();
                player.teleport(waystone.getLocation().clone().add(0.5, 1, 0.5));
                player.sendMessage("§aTeletransportado a '" + name + "'.");
                break;
            }
        }
    }

    private void openWaystoneInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Waystones Registradas");
        for (Waystone waystone : waystones.values()) {
            if (waystone.isRegistered(player.getUniqueId())) {
                ItemStack item = new ItemStack(Material.LODESTONE);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§e" + waystone.getName());
                meta.setLore(Arrays.asList(
                        "§7Ubicación: " + waystone.getLocation().getBlockX() + ", " +
                                waystone.getLocation().getBlockY() + ", " +
                                waystone.getLocation().getBlockZ(),
                        "§7Clic para teletransportarte"
                ));
                item.setItemMeta(meta);
                inv.addItem(item);
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
    }

    private static class Waystone {
        private int id;
        private final Location location;
        private final String name;
        private final UUID creator;
        private final Set<UUID> registeredPlayers;
        private ArmorStand hologram;
        private final boolean isAdminCreated;

        public Waystone(int id, Location location, String name, UUID creator) {
            this.id = id;
            this.location = location;
            this.name = name;
            this.creator = creator;
            this.registeredPlayers = new HashSet<>();
            this.isAdminCreated = Bukkit.getPlayer(creator) != null && Objects.requireNonNull(Bukkit.getPlayer(creator)).hasPermission("vitamin.module.waystone.admin");
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void registerPlayer(UUID playerId) {
            registeredPlayers.add(playerId);
        }

        public boolean isRegistered(UUID playerId) {
            return registeredPlayers.contains(playerId);
        }

        public Location getLocation() {
            return location;
        }

        public String getName() {
            return name;
        }

        public UUID getCreator() {
            return creator;
        }

        public ArmorStand getHologram() {
            return hologram;
        }

        public void setHologram(ArmorStand hologram) {
            this.hologram = hologram;
        }

        public Set<UUID> getRegisteredPlayers() {
            return registeredPlayers;
        }

        public void setRegisteredPlayers(Set<UUID> players) {
            this.registeredPlayers.clear();
            this.registeredPlayers.addAll(players);
        }

        public boolean isAdminCreated() {
            return isAdminCreated;
        }
    }
}