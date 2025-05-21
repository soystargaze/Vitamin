package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import com.soystargaze.vitamin.utils.text.legacy.LegacyTranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

@SuppressWarnings("deprecation")
public class WaystoneModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<Location, Waystone> waystones = new HashMap<>();
    private final Map<UUID, PendingWaystone> pendingWaystones = new HashMap<>();
    private final Map<UUID, Waystone> renamingWaystones = new HashMap<>();
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
            TextDisplay hologram = (TextDisplay) loc.getWorld().spawnEntity(
                    loc.clone().add(0.5, 2.5, 0.5), EntityType.TEXT_DISPLAY);
            hologram.setText("§r" + data.name());
            hologram.setBillboard(Display.Billboard.CENTER);
            hologram.setSeeThrough(true);
            hologram.setShadowed(false);
            hologram.setBrightness(new Display.Brightness(15, 15));
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
            pendingWaystones.put(player.getUniqueId(), new PendingWaystone(below, System.currentTimeMillis()));
            TextHandler.get().sendMessage(player, "waystone.enter_new_name");
        } else if (above.getBlock().getType() == Material.LODESTONE) {
            pendingWaystones.put(player.getUniqueId(), new PendingWaystone(loc, System.currentTimeMillis()));
            TextHandler.get().sendMessage(player, "waystone.enter_new_name");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (renamingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            String newName = event.getMessage().trim();
            Waystone waystone = renamingWaystones.remove(playerId);
            Bukkit.getScheduler().runTask(plugin, () -> {
                waystone.getHologram().setText("§e" + newName);
                waystone.setName(newName);
                DatabaseHandler.updateWaystoneName(waystone.getId(), newName);
            });
            TextHandler.get().sendMessage(player, "waystone.renamed", newName);
            return;
        }

        if (pendingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            PendingWaystone pending = pendingWaystones.get(playerId);
            Location loc = pending.location();
            if (loc.getBlock().getType() != Material.LODESTONE || loc.clone().add(0, 1, 0).getBlock().getType() != Material.LODESTONE) {
                TextHandler.get().sendMessage(player, "waystone.blocks_missing");
                pendingWaystones.remove(playerId);
                return;
            }
            String name = event.getMessage().trim();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (waystones.containsKey(loc)) {
                    TextHandler.get().sendMessage(player, "waystone.already_exists");
                    return;
                }
                TextDisplay hologram = (TextDisplay) loc.getWorld().spawnEntity(
                        loc.clone().add(0.5, 2.5, 0.5), EntityType.TEXT_DISPLAY);
                hologram.setText("§e" + name);
                hologram.setBillboard(Display.Billboard.CENTER);
                hologram.setSeeThrough(true);
                hologram.setShadowed(false);
                hologram.setBrightness(new Display.Brightness(15, 15));
                Waystone waystone = new Waystone(-1, loc, name, playerId);
                waystone.registerPlayer(playerId);
                waystone.setHologram(hologram);
                waystones.put(loc, waystone);
                saveWaystone(waystone);
                TextHandler.get().sendMessage(player, "waystone.created", name);
            });
            pendingWaystones.remove(playerId);
        }
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

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LODESTONE) return;

        Location loc = block.getLocation();
        Location below = loc.clone().subtract(0, 1, 0);
        Location above = loc.clone().add(0, 1, 0);

        Location baseLoc = null;
        if (above.getBlock().getType() == Material.LODESTONE) {
            baseLoc = loc;
        } else if (below.getBlock().getType() == Material.LODESTONE) {
            baseLoc = below;
        }

        if (baseLoc == null) return;

        Waystone waystone = waystones.get(baseLoc);
        if (waystone == null) return;

        event.setCancelled(true);
        UUID playerId = player.getUniqueId();

        if (player.isSneaking() && waystone.getCreator().equals(playerId)) {
            renamingWaystones.put(playerId, waystone);
            TextHandler.get().sendMessage(player, "waystone.enter_new_name_for_rename");
            return;
        }

        if (!waystone.isRegistered(playerId)) {
            waystone.registerPlayer(playerId);
            DatabaseHandler.registerPlayerToWaystone(waystone.getId(), playerId);
            TextHandler.get().sendMessage(player, "waystone.registered", waystone.getName());
        }

        openWaystoneInventory(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        for (Map.Entry<UUID, PendingWaystone> entry : pendingWaystones.entrySet()) {
            Location baseLoc = entry.getValue().location();
            Location topLoc = baseLoc.clone().add(0, 1, 0);
            if (loc.equals(baseLoc) || loc.equals(topLoc)) {
                UUID playerId = entry.getKey();
                pendingWaystones.remove(playerId);
                Player targetPlayer = Bukkit.getPlayer(playerId);
                if (targetPlayer != null) {
                    TextHandler.get().sendMessage(targetPlayer, "waystone.creation_canceled");
                }
                return;
            }
        }

        Waystone waystone = waystones.get(loc);
        if (waystone == null) {
            Location below = loc.clone().subtract(0, 1, 0);
            waystone = waystones.get(below);
            if (waystone != null) {
                loc = below;
            } else {
                return;
            }
        }

        UUID playerId = player.getUniqueId();
        boolean isCreator = waystone.getCreator().equals(playerId);
        boolean isAdmin = player.hasPermission("vitamin.module.waystone.admin");
        boolean isOp = player.isOp();

        if (onlyCreatorCanBreak && !isCreator) {
            if (!(isAdmin && (isOp || waystone.isAdminCreated()))) {
                TextHandler.get().sendMessage(player, "waystone.only_creator_can_break");
                event.setCancelled(true);
                return;
            }
        }

        if (waystone.isAdminCreated() && !(isOp || isCreator)) {
            TextHandler.get().sendMessage(player, "waystone.only_operator_or_creator_can_break");
            event.setCancelled(true);
            return;
        }

        waystones.remove(loc);
        waystone.getHologram().remove();
        removeWaystone(waystone);
        TextHandler.get().sendMessage(player, "waystone.destroyed", waystone.getName());

        Location otherLoc;
        if (event.getBlock().getLocation().equals(loc)) {
            otherLoc = loc.clone().add(0, 1, 0);
        } else {
            otherLoc = loc;
        }

        Block otherBlock = otherLoc.getBlock();
        if (otherBlock.getType() == Material.LODESTONE) {
            otherBlock.setType(Material.AIR);
            otherLoc.getWorld().dropItemNaturally(otherLoc, new ItemStack(Material.LODESTONE));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (!pendingWaystones.containsKey(playerId)) return;

        PendingWaystone pending = pendingWaystones.get(playerId);
        long timeElapsed = System.currentTimeMillis() - pending.creationTime();
        if (timeElapsed > 30000) { // 30 seconds
            Location loc = pending.location();
            if (player.getLocation().distanceSquared(loc) > 100) { // 10 blocks squared
                if (loc.getBlock().getType() == Material.LODESTONE && loc.clone().add(0, 1, 0).getBlock().getType() == Material.LODESTONE) {
                    String defaultName = "Waystone";
                    TextDisplay hologram = (TextDisplay) loc.getWorld().spawnEntity(
                            loc.clone().add(0.5, 1.5, 0.5), EntityType.TEXT_DISPLAY);
                    hologram.setText("§e" + defaultName);
                    hologram.setBillboard(Display.Billboard.CENTER);
                    hologram.setSeeThrough(true);
                    hologram.setShadowed(false);
                    hologram.setBrightness(new Display.Brightness(15, 15));
                    Waystone waystone = new Waystone(-1, loc, defaultName, playerId);
                    waystone.registerPlayer(playerId);
                    waystone.setHologram(hologram);
                    waystones.put(loc, waystone);
                    saveWaystone(waystone);
                    TextHandler.get().sendMessage(player, "waystone.created_with_default", defaultName);
                    pendingWaystones.remove(playerId);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingWaystones.remove(playerId);
        renamingWaystones.remove(playerId);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String translatedTitle = ChatColor.translateAlternateColorCodes('&', LegacyTranslationHandler.get("waystone.inventory.title"));
        if (!event.getView().getTitle().equals(translatedTitle)) return;

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        String name = clickedItem.getItemMeta().getDisplayName().replace("§e", "");

        for (Waystone waystone : waystones.values()) {
            if (waystone.getName().equals(name) && waystone.isRegistered(player.getUniqueId())) {
                player.closeInventory();
                player.teleport(waystone.getLocation().clone().add(0.5, 1, 0.5));
                player.sendMessage(LegacyTranslationHandler.getPlayerMessage("waystone.teleported", name));
                break;
            }
        }
    }

    private void openWaystoneInventory(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', LegacyTranslationHandler.get("waystone.inventory.title"));
        Inventory inv = Bukkit.createInventory(null, 27, title);
        for (Waystone waystone : waystones.values()) {
            if (waystone.isRegistered(player.getUniqueId())) {
                ItemStack item = new ItemStack(Material.LODESTONE);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§e" + waystone.getName());
                String locationText = ChatColor.translateAlternateColorCodes('&', LegacyTranslationHandler.getFormatted(
                        "waystone.inventory.item.location",
                        waystone.getLocation().getBlockX(),
                        waystone.getLocation().getBlockY(),
                        waystone.getLocation().getBlockZ()
                ));
                String clickText = ChatColor.translateAlternateColorCodes('&', LegacyTranslationHandler.get("waystone.inventory.item.click_to_teleport"));
                meta.setLore(Arrays.asList(locationText, clickText));
                item.setItemMeta(meta);
                inv.addItem(item);
            }
        }
        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
    }

    private static class Waystone {
        private int id;
        private final Location location;
        private String name;
        private final UUID creator;
        private final Set<UUID> registeredPlayers;
        private TextDisplay hologram;
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

        public void setName(String name) {
            this.name = name;
        }

        public UUID getCreator() {
            return creator;
        }

        public TextDisplay getHologram() {
            return hologram;
        }

        public void setHologram(TextDisplay hologram) {
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

    private record PendingWaystone(Location location, long creationTime) {
    }
}