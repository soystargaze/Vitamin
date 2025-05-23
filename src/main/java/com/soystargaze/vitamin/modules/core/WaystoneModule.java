package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import com.soystargaze.vitamin.utils.text.legacy.LegacyTranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Particle.DustOptions;

import java.util.*;

@SuppressWarnings("deprecation")
public class WaystoneModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<Location, Waystone> waystones = new HashMap<>();
    private final Map<UUID, PendingWaystone> pendingWaystones = new HashMap<>();
    private final Map<UUID, Waystone> renamingWaystones = new HashMap<>();
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();
    private final Map<UUID, Location> playerTeleportLocations = new HashMap<>();
    private final boolean onlyCreatorCanBreak;
    private final long autoCreateTime;
    private final double autoCreateDistanceSquared;
    private final String defaultWaystoneName;
    private final int teleportDelay;
    private final boolean cancelTeleportOnMove;

    public WaystoneModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.onlyCreatorCanBreak = plugin.getConfig().getBoolean("waystone.only_creator_can_break", false);
        this.autoCreateTime = plugin.getConfig().getLong("waystone.auto_create_time", 30000L);
        this.autoCreateDistanceSquared = plugin.getConfig().getDouble("waystone.auto_create_distance_squared", 100.0);
        this.defaultWaystoneName = plugin.getConfig().getString("waystone.default_name", "Waystone");
        this.teleportDelay = plugin.getConfig().getInt("waystone.teleport_delay", 3);
        this.cancelTeleportOnMove = plugin.getConfig().getBoolean("waystone.cancel_teleport_on_move", true);
        loadWaystones();

        // Verifica las waystones y genera partículas ambientales
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Waystone waystone : new ArrayList<>(waystones.values())) {
                Location loc = waystone.getLocation();
                if (loc.getBlock().getType() != Material.LODESTONE ||
                        loc.clone().add(0, 1, 0).getBlock().getType() != Material.LODESTONE) {
                    waystone.getHologram().remove();
                    waystones.remove(loc);
                    removeWaystone(waystone);
                } else {
                    TextDisplay hologram = waystone.getHologram();
                    if (hologram.isDead()) {
                        TextDisplay newHologram = createHologram(loc, waystone.getName());
                        waystone.setHologram(newHologram);
                    }

                    // Efecto de partícula ambiental
                    if (Math.random() < 0.1) { // Solo 10% de probabilidad para reducir sobrecarga
                        spawnAmbientParticles(loc);
                    }
                }
            }
        }, 100L, 100L);

        // Animación para las waystones durante teleportación
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Location> entry : playerTeleportLocations.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                Location targetLocation = entry.getValue();
                if (player != null && targetLocation != null) {
                    spawnTeleportParticles(player.getLocation(), targetLocation);
                }
            }
        }, 2L, 2L);
    }

    private void spawnAmbientParticles(Location location) {
        Location particleLocation = location.clone().add(0.5, 2.0, 0.5);
        location.getWorld().spawnParticle(
                Particle.WITCH,
                particleLocation,
                3,
                0.2, 0.2, 0.2,
                0.01);
    }

    private void spawnTeleportParticles(Location playerLoc, Location targetLoc) {
        // Partículas en la ubicación del jugador
        playerLoc.getWorld().spawnParticle(
                Particle.PORTAL,
                playerLoc.clone().add(0, 1, 0),
                20,
                0.5, 1, 0.5,
                0.1);

        // Partículas en la Waystone de destino
        DustOptions dustOptions = new DustOptions(Color.fromRGB(55, 166, 229), 1.0f);
        targetLoc.getWorld().spawnParticle(
                Particle.DUST,
                targetLoc.clone().add(0.5, 1.5, 0.5),
                10,
                0.5, 1, 0.5,
                0,
                dustOptions);
    }

    private void playWaystoneActivateSound(Location location) {
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
        location.getWorld().playSound(location, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.BLOCKS, 0.5f, 1.5f);
    }

    private void playWaystoneDeactivateSound(Location location) {
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
    }

    private void playWaystoneBreakSound(Location location) {
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    private void playWaystoneCreateSound(Location location) {
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0f, 1.5f);
        location.getWorld().playSound(location, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    private void spawnCreationParticles(Location location) {
        Location particleLocation = location.clone().add(0.5, 1.5, 0.5);
        location.getWorld().spawnParticle(
                Particle.END_ROD,
                particleLocation,
                50,
                0.6, 1.0, 0.6,
                0.1);

        location.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                particleLocation,
                30,
                0.3, 0.5, 0.3,
                0.3);
    }

    private void playTeleportBeginSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.5f, 1.5f);
    }

    private void playTeleportSound(Player player, Location destinationLocation) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        destinationLocation.getWorld().playSound(destinationLocation, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private void spawnTeleportCompleteParticles(Location location) {
        location.getWorld().spawnParticle(
                Particle.FLASH,
                location.clone().add(0, 1, 0),
                2,
                0.1, 0.1, 0.1,
                0);

        location.getWorld().spawnParticle(
                Particle.PORTAL,
                location.clone().add(0, 1, 0),
                50,
                0.5, 1, 0.5,
                0.5);
    }

    private void loadWaystones() {
        List<DatabaseHandler.WaystoneData> dataList = DatabaseHandler.loadWaystones();
        for (DatabaseHandler.WaystoneData data : dataList) {
            Location loc = data.location();
            Waystone waystone = new Waystone(data.id(), loc, data.name(), data.creator());
            waystone.setRegisteredPlayers(DatabaseHandler.getRegisteredPlayers(data.id()));
            waystones.put(loc, waystone);
            TextDisplay hologram = createHologram(loc, data.name());
            waystone.setHologram(hologram);
        }
    }

    private TextDisplay createHologram(Location loc, String name) {
        TextDisplay hologram = (TextDisplay) loc.getWorld().spawnEntity(
                loc.clone().add(0.5, 2.5, 0.5), EntityType.TEXT_DISPLAY);
        hologram.setText("§e" + name);
        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setSeeThrough(true);
        hologram.setShadowed(false);
        hologram.setBrightness(new Display.Brightness(15, 15));
        return hologram;
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
            playWaystoneActivateSound(below);
            TextHandler.get().sendMessage(player, "waystone.enter_new_name");
        } else if (above.getBlock().getType() == Material.LODESTONE) {
            pendingWaystones.put(player.getUniqueId(), new PendingWaystone(loc, System.currentTimeMillis()));
            playWaystoneActivateSound(loc);
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

                // Efectos al renombrar
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.8f);
                Location waystoneLoc = waystone.getLocation();
                waystoneLoc.getWorld().spawnParticle(
                        Particle.ENCHANT,
                        waystoneLoc.clone().add(0.5, 2.5, 0.5),
                        20,
                        0.5, 0.5, 0.5,
                        1);
            });
            TextHandler.get().sendMessage(player, "waystone.renamed", newName);
            return;
        }

        if (pendingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            PendingWaystone pending = pendingWaystones.get(playerId);
            Location loc = pending.location();
            if (loc.getBlock().getType() != Material.LODESTONE || loc.clone().add(0, 1, 0).getBlock().getType() != Material.LODESTONE) {
                TextHandler.get().sendMessage(player, "waystone.keys_missing");
                pendingWaystones.remove(playerId);
                return;
            }
            String name = event.getMessage().trim();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (waystones.containsKey(loc)) {
                    TextHandler.get().sendMessage(player, "waystone.already_exists");
                    playWaystoneDeactivateSound(loc);
                    return;
                }
                TextDisplay hologram = createHologram(loc, name);
                Waystone waystone = new Waystone(-1, loc, name, playerId);
                waystone.registerPlayer(playerId);
                waystone.setHologram(hologram);
                waystones.put(loc, waystone);
                saveWaystone(waystone);

                // Efectos al crear una waystone
                playWaystoneCreateSound(loc);
                spawnCreationParticles(loc);

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
            // Efectos al iniciar el renombrado
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.5f);
            Location waystoneLoc = waystone.getLocation();
            waystoneLoc.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    waystoneLoc.clone().add(0.5, 2.0, 0.5),
                    10,
                    0.4, 0.4, 0.4,
                    0.1);
            return;
        }

        if (!waystone.isRegistered(playerId)) {
            waystone.registerPlayer(playerId);
            DatabaseHandler.registerPlayerToWaystone(waystone.getId(), playerId);
            TextHandler.get().sendMessage(player, "waystone.registered", waystone.getName());

            // Efectos al registrar
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7f, 1.2f);
            Location waystoneLoc = waystone.getLocation();
            waystoneLoc.getWorld().spawnParticle(
                    Particle.COMPOSTER,
                    waystoneLoc.clone().add(0.5, 1.8, 0.5),
                    15,
                    0.3, 0.3, 0.3,
                    0.1);
        } else {
            // Efecto al interactuar con una waystone ya registrada
            player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 0.8f, 1.5f);
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
                playWaystoneDeactivateSound(baseLoc);
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
                // Sonido de negación
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
                return;
            }
        }

        if (waystone.isAdminCreated() && !(isOp || isCreator)) {
            TextHandler.get().sendMessage(player, "waystone.only_operator_or_creator_can_break");
            event.setCancelled(true);
            // Sonido de negación
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
            return;
        }

        waystones.remove(loc);
        waystone.getHologram().remove();
        removeWaystone(waystone);

        // Efectos de destrucción
        playWaystoneBreakSound(loc);
        loc.getWorld().spawnParticle(
                Particle.LAVA,
                loc.clone().add(0.5, 1.5, 0.5),
                20,
                0.5, 1, 0.5,
                0.2);

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

        if (cancelTeleportOnMove && pendingTeleports.containsKey(playerId)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
                BukkitTask task = pendingTeleports.remove(playerId);
                if (task != null) {
                    task.cancel();
                }
                playerTeleportLocations.remove(playerId);

                // Efectos de teleportación cancelada
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5f, 1.0f);
                player.getWorld().spawnParticle(
                        Particle.SMOKE,
                        player.getLocation().add(0, 1, 0),
                        20,
                        0.3, 0.5, 0.3,
                        0.05);

                TextHandler.get().sendMessage(player, "waystone.teleport_canceled");
            }
            return;
        }

        if (!pendingWaystones.containsKey(playerId)) return;

        PendingWaystone pending = pendingWaystones.get(playerId);
        long timeElapsed = System.currentTimeMillis() - pending.creationTime();
        if (timeElapsed > autoCreateTime) {
            Location loc = pending.location();
            if (player.getLocation().distanceSquared(loc) > autoCreateDistanceSquared) {
                if (loc.getBlock().getType() == Material.LODESTONE && loc.clone().add(0, 1, 0).getBlock().getType() == Material.LODESTONE) {
                    TextDisplay hologram = createHologram(loc, defaultWaystoneName);
                    Waystone waystone = new Waystone(-1, loc, defaultWaystoneName, playerId);
                    waystone.registerPlayer(playerId);
                    waystone.setHologram(hologram);
                    waystones.put(loc, waystone);
                    saveWaystone(waystone);

                    // Efectos de creación automática
                    playWaystoneCreateSound(loc);
                    spawnCreationParticles(loc);

                    TextHandler.get().sendMessage(player, "waystone.created_with_default", defaultWaystoneName);
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
        playerTeleportLocations.remove(playerId);
        if (pendingTeleports.containsKey(playerId)) {
            pendingTeleports.get(playerId).cancel();
            pendingTeleports.remove(playerId);
        }
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
                UUID playerId = player.getUniqueId();

                if (pendingTeleports.containsKey(playerId)) {
                    pendingTeleports.get(playerId).cancel();
                    pendingTeleports.remove(playerId);
                    playerTeleportLocations.remove(playerId);
                }

                // Efectos al iniciar el teleporte
                playTeleportBeginSound(player);
                final Location destination = waystone.getLocation().clone().add(0.5, 1, 0.5);
                playerTeleportLocations.put(playerId, destination);

                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Efectos de teleportación
                    Location originalLoc = player.getLocation();
                    playTeleportSound(player, destination);
                    player.teleport(destination);
                    spawnTeleportCompleteParticles(destination);
                    originalLoc.getWorld().spawnParticle(
                            Particle.REVERSE_PORTAL,
                            originalLoc.clone().add(0, 1, 0),
                            40,
                            0.4, 0.8, 0.4,
                            0.1);

                    player.sendMessage(LegacyTranslationHandler.getPlayerMessage("waystone.teleported", name));
                    pendingTeleports.remove(playerId);
                    playerTeleportLocations.remove(playerId);
                }, teleportDelay * 20L);

                pendingTeleports.put(playerId, task);

                // Genera un "countdown" visual con partículas
                for (int i = 1; i <= teleportDelay; i++) {
                    final int count = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.getWorld().spawnParticle(
                                Particle.ELECTRIC_SPARK,
                                player.getLocation().add(0, 1.8, 0),
                                10,
                                0.3, 0.3, 0.3,
                                0);
                        if (count < teleportDelay) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.5f, 1.0f + (count * 0.1f));
                        }
                    }, i * 20L);
                }

                TextHandler.get().sendMessage(player, "waystone.teleporting_in", String.valueOf(teleportDelay));
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