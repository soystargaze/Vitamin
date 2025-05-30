package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import com.soystargaze.vitamin.utils.text.legacy.LegacyTranslationHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public class WaystoneModule implements Listener {

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<Location, Waystone> waystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingWaystone> pendingWaystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Waystone> renamingWaystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> playerTeleportLocations = new ConcurrentHashMap<>();

    private final boolean onlyCreatorCanBreak;
    private final long autoCreateTime;
    private final double autoCreateDistanceSquared;
    private final String defaultWaystoneName;
    private final int teleportDelay;
    private final boolean cancelTeleportOnMove;
    private final int particleViewDistance;
    private final int holoRefreshRate;
    private final int defaultWaystoneLimit;
    private final boolean enableParticles;
    private final boolean enableSounds;
    private final List<String> restrictedWorlds;

    public WaystoneModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.onlyCreatorCanBreak = plugin.getConfig().getBoolean("waystone.only_creator_can_break", false);
        this.autoCreateTime = plugin.getConfig().getLong("waystone.auto_create_time", 30000L);
        this.autoCreateDistanceSquared = plugin.getConfig().getDouble("waystone.auto_create_distance_squared", 100.0);
        this.defaultWaystoneName = plugin.getConfig().getString("waystone.default_name", "Waystone");
        this.teleportDelay = plugin.getConfig().getInt("waystone.teleport_delay", 3);
        this.cancelTeleportOnMove = plugin.getConfig().getBoolean("waystone.cancel_teleport_on_move", true);
        this.particleViewDistance = plugin.getConfig().getInt("waystone.particle_view_distance", 32);
        this.holoRefreshRate = plugin.getConfig().getInt("waystone.hologram_refresh_rate", 100);
        this.defaultWaystoneLimit = plugin.getConfig().getInt("waystone.default_limit", 5);
        this.enableParticles = plugin.getConfig().getBoolean("waystone.enable_particles", true);
        this.enableSounds = plugin.getConfig().getBoolean("waystone.enable_sounds", true);
        this.restrictedWorlds = plugin.getConfig().getStringList("waystone.restricted_worlds");

        loadWaystones();
        startOptimizedTasks();
    }

    private String processColorCodes(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String processed = input.replace('&', '§');

        if (hasValidMiniMessageTags(processed)) {
            try {
                Component miniMessageComponent = MiniMessage.miniMessage().deserialize(processed);
                LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
                processed = legacy.serialize(miniMessageComponent);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse MiniMessage in waystone name: " + input);
            }
        }

        return processed;
    }

    private String convertToMiniMessageFormat(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String processed = input.replace('&', '§');

        if (hasValidMiniMessageTags(processed)) {
            return processed;
        }

        processed = processed
                .replace("§0", "<black>")
                .replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>")
                .replace("§6", "<gold>")
                .replace("§7", "<gray>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<blue>")
                .replace("§a", "<green>")
                .replace("§b", "<aqua>")
                .replace("§c", "<red>")
                .replace("§d", "<light_purple>")
                .replace("§e", "<yellow>")
                .replace("§f", "<white>")
                .replace("§k", "<obfuscated>")
                .replace("§l", "<bold>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>")
                .replace("§r", "<reset>");

        return processed;
    }

    private boolean hasValidMiniMessageTags(String text) {
        return text.contains("<") && text.contains(">") &&
                (text.contains("color:") || text.contains("gradient:") ||
                        text.contains("rainbow") || text.contains("bold") ||
                        text.contains("italic") || text.contains("underlined") ||
                        text.contains("strikethrough") || text.contains("obfuscated") ||
                        text.contains("red") || text.contains("green") || text.contains("blue") ||
                        text.contains("yellow") || text.contains("purple") || text.contains("aqua") ||
                        text.contains("white") || text.contains("black") || text.contains("gray") ||
                        text.contains("dark_red") || text.contains("dark_green") || text.contains("dark_blue") ||
                        text.contains("dark_aqua") || text.contains("dark_purple") || text.contains("gold") ||
                        text.contains("dark_gray") || text.contains("light_purple"));
    }

    private void startOptimizedTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<Map.Entry<Location, Waystone>> waystoneList = new ArrayList<>(waystones.entrySet());

            for (Map.Entry<Location, Waystone> entry : waystoneList) {
                Location loc = entry.getKey();
                Waystone waystone = entry.getValue();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!isValidWaystoneStructure(loc)) {
                        TextDisplay hologram = waystone.getHologram();
                        if (hologram != null) {
                            hologram.remove();
                            waystone.setHologram(null);
                        }
                        waystones.remove(loc);
                        removeWaystone(waystone);
                    } else {
                        TextDisplay hologram = waystone.getHologram();
                        if (hologram.isDead()) {
                            TextDisplay newHologram = createHologram(loc, waystone.getName());
                            waystone.setHologram(newHologram);
                        }

                        if (enableParticles && Math.random() < 0.05 && hasPlayersNearby(loc)) { // Solo 5% y con jugadores cerca
                            spawnAmbientParticles(loc);
                        }
                    }
                });
            }
        }, holoRefreshRate, holoRefreshRate);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> playerTeleportLocations.forEach((playerId, targetLocation) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && targetLocation != null) {
                if (enableParticles) {
                    spawnTeleportParticles(player.getLocation(), targetLocation);
                }
            }
        }), 2L, 2L);
    }

    private boolean canCreateWaystone(Player player) {
        UUID playerId = player.getUniqueId();

        if (player.hasPermission("vitamin.module.waystone.limit.infinite")) {
            return true;
        }

        int limit = getPlayerWaystoneLimit(player);

        long playerWaystones = waystones.values().stream()
                .filter(waystone -> waystone.getCreator().equals(playerId))
                .count();

        return playerWaystones < limit;
    }

    private int getPlayerWaystoneLimit(Player player) {
        for (int i = 1; i <= 100; i++) {
            if (player.hasPermission("vitamin.module.waystone.limit." + i)) {
                return i;
            }
        }

        return defaultWaystoneLimit;
    }

    private boolean isValidWaystoneStructure(Location loc) {
        return loc.getBlock().getType() == Material.LODESTONE &&
                loc.clone().add(0, 1, 0).getBlock().getType() == Material.LODESTONE;
    }

    private boolean hasPlayersNearby(Location location) {
        double distanceSquared = particleViewDistance * particleViewDistance;
        return Bukkit.getOnlinePlayers().stream()
                .anyMatch(p -> p.getWorld().equals(location.getWorld()) &&
                        p.getLocation().distanceSquared(location) <= distanceSquared);
    }

    private void spawnAmbientParticles(Location location) {
        Location particleLocation = location.clone().add(0.5, 2.0, 0.5);
        location.getWorld().spawnParticle(
                Particle.WITCH,
                particleLocation,
                2,
                0.15, 0.15, 0.15,
                0.01);
    }

    private void spawnTeleportParticles(Location playerLoc, Location targetLoc) {
        if (hasPlayersNearby(playerLoc)) {
            playerLoc.getWorld().spawnParticle(
                    Particle.PORTAL,
                    playerLoc.clone().add(0, 1, 0),
                    15,
                    0.4, 0.8, 0.4,
                    0.08);
        }

        if (hasPlayersNearby(targetLoc)) {
            DustOptions dustOptions = new DustOptions(Color.fromRGB(55, 166, 229), 0.8f);
            targetLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    targetLoc.clone().add(0.5, 1.5, 0.5),
                    8,
                    0.4, 0.8, 0.4,
                    0,
                    dustOptions);
        }
    }

    private void playWaystoneActivateSound(Location location) {
        if (enableSounds) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
            location.getWorld().playSound(location, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.BLOCKS, 0.5f, 1.5f);
        }
    }

    private void playWaystoneDeactivateSound(Location location) {
        if (enableSounds) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
    }

    private void playWaystoneBreakSound(Location location) {
        if (enableSounds) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 0.8f);
            location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0f, 0.5f);
        }
    }

    private void playWaystoneCreateSound(Location location) {
        if (enableSounds) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0f, 1.5f);
            location.getWorld().playSound(location, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    private void spawnCreationParticles(Location location) {
        if (enableParticles) {
            Location particleLocation = location.clone().add(0.5, 1.5, 0.5);
            location.getWorld().spawnParticle(
                    Particle.END_ROD,
                    particleLocation,
                    40,
                    0.5, 0.8, 0.5,
                    0.08);

            location.getWorld().spawnParticle(
                    Particle.TOTEM_OF_UNDYING,
                    particleLocation,
                    25,
                    0.25, 0.4, 0.25,
                    0.25);
        }
    }

    private void playTeleportBeginSound(Player player) {
        if (enableSounds) {
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.5f, 1.5f);
        }
    }

    private void playTeleportSound(Player player, Location destinationLocation) {
        if (enableSounds) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            destinationLocation.getWorld().playSound(destinationLocation, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    private void spawnTeleportCompleteParticles(Location location) {
        if (enableParticles) {
            location.getWorld().spawnParticle(
                    Particle.FLASH,
                    location.clone().add(0, 1, 0),
                    1,
                    0.05, 0.05, 0.05,
                    0);

            location.getWorld().spawnParticle(
                    Particle.PORTAL,
                    location.clone().add(0, 1, 0),
                    35,
                    0.4, 0.8, 0.4,
                    0.4);
        }
    }

    private void loadWaystones() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseHandler.WaystoneData> dataList = DatabaseHandler.loadWaystones();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (DatabaseHandler.WaystoneData data : dataList) {
                    Location loc = data.location();
                    Waystone waystone = new Waystone(data.id(), loc, data.name(), data.creator());
                    waystone.setRegisteredPlayers(DatabaseHandler.getRegisteredPlayers(data.id()));
                    waystones.put(loc, waystone);
                    TextDisplay hologram = createHologram(loc, data.name());
                    waystone.setHologram(hologram);
                }
            });
        });
    }

    private TextDisplay createHologram(Location loc, String name) {
        TextDisplay hologram = (TextDisplay) loc.getWorld().spawnEntity(
                loc.clone().add(0.5, 2.5, 0.5), EntityType.TEXT_DISPLAY);

        String processedName = processColorCodes(name);
        hologram.setText(processedName);

        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setSeeThrough(true);
        hologram.setShadowed(false);
        hologram.setBrightness(new Display.Brightness(15, 15));
        return hologram;
    }

    private void saveWaystone(Waystone waystone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseHandler.WaystoneData data = new DatabaseHandler.WaystoneData(
                    waystone.getId(), waystone.getLocation(), waystone.getName(), waystone.getCreator(), true);
            int id = DatabaseHandler.saveWaystone(data);
            waystone.setId(id);
            for (UUID playerId : waystone.getRegisteredPlayers()) {
                DatabaseHandler.registerPlayerToWaystone(id, playerId);
            }
        });
    }

    private void removeWaystone(Waystone waystone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                DatabaseHandler.removeWaystone(waystone.getId())
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.waystone") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.waystone")) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() != Material.LODESTONE) return;

        if (restrictedWorlds.contains(block.getWorld().getName())) {
            event.setCancelled(true);
            TextHandler.get().sendMessage(player, "waystone.restricted_world");
            return;
        }

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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (renamingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            String newName = event.getMessage().trim();
            Waystone waystone = renamingWaystones.remove(playerId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (waystones.containsKey(waystone.getLocation()) && waystone.getHologram() != null) {
                    String processedName = processColorCodes(newName);
                    waystone.getHologram().setText(processedName);
                    waystone.setName(newName);

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                            DatabaseHandler.updateWaystoneName(waystone.getId(), newName)
                    );

                    if (enableSounds) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.8f);
                    }
                    if (enableParticles) {
                        Location waystoneLoc = waystone.getLocation();
                        waystoneLoc.getWorld().spawnParticle(
                                Particle.ENCHANT,
                                waystoneLoc.clone().add(0.5, 2.5, 0.5),
                                15,
                                0.4, 0.4, 0.4,
                                0.8);
                    }

                    String miniMessageFormattedName = convertToMiniMessageFormat(newName);
                    TextHandler.get().sendMessage(player, "waystone.renamed", miniMessageFormattedName);
                } else {
                    TextHandler.get().sendMessage(player, "waystone.renaming_canceled");
                }
            });
            return;
        }

        if (pendingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            PendingWaystone pending = pendingWaystones.get(playerId);
            Location loc = pending.location();

            if (!isValidWaystoneStructure(loc)) {
                TextHandler.get().sendMessage(player, "waystone.keys_missing");
                pendingWaystones.remove(playerId);
                return;
            }

            if (!canCreateWaystone(player)) {
                int limit = getPlayerWaystoneLimit(player);
                TextHandler.get().sendMessage(player, "waystone.limit_reached", String.valueOf(limit));
                pendingWaystones.remove(playerId);
                playWaystoneDeactivateSound(loc);
                return;
            }

            String name = event.getMessage().trim();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (waystones.containsKey(loc)) {
                    TextHandler.get().sendMessage(player, "waystone.already_exists");
                    playWaystoneDeactivateSound(loc);
                    return;
                }

                if (!canCreateWaystone(player)) {
                    int limit = getPlayerWaystoneLimit(player);
                    TextHandler.get().sendMessage(player, "waystone.limit_reached", String.valueOf(limit));
                    playWaystoneDeactivateSound(loc);
                    return;
                }

                TextDisplay hologram = createHologram(loc, name);
                Waystone waystone = new Waystone(-1, loc, name, playerId);
                waystone.registerPlayer(playerId);
                waystone.setHologram(hologram);
                waystones.put(loc, waystone);
                saveWaystone(waystone);

                playWaystoneCreateSound(loc);
                spawnCreationParticles(loc);

                String miniMessageFormattedName = convertToMiniMessageFormat(name);
                TextHandler.get().sendMessage(player, "waystone.created", miniMessageFormattedName);
            });
            pendingWaystones.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.waystone") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.waystone")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() != Material.AIR) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LODESTONE) return;

        if (restrictedWorlds.contains(block.getWorld().getName())) {
            event.setCancelled(true);
            TextHandler.get().sendMessage(player, "waystone.restricted_world");
            return;
        }

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
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }
            if (enableParticles) {
                Location waystoneLoc = waystone.getLocation();
                waystoneLoc.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        waystoneLoc.clone().add(0.5, 2.0, 0.5),
                        8,
                        0.3, 0.3, 0.3,
                        0.08);
            }
            return;
        }

        if (!waystone.isRegistered(playerId)) {
            waystone.registerPlayer(playerId);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    DatabaseHandler.registerPlayerToWaystone(waystone.getId(), playerId)
            );

            String miniMessageFormattedName = convertToMiniMessageFormat(waystone.getName());
            TextHandler.get().sendMessage(player, "waystone.registered", miniMessageFormattedName);

            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7f, 1.2f);
            }
            if (enableParticles) {
                Location waystoneLoc = waystone.getLocation();
                waystoneLoc.getWorld().spawnParticle(
                        Particle.COMPOSTER,
                        waystoneLoc.clone().add(0.5, 1.8, 0.5),
                        12,
                        0.25, 0.25, 0.25,
                        0.08);
            }
        } else {
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 0.8f, 1.5f);
            }
        }

        openWaystoneInventory(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        if (restrictedWorlds.contains(loc.getWorld().getName())) {
            event.setCancelled(true);
            TextHandler.get().sendMessage(player, "waystone.restricted_world");
            return;
        }

        Iterator<Map.Entry<UUID, PendingWaystone>> pendingIterator = pendingWaystones.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<UUID, PendingWaystone> entry = pendingIterator.next();
            Location baseLoc = entry.getValue().location();
            Location topLoc = baseLoc.clone().add(0, 1, 0);
            if (loc.equals(baseLoc) || loc.equals(topLoc)) {
                UUID playerId = entry.getKey();
                pendingIterator.remove();
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
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
                }
                return;
            }
        }

        if (waystone.isAdminCreated() && !(isOp || isCreator)) {
            TextHandler.get().sendMessage(player, "waystone.only_operator_or_creator_can_break");
            event.setCancelled(true);
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
            }
            return;
        }

        TextDisplay hologram = waystone.getHologram();
        if (hologram != null) {
            hologram.remove();
            waystone.setHologram(null);
        }
        waystones.remove(loc);
        removeWaystone(waystone);

        playWaystoneBreakSound(loc);
        if (enableParticles) {
            loc.getWorld().spawnParticle(
                    Particle.LAVA,
                    loc.clone().add(0.5, 1.5, 0.5),
                    15,
                    0.4, 0.8, 0.4,
                    0.15);
        }

        String miniMessageFormattedName = convertToMiniMessageFormat(waystone.getName());
        TextHandler.get().sendMessage(player, "waystone.destroyed", miniMessageFormattedName);

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
                if (enableParticles) {
                    player.getWorld().spawnParticle(
                            Particle.SMOKE,
                            player.getLocation().add(0, 1, 0),
                            15,
                            0.25, 0.4, 0.25,
                            0.04);
                }

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
                if (isValidWaystoneStructure(loc)) {
                    if (!canCreateWaystone(player)) {
                        TextHandler.get().sendMessage(player, "waystone.auto_creation_canceled_limit");
                        pendingWaystones.remove(playerId);
                        playWaystoneDeactivateSound(loc);
                        return;
                    }

                    TextDisplay hologram = createHologram(loc, defaultWaystoneName);
                    Waystone waystone = new Waystone(-1, loc, defaultWaystoneName, playerId);
                    waystone.registerPlayer(playerId);
                    waystone.setHologram(hologram);
                    waystones.put(loc, waystone);
                    saveWaystone(waystone);

                    playWaystoneCreateSound(loc);
                    spawnCreationParticles(loc);

                    String miniMessageFormattedName = convertToMiniMessageFormat(defaultWaystoneName);
                    TextHandler.get().sendMessage(player, "waystone.created_with_default", miniMessageFormattedName);
                    pendingWaystones.remove(playerId);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component titleComponent = MiniMessage.miniMessage()
                .deserialize(LegacyTranslationHandler.get("waystone.inventory.title"))
                .decoration(TextDecoration.ITALIC, false);

        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
        String legacyTitle = legacy.serialize(titleComponent);

        if (!event.getView().getTitle().equals(legacyTitle)) return;

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;

        String displayName = clickedItem.getItemMeta().getDisplayName();
        String cleanDisplayName = ChatColor.stripColor(displayName).trim();

        for (Waystone waystone : waystones.values()) {
            if (waystone.isRegistered(player.getUniqueId())) {
                String cleanWaystoneName = ChatColor.stripColor(processColorCodes(waystone.getName())).trim();

                if (cleanWaystoneName.equals(cleanDisplayName)) {
                    player.closeInventory();
                    UUID playerId = player.getUniqueId();

                    if (pendingTeleports.containsKey(playerId)) {
                        pendingTeleports.get(playerId).cancel();
                        pendingTeleports.remove(playerId);
                        playerTeleportLocations.remove(playerId);
                    }

                    playTeleportBeginSound(player);
                    final Location destination = waystone.getLocation().clone().add(1.5, 0.2, 1.5);
                    playerTeleportLocations.put(playerId, destination);

                    BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Location originalLoc = player.getLocation();
                        playTeleportSound(player, destination);
                        player.teleport(destination);
                        spawnTeleportCompleteParticles(destination);
                        if (enableParticles) {
                            originalLoc.getWorld().spawnParticle(
                                    Particle.REVERSE_PORTAL,
                                    originalLoc.clone().add(0, 1, 0),
                                    30,
                                    0.3, 0.6, 0.3,
                                    0.08);
                        }

                        String miniMessageFormattedName = convertToMiniMessageFormat(waystone.getName());
                        TextHandler.get().sendMessage(player, "waystone.teleported", miniMessageFormattedName);

                        pendingTeleports.remove(playerId);
                        playerTeleportLocations.remove(playerId);
                    }, teleportDelay * 20L);

                    pendingTeleports.put(playerId, task);

                    for (int i = 1; i <= teleportDelay; i++) {
                        final int count = i;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (enableParticles) {
                                player.getWorld().spawnParticle(
                                        Particle.ELECTRIC_SPARK,
                                        player.getLocation().add(0, 1.8, 0),
                                        8,
                                        0.25, 0.25, 0.25,
                                        0);
                            }
                            if (enableSounds && count < teleportDelay) {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.5f, 1.0f + (count * 0.1f));
                            }
                        }, i * 20L);
                    }

                    TextHandler.get().sendMessage(player, "waystone.teleporting_in", String.valueOf(teleportDelay));
                    break;
                }
            }
        }
    }

    private String processColorCodesAsString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String processed = input.replace('&', '§');

        if (hasValidMiniMessageTags(processed)) {
            try {
                Component miniMessageComponent = MiniMessage.miniMessage().deserialize(processed);
                LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
                processed = legacy.serialize(miniMessageComponent);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse MiniMessage in waystone name: " + input);
            }
        }

        return processed;
    }

    private void openWaystoneInventory(Player player) {
        Component titleComponent = MiniMessage.miniMessage()
                .deserialize(LegacyTranslationHandler.get("waystone.inventory.title"))
                .decoration(TextDecoration.ITALIC, false);

        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
        String legacyTitle = legacy.serialize(titleComponent);

        Inventory inv = Bukkit.createInventory(null, 27, legacyTitle);

        for (Waystone waystone : waystones.values()) {
            if (waystone.isRegistered(player.getUniqueId())) {
                ItemStack item = new ItemStack(Material.LODESTONE);
                ItemMeta meta = item.getItemMeta();

                String processedName = processColorCodesAsString(waystone.getName());
                meta.setDisplayName(processedName);

                List<String> lore = new ArrayList<>();

                Component locationComponent = MiniMessage.miniMessage()
                        .deserialize(LegacyTranslationHandler.getFormatted(
                                "waystone.inventory.item.location",
                                waystone.getLocation().getBlockX(),
                                waystone.getLocation().getBlockY(),
                                waystone.getLocation().getBlockZ()
                        )).decoration(TextDecoration.ITALIC, false);

                Component clickComponent = MiniMessage.miniMessage()
                        .deserialize(LegacyTranslationHandler.get("waystone.inventory.item.click_to_teleport"))
                        .decoration(TextDecoration.ITALIC, false);

                lore.add(legacy.serialize(locationComponent));
                lore.add(legacy.serialize(clickComponent));

                lore.add("§0§k" + waystone.getName());

                meta.setLore(lore);
                item.setItemMeta(meta);
                inv.addItem(item);
            }
        }
        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
    }

    private static class Waystone {
        private volatile int id;
        private final Location location;
        private volatile String name;
        private final UUID creator;
        private final Set<UUID> registeredPlayers = ConcurrentHashMap.newKeySet();
        private volatile TextDisplay hologram;
        private final boolean isAdminCreated;

        public Waystone(int id, Location location, String name, UUID creator) {
            this.id = id;
            this.location = location;
            this.name = name;
            this.creator = creator;
            this.isAdminCreated = Bukkit.getPlayer(creator) != null &&
                    Objects.requireNonNull(Bukkit.getPlayer(creator)).hasPermission("vitamin.module.waystone.admin");
        }

        public void setId(int id) { this.id = id; }
        public int getId() { return id; }
        public void registerPlayer(UUID playerId) { registeredPlayers.add(playerId); }
        public boolean isRegistered(UUID playerId) { return registeredPlayers.contains(playerId); }
        public Location getLocation() { return location; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public UUID getCreator() { return creator; }
        public TextDisplay getHologram() { return hologram; }
        public void setHologram(TextDisplay hologram) { this.hologram = hologram; }
        public Set<UUID> getRegisteredPlayers() { return new HashSet<>(registeredPlayers); }
        public void setRegisteredPlayers(Set<UUID> players) {
            registeredPlayers.clear();
            registeredPlayers.addAll(players);
        }
        public boolean isAdminCreated() { return isAdminCreated; }
    }

    private record PendingWaystone(Location location, long creationTime) {}
}