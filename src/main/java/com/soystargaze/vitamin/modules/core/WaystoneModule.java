package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.integration.GriefPreventionIntegrationHandler;
import com.soystargaze.vitamin.integration.LandsIntegrationHandler;
import com.soystargaze.vitamin.integration.WorldGuardIntegrationHandler;
import com.soystargaze.vitamin.modules.CancellableModule;
import com.soystargaze.vitamin.utils.BlockDisplayUtils;
import com.soystargaze.vitamin.utils.text.TextHandler;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Particle.DustOptions;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class WaystoneModule implements Listener, CancellableModule {

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<Location, Waystone> waystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingWaystone> pendingWaystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Waystone> renamingWaystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> playerTeleportLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Waystone> editingWaystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> addingPlayerToWaystone = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Waystone> changingIconWaystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Cost> pendingCosts = new ConcurrentHashMap<>();

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
    private final boolean enableCreationEffects;
    private final List<String> restrictedWorlds;
    private final int nameCharacterLimit;

    private final boolean costEnabled;
    private final String costType;
    private final int costAmount;
    private final Material costItemType;

    private final String waystoneCoreTexture;
    private final String waystoneCoreName;
    private final List<String> waystoneCoreLore;
    private final String waystoneRecipeShape1;
    private final String waystoneRecipeShape2;
    private final String waystoneRecipeShape3;
    private final Map<Character, Material> waystoneRecipeIngredients;

    private final Material scrollUpMaterial;
    private final String scrollUpName;
    private final List<String> scrollUpLore;
    private final Material scrollDownMaterial;
    private final String scrollDownName;
    private final List<String> scrollDownLore;
    private final Material closeMaterial;
    private final String closeName;
    private final List<String> closeLore;
    private final String iconChangeTitleStr;
    private final int inventorySize;

    private final Material infoItemMaterial;
    private final String noOtherWaystonesName;
    private final List<String> noOtherWaystonesLore;
    private final String noWaystonesName;
    private final List<String> noWaystonesLore;

    // Distance configuration
    private final double minWaystoneDistanceSquared;

    private final Economy economy;

    // Integration handlers
    private final WorldGuardIntegrationHandler worldGuardHandler;
    private final GriefPreventionIntegrationHandler griefPreventionHandler;
    private final LandsIntegrationHandler landsHandler;
    private final boolean hasWorldGuard;
    private final boolean hasGriefPrevention;
    private final boolean hasLands;

    private static final String WAYSTONE_CORE_IDENTIFIER = "vitamin_waystone";

    private final NamespacedKey waystoneCoreKey;
    private final NamespacedKey waystoneIdentifierKey;
    private static WaystoneModule instance;

    private BukkitTask waystoneUpdateTask;
    private BukkitTask teleportParticlesTask;

    private static class Cost {
        String type;
        int amount;
        Material itemType;

        public Cost(String type, int amount, Material itemType) {
            this.type = type;
            this.amount = amount;
            this.itemType = itemType;
        }
    }

    public WaystoneModule(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.waystoneCoreKey = new NamespacedKey(plugin, "vitamin_id");
        this.waystoneIdentifierKey = new NamespacedKey(plugin, "waystone_id");

        // Initialize integration handlers
        this.hasWorldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        this.hasGriefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention") != null;
        this.hasLands = Bukkit.getPluginManager().getPlugin("Lands") != null;

        this.worldGuardHandler = hasWorldGuard ? new WorldGuardIntegrationHandler(plugin) : null;
        this.griefPreventionHandler = hasGriefPrevention ? new GriefPreventionIntegrationHandler(plugin) : null;
        this.landsHandler = hasLands ? new LandsIntegrationHandler(plugin) : null;

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
        this.enableCreationEffects = plugin.getConfig().getBoolean("waystone.enable_creation_effects", true);
        this.restrictedWorlds = plugin.getConfig().getStringList("waystone.restricted_worlds");
        this.nameCharacterLimit = plugin.getConfig().getInt("waystone.name_character_limit", 16);

        // Distance configuration for minimum distance between waystones
        double minDistance = plugin.getConfig().getDouble("waystone.min_distance_between_waystones", 5.0);
        this.minWaystoneDistanceSquared = minDistance * minDistance;

        this.costEnabled = plugin.getConfig().getBoolean("waystone.cost.enabled", false);
        this.costType = plugin.getConfig().getString("waystone.cost.type", "none");
        this.costAmount = plugin.getConfig().getInt("waystone.cost.amount", 1);
        String itemTypeString = plugin.getConfig().getString("waystone.cost.item_type", "ENDER_PEARL");
        Material tempItemType = Material.ENDER_PEARL;
        try {
            tempItemType = Material.valueOf(itemTypeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid cost item type: " + itemTypeString + ", using ENDER_PEARL");
        }
        this.costItemType = tempItemType;

        this.waystoneCoreTexture = plugin.getConfig().getString("waystone.core.texture",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjJkNWQ5ZDQxZDE5MTdmODZkODIyZDEzZDI2YWEwN2VlNmRlZmVjY2RhMWM5ODg1MDVhYjg2NGFmZTk4YTYwNSJ9fX0=");
        this.waystoneCoreName = plugin.getConfig().getString("waystone.core.name", "<gold><bold>Waystone Core");
        this.waystoneCoreLore = plugin.getConfig().getStringList("waystone.core.lore");

        if (waystoneCoreLore.isEmpty()) {
            waystoneCoreLore.add("<gray>A magical core that allows waystone creation.");
            waystoneCoreLore.add("Place it to create a waystone.");
        }

        this.waystoneRecipeShape1 = plugin.getConfig().getString("waystone.core.recipe.shape.1", "EDE");
        this.waystoneRecipeShape2 = plugin.getConfig().getString("waystone.core.recipe.shape.2", "DND");
        this.waystoneRecipeShape3 = plugin.getConfig().getString("waystone.core.recipe.shape.3", "EDE");

        this.waystoneRecipeIngredients = new HashMap<>();
        loadRecipeIngredients();

        String scrollUpMaterialStr = plugin.getConfig().getString("waystone.gui.discovered.navigation.scroll_up.material", "PAPER");
        this.scrollUpMaterial = getMaterialSafely(scrollUpMaterialStr, Material.PAPER);
        this.scrollUpName = plugin.getConfig().getString("waystone.gui.discovered.navigation.scroll_up.name", "Previous Waystones");
        this.scrollUpLore = plugin.getConfig().getStringList("waystone.gui.discovered.navigation.scroll_up.lore");

        String scrollDownMaterialStr = plugin.getConfig().getString("waystone.gui.discovered.navigation.scroll_down.material", "PAPER");
        this.scrollDownMaterial = getMaterialSafely(scrollDownMaterialStr, Material.PAPER);
        this.scrollDownName = plugin.getConfig().getString("waystone.gui.discovered.navigation.scroll_down.name", "Next Waystones");
        this.scrollDownLore = plugin.getConfig().getStringList("waystone.gui.discovered.navigation.scroll_down.lore");

        String closeMaterialStr = plugin.getConfig().getString("waystone.gui.discovered.navigation.close.material", "BARRIER");
        this.closeMaterial = getMaterialSafely(closeMaterialStr, Material.BARRIER);
        this.closeName = plugin.getConfig().getString("waystone.gui.discovered.navigation.close.name", "Close");
        this.closeLore = plugin.getConfig().getStringList("waystone.gui.discovered.navigation.close.lore");

        String iconChangeTitleConfig = plugin.getConfig().getString("waystone.gui.change_icon.title", "Change Waystone Icon");
        this.iconChangeTitleStr = convertToLegacyText(iconChangeTitleConfig);

        this.inventorySize = Math.min(54, Math.max(18, plugin.getConfig().getInt("waystone.gui.discovered.size", 54)));

        String infoMaterialStr = plugin.getConfig().getString("waystone.gui.discovered.info_item.material", "KNOWLEDGE_BOOK");
        this.infoItemMaterial = getMaterialSafely(infoMaterialStr, Material.KNOWLEDGE_BOOK);

        this.noOtherWaystonesName = plugin.getConfig().getString("waystone.gui.discovered.info_item.no_other_waystones.name", "<gold>No Other Waystones Available");
        this.noOtherWaystonesLore = plugin.getConfig().getStringList("waystone.gui.discovered.info_item.no_other_waystones.lore");

        this.noWaystonesName = plugin.getConfig().getString("waystone.gui.discovered.info_item.no_waystones.name", "<gold>No Waystones Available");
        this.noWaystonesLore = plugin.getConfig().getStringList("waystone.gui.discovered.info_item.no_waystones.lore");

        if (noOtherWaystonesLore.isEmpty()) {
            noOtherWaystonesLore.add("<gray>You are currently at this waystone,");
            noOtherWaystonesLore.add("<gray>so it's not shown in the list.");
            noOtherWaystonesLore.add("<yellow>Discover more waystones to travel!");
        }

        if (noWaystonesLore.isEmpty()) {
            noWaystonesLore.add("<gray>You haven't discovered any waystones yet.");
            noWaystonesLore.add("<yellow>Explore the world to find them!");
        }

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            this.economy = Bukkit.getServer().getServicesManager().getRegistration(Economy.class) != null ?
                    Objects.requireNonNull(Bukkit.getServer().getServicesManager().getRegistration(Economy.class)).getProvider() : null;
            if (this.economy == null) {
                TextHandler.get().logTranslated("waystone.vault_not_found");
            }
        } else {
            this.economy = null;
            TextHandler.get().logTranslated("waystone.vault_not_installed");
        }

        registerWaystoneCoreRecipe();
        loadWaystones();
        startOptimizedTasks();
    }

    private boolean isAdmin(Player player) {
        return player.hasPermission("vitamin.module.waystone.admin");
    }

    // Integration permission checks
    private boolean canCreateWaystoneWithIntegrations(Player player, Location location) {
        if (hasWorldGuard && worldGuardHandler != null) {
            if (!worldGuardHandler.canCreateWaystone(player, location)) {
                return false;
            }
        }

        if (hasGriefPrevention && griefPreventionHandler != null) {
            if (!griefPreventionHandler.canCreateWaystone(player, location, null)) {
                return false;
            }
        }

        if (hasLands && landsHandler != null) {
            if (!landsHandler.canCreateWaystone(player, location)) {
                return false;
            }
        }

        return true;
    }

    private boolean canUseWaystoneWithIntegrations(Player player, Location location) {
        if (hasWorldGuard && worldGuardHandler != null) {
            if (!worldGuardHandler.canUseWaystone(player, location)) {
                return false;
            }
        }

        if (hasGriefPrevention && griefPreventionHandler != null) {
            if (!griefPreventionHandler.canUseWaystone(player, location, null)) {
                return false;
            }
        }

        if (hasLands && landsHandler != null) {
            if (!landsHandler.canUseWaystone(player, location)) {
                return false;
            }
        }

        return true;
    }

    private boolean canBreakWaystoneWithIntegrations(Player player, Location location) {
        if (hasWorldGuard && worldGuardHandler != null) {
            if (!worldGuardHandler.canBreakWaystone(player, location)) {
                return false;
            }
        }

        if (hasGriefPrevention && griefPreventionHandler != null) {
            if (!griefPreventionHandler.canBreakWaystone(player, location, null)) {
                return false;
            }
        }

        if (hasLands && landsHandler != null) {
            if (!landsHandler.canBreakWaystone(player, location)) {
                return false;
            }
        }

        return true;
    }

    // Check if location is too close to existing waystones
    private boolean isTooCloseToExistingWaystone(Location location) {
        return waystones.keySet().stream()
                .anyMatch(existingLocation -> existingLocation.getWorld().equals(location.getWorld()) &&
                        existingLocation.distanceSquared(location) < minWaystoneDistanceSquared);
    }

    public void clearWaystones() {
        for (Waystone waystone : waystones.values()) {
            removeWaystoneEntities(waystone);
            removeWaystoneBarrierPillar(waystone.getLocation());
        }
        waystones.clear();
        DatabaseHandler.clearWaystones();
    }

    private void handleEditGUIClick(int slot, Player player, Waystone waystone) {
        switch (slot) {
            case 9:
                if (waystone.isGlobal()) {
                    waystone.setGlobal(false);
                    waystone.setDiscoverable(false);
                } else if (waystone.isDiscoverable()) {
                    waystone.setGlobal(true);
                    waystone.setDiscoverable(false);
                } else {
                    waystone.setDiscoverable(true);
                }
                saveWaystoneSettings(waystone);
                openWaystoneEditGUI(player, waystone);
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, 1.5f);
                }
                break;

            case 11:
                player.closeInventory();
                editingWaystones.remove(player.getUniqueId());
                renamingWaystones.put(player.getUniqueId(), waystone);
                TextHandler.get().sendMessage(player, "waystone.rename_prompt");
                break;

            case 13:
                openIconChangeGUI(player, waystone);
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.8f);
                }
                break;

            case 15:
                if (!waystone.isDiscoverable() && !waystone.isGlobal()) {
                    openPlayerManagementGUI(player, waystone);
                }
                break;

            case 17:
                player.closeInventory();
                editingWaystones.remove(player.getUniqueId());

                Location waystoneLoc = waystone.getLocation();
                removeWaystoneEntities(waystone);
                removeWaystoneBarrierPillar(waystoneLoc);
                waystones.remove(waystoneLoc);
                removeWaystone(waystone);

                ItemStack waystoneCoreItem = createWaystoneCore();
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(waystoneCoreItem);
                } else {
                    waystoneLoc.getWorld().dropItemNaturally(waystoneLoc, waystoneCoreItem);
                }

                playWaystoneBreakSound(waystoneLoc);

                String miniMessageFormattedName = convertToMiniMessageFormat(waystone.getName());
                TextHandler.get().sendMessage(player, "waystone.destroyed", miniMessageFormattedName);
                break;

            case 4:
                waystone.setNameVisible(!waystone.isNameVisible());
                saveWaystoneSettings(waystone);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                        DatabaseHandler.updateWaystoneNameVisibility(waystone.getId(), waystone.isNameVisible())
                );

                openWaystoneEditGUI(player, waystone);
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, 1.5f);
                }

                String visibilityMessage = waystone.isNameVisible() ? "waystone.name_shown" : "waystone.name_hidden";
                TextHandler.get().sendMessage(player, visibilityMessage);
                break;

            case 22:
                player.closeInventory();
                editingWaystones.remove(player.getUniqueId());
                break;
        }
    }

    private void handlePlayerManagementClick(Player player, Waystone waystone, ItemStack clicked, int slot) {
        if (slot == 45) {
            player.closeInventory();
            TextHandler.get().sendMessage(player, "waystone.add_player_prompt");
            addingPlayerToWaystone.put(player.getUniqueId(), "adding");
            return;
        }

        if (slot == 53) {
            openWaystoneEditGUI(player, waystone);
            return;
        }

        if (clicked != null && clicked.getType() == Material.PLAYER_HEAD && slot < 45) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.getLore() != null && !meta.getLore().isEmpty()) {
                List<String> lore = meta.getLore();
                for (String loreLine : lore) {
                    if (loreLine.startsWith("UUID: ")) {
                        String uuidPart = loreLine.substring(6);
                        try {
                            UUID playerToRemove = UUID.fromString(uuidPart);
                            waystone.removeAllowedPlayer(playerToRemove);
                            saveWaystoneSettings(waystone);

                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                                    DatabaseHandler.removeWaystonePermission(waystone.getId(), playerToRemove)
                            );

                            openPlayerManagementGUI(player, waystone);
                            TextHandler.get().sendMessage(player, "waystone.player_removed", playerToRemove.toString());
                            return;
                        } catch (IllegalArgumentException e) {
                            TextHandler.get().sendMessage(player, "waystone.error_removing_player");
                        }
                    }
                }
            }
        }
    }

    private void handleWaystoneInventoryClick(Player player, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        UUID playerId = player.getUniqueId();

        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(waystoneIdentifierKey, PersistentDataType.STRING)) return;

        String waystoneIdStr = container.get(waystoneIdentifierKey, PersistentDataType.STRING);
        if (waystoneIdStr == null) return;

        int waystoneId = Integer.parseInt(waystoneIdStr);

        Waystone targetWaystone = waystones.values().stream()
                .filter(waystone -> waystone.getId() == waystoneId)
                .findFirst().orElse(null);

        if (targetWaystone == null) return;

        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            player.closeInventory();

            if (!canPlayerAffordWaystone(player)) {
                TextHandler.get().sendMessage(player, "waystone.not_enough_resources", getCostMessage());
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
                }
                return;
            }

            chargePlayerForWaystone(player);
            if (costEnabled && !costType.equals("none")) {
                pendingCosts.put(playerId, new Cost(costType, costAmount, costItemType));
            }

            if (pendingTeleports.containsKey(playerId)) {
                pendingTeleports.get(playerId).cancel();
                pendingTeleports.remove(playerId);
                playerTeleportLocations.remove(playerId);
                pendingCosts.remove(playerId);
            }

            playTeleportBeginSound(player);
            final Location destination = targetWaystone.getLocation().clone().add(1.5, 0.2, 1.5);
            destination.setYaw(player.getLocation().getYaw());
            destination.setPitch(0.0f);
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

                String miniMessageFormattedName = convertToMiniMessageFormat(targetWaystone.getName());
                TextHandler.get().sendMessage(player, "waystone.teleported", miniMessageFormattedName);

                pendingTeleports.remove(playerId);
                playerTeleportLocations.remove(playerId);
                pendingCosts.remove(playerId);
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
        }
    }

    private void handleIconConfirm(Player player, Waystone waystone, ItemStack iconItem) {
        UUID playerId = player.getUniqueId();

        if (iconItem != null && iconItem.getType() != Material.AIR) {
            ItemStack iconItemCopy = iconItem.clone();
            iconItemCopy.setAmount(1);

            String iconData = serializeItemStack(iconItemCopy);
            waystone.setIconData(iconData);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    DatabaseHandler.updateWaystoneIcon(waystone.getId(), iconData)
            );

            TextHandler.get().sendMessage(player, "waystone.gui.icon_changed");

            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }
        } else {
            waystone.setIconData(null);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    DatabaseHandler.updateWaystoneIcon(waystone.getId(), null)
            );

            TextHandler.get().sendMessage(player, "waystone.gui.icon_reset");

            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }

        player.closeInventory();
        changingIconWaystones.remove(playerId);

        Bukkit.getScheduler().runTaskLater(plugin, () -> openWaystoneEditGUI(player, waystone), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        if (changingIconWaystones.containsKey(player.getUniqueId())) {
            String inventoryTitle = event.getView().getTitle();
            String expectedTitle = LegacyComponentSerializer.legacySection().serialize(
                    processColorCodes(plugin.getConfig().getString("waystone.gui.change_icon.title", "Change Icon"))
            );

            if (inventoryTitle.equals(expectedTitle)) {
                ItemStack iconItem = event.getInventory().getItem(11);
                if (iconItem != null && iconItem.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(iconItem);
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }

                changingIconWaystones.remove(player.getUniqueId());
            }
        }
    }

    private String convertToLegacyText(String text) {
        try {
            Component component = processColorCodes(text);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } catch (Exception e) {
            return text.replace("&", "§");
        }
    }

    private Material getMaterialSafely(String materialName, Material fallback) {
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialName + ", using " + fallback.name());
            return fallback;
        }
    }

    private String serializeItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Error serializing item: " + e.getMessage());
            return null;
        }
    }

    private ItemStack deserializeItemStack(String data) {
        if (data == null || data.trim().isEmpty()) return null;

        try {
            byte[] serializedData = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Error deserializing item: " + e.getMessage());
            return null;
        }
    }

    private void openIconChangeGUI(Player player, Waystone waystone) {
        changingIconWaystones.put(player.getUniqueId(), waystone);

        Component title = processColorCodes(plugin.getConfig().getString("waystone.gui.change_icon.title", "Change Icon"));

        Gui gui = Gui.gui()
                .title(title)
                .rows(3)
                .create();

        gui.disableAllInteractions();
        gui.enableItemPlace();

        GuiItem glassPane = createGlassPane(Material.WHITE_STAINED_GLASS_PANE);
        gui.getFiller().fill(glassPane);

        gui.setItem(11, new GuiItem(new ItemStack(Material.AIR)));

        // Confirm button
        ItemStack confirmButton = new ItemStack(Material.LIME_DYE);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            Component confirmNameComponent = processColorCodes(plugin.getConfig().getString("waystone.gui.change_icon.confirm.name", "Confirm"))
                    .decoration(TextDecoration.ITALIC, false);
            String confirmName = LegacyComponentSerializer.legacySection().serialize(confirmNameComponent);
            confirmMeta.setDisplayName(confirmName);

            List<String> confirmLore = new ArrayList<>();
            List<String> confirmLoreConfig = plugin.getConfig().getStringList("waystone.gui.change_icon.confirm.lore");
            for (String loreLine : confirmLoreConfig) {
                Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                confirmLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
            }
            if (confirmLore.isEmpty()) {
                Component defaultLore = processColorCodes("Click to use this item as icon").decoration(TextDecoration.ITALIC, false);
                confirmLore.add(LegacyComponentSerializer.legacySection().serialize(defaultLore));
            }
            confirmMeta.setLore(confirmLore);
            confirmButton.setItemMeta(confirmMeta);
        }

        gui.setItem(13, new GuiItem(confirmButton, (event) -> {
            ItemStack iconItem = gui.getInventory().getItem(11);
            if (iconItem != null && iconItem.getType() != Material.AIR) {
                ItemStack singleIconItem = iconItem.clone();
                singleIconItem.setAmount(1);

                if (iconItem.getAmount() > 1) {
                    iconItem.setAmount(iconItem.getAmount() - 1);
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(iconItem);
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }

                handleIconConfirm(player, waystone, singleIconItem);
            } else {
                handleIconConfirm(player, waystone, null);
            }

            player.closeInventory();
            changingIconWaystones.remove(player.getUniqueId());
        }));

        // Cancel button
        ItemStack cancelButton = new ItemStack(Material.RED_DYE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            Component cancelNameComponent = processColorCodes(plugin.getConfig().getString("waystone.gui.change_icon.cancel.name", "Cancel"))
                    .decoration(TextDecoration.ITALIC, false);
            String cancelName = LegacyComponentSerializer.legacySection().serialize(cancelNameComponent);
            cancelMeta.setDisplayName(cancelName);

            List<String> cancelLore = new ArrayList<>();
            List<String> cancelLoreConfig = plugin.getConfig().getStringList("waystone.gui.change_icon.cancel.lore");
            for (String loreLine : cancelLoreConfig) {
                Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                cancelLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
            }
            if (cancelLore.isEmpty()) {
                Component defaultLore = processColorCodes("<gray>Click to cancel").decoration(TextDecoration.ITALIC, false);
                cancelLore.add(LegacyComponentSerializer.legacySection().serialize(defaultLore));
            }
            cancelMeta.setLore(cancelLore);
            cancelButton.setItemMeta(cancelMeta);
        }

        gui.setItem(15, new GuiItem(cancelButton, (event) -> {
            ItemStack iconItem = gui.getInventory().getItem(11);
            if (iconItem != null && iconItem.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(iconItem);
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }

            player.closeInventory();
            changingIconWaystones.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> openWaystoneEditGUI(player, waystone), 1L);
        }));

        gui.open(player);
        TextHandler.get().sendMessage(player, "waystone.gui.change_icon.instructions");
    }

    private ItemStack createWaystoneItem(Waystone waystone) {
        Material iconMaterial = Material.LODESTONE;
        ItemStack item;

        if (waystone.getIconData() != null) {
            ItemStack customIcon = deserializeItemStack(waystone.getIconData());
            if (customIcon != null) {
                item = new ItemStack(customIcon.getType(), 1);
                if (customIcon.hasItemMeta()) {
                    ItemMeta originalMeta = customIcon.getItemMeta();
                    ItemMeta newMeta = item.getItemMeta();

                    if (originalMeta instanceof SkullMeta originalSkullMeta && newMeta instanceof SkullMeta newSkullMeta) {
                        if (originalSkullMeta.hasOwner()) {
                            newSkullMeta.setOwningPlayer(originalSkullMeta.getOwningPlayer());
                        }
                        if (originalSkullMeta.getOwnerProfile() != null) {
                            newSkullMeta.setOwnerProfile(originalSkullMeta.getOwnerProfile());
                        }
                    }
                    item.setItemMeta(newMeta);
                }
            } else {
                item = new ItemStack(iconMaterial);
            }
        } else {
            item = new ItemStack(iconMaterial);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Component nameComponent = processColorCodes(waystone.getName())
                    .decoration(TextDecoration.ITALIC, false);
            String nameText = LegacyComponentSerializer.legacySection().serialize(nameComponent);
            meta.setDisplayName(nameText);

            List<String> lore = new ArrayList<>();

            String locationFormat = plugin.getConfig().getString("waystone.gui.discovered.item.location", "X: %d, Y: %d, Z: %d");
            String locationString = String.format(locationFormat,
                    waystone.getLocation().getBlockX(),
                    waystone.getLocation().getBlockY(),
                    waystone.getLocation().getBlockZ());
            Component locationComponent = processColorCodes(locationString).decoration(TextDecoration.ITALIC, false);
            String locationText = LegacyComponentSerializer.legacySection().serialize(locationComponent);
            lore.add(locationText);

            String clickString = plugin.getConfig().getString("waystone.gui.discovered.item.click_to_teleport", "Click to teleport");
            Component clickComponent = processColorCodes(clickString).decoration(TextDecoration.ITALIC, false);
            String clickText = LegacyComponentSerializer.legacySection().serialize(clickComponent);
            lore.add(clickText);

            if (costEnabled && !costType.equals("none")) {
                String costFormat = plugin.getConfig().getString("waystone.gui.discovered.item.cost", "Cost: %s");
                String costMessage = getCostMessage();
                String costString = String.format(costFormat, costMessage);
                Component costComponent = processColorCodes(costString).decoration(TextDecoration.ITALIC, false);
                String costText = LegacyComponentSerializer.legacySection().serialize(costComponent);
                lore.add(costText);
            }

            if (waystone.isGlobal()) {
                String globalString = plugin.getConfig().getString("waystone.gui.discovered.item.global", "Global Waystone");
                Component globalComponent = processColorCodes(globalString).decoration(TextDecoration.ITALIC, false);
                String globalText = LegacyComponentSerializer.legacySection().serialize(globalComponent);
                lore.add(globalText);
            } else if (waystone.isDiscoverable()) {
                String publicString = plugin.getConfig().getString("waystone.gui.discovered.item.discoverable", "Discoverable Waystone");
                Component publicComponent = processColorCodes(publicString).decoration(TextDecoration.ITALIC, false);
                String publicText = LegacyComponentSerializer.legacySection().serialize(publicComponent);
                lore.add(publicText);
            } else {
                String privateString = plugin.getConfig().getString("waystone.gui.discovered.item.private", "Private Waystone");
                Component privateComponent = processColorCodes(privateString).decoration(TextDecoration.ITALIC, false);
                String privateText = LegacyComponentSerializer.legacySection().serialize(privateComponent);
                lore.add(privateText);
            }

            meta.setLore(lore);

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(waystoneIdentifierKey, PersistentDataType.STRING, String.valueOf(waystone.getId()));

            item.setItemMeta(meta);
        }

        return item;
    }

    private void loadRecipeIngredients() {
        if (plugin.getConfig().isConfigurationSection("waystone.core.recipe.ingredients")) {
            var section = plugin.getConfig().getConfigurationSection("waystone.core.recipe.ingredients");
            assert section != null;
            for (String key : section.getKeys(false)) {
                String materialName = section.getString(key);
                try {
                    assert materialName != null;
                    Material material = Material.valueOf(materialName.toUpperCase());
                    waystoneRecipeIngredients.put(key.charAt(0), material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in waystone recipe: " + materialName +
                            ". Using default ingredients instead.");
                }
            }
        }

        if (waystoneRecipeIngredients.isEmpty()) {
            waystoneRecipeIngredients.put('E', Material.ENDER_PEARL);
            waystoneRecipeIngredients.put('D', Material.DIAMOND);
            waystoneRecipeIngredients.put('N', Material.NETHER_STAR);
        }
    }

    private boolean canPlayerAffordWaystone(Player player) {
        if (!costEnabled || costType.equals("none")) {
            return true;
        }

        return switch (costType.toLowerCase()) {
            case "exp_levels" -> player.getLevel() >= costAmount;
            case "exp_points" -> player.getTotalExperience() >= costAmount;
            case "items" -> player.getInventory().containsAtLeast(new ItemStack(costItemType), costAmount);
            case "vault" -> economy != null && economy.has(player, costAmount);
            default -> true;
        };
    }

    private void chargePlayerForWaystone(Player player) {
        if (!costEnabled || costType.equals("none")) {
            return;
        }

        switch (costType.toLowerCase()) {
            case "exp_levels":
                player.setLevel(player.getLevel() - costAmount);
                break;
            case "exp_points":
                player.setTotalExperience(player.getTotalExperience() - costAmount);
                break;
            case "items":
                ItemStack costItem = new ItemStack(costItemType, costAmount);
                player.getInventory().removeItem(costItem);
                break;
            case "vault":
                if (economy != null) {
                    economy.withdrawPlayer(player, costAmount);
                }
                break;
        }
    }

    private void refundCost(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        Cost cost = pendingCosts.remove(playerId);
        if (cost == null) return;

        switch (cost.type.toLowerCase()) {
            case "exp_levels":
                player.setLevel(player.getLevel() + cost.amount);
                break;
            case "exp_points":
                player.setTotalExperience(player.getTotalExperience() + cost.amount);
                break;
            case "items":
                ItemStack refundItem = new ItemStack(cost.itemType, cost.amount);
                player.getInventory().addItem(refundItem);
                break;
            case "vault":
                if (economy != null) {
                    economy.depositPlayer(player, cost.amount);
                }
                break;
        }

        TextHandler.get().sendMessage(player, "waystone.refunded_resources", getCostMessage());
    }

    private String getCostMessage() {
        if (!costEnabled || costType.equals("none")) {
            return plugin.getConfig().getString("waystone.cost.messages.free", "Free");
        }

        return switch (costType.toLowerCase()) {
            case "exp_levels" -> {
                String format = plugin.getConfig().getString("waystone.cost.messages.exp_levels", "%d EXP Levels");
                yield String.format(format, costAmount);
            }
            case "exp_points" -> {
                String format = plugin.getConfig().getString("waystone.cost.messages.exp_points", "%d EXP Points");
                yield String.format(format, costAmount);
            }
            case "items" -> {
                String format = plugin.getConfig().getString("waystone.cost.messages.items", "%d %s");
                yield String.format(format, costAmount, costItemType.name());
            }
            case "vault" -> {
                String format = plugin.getConfig().getString("waystone.cost.messages.vault", "%d Money");
                yield String.format(format, costAmount);
            }
            default -> "Free";
        };
    }

    private ItemStack createWaystoneCore() {
        ItemStack head = getSkull(waystoneCoreTexture);
        ItemMeta meta = head.getItemMeta();

        if (meta != null) {
            Component displayNameComponent = MiniMessage.miniMessage()
                    .deserialize(waystoneCoreName)
                    .decoration(TextDecoration.ITALIC, false);
            String displayName = LegacyComponentSerializer.legacySection().serialize(displayNameComponent);

            List<String> loreComponents = waystoneCoreLore.stream()
                    .map(line -> {
                        Component lineComponent = MiniMessage.miniMessage()
                                .deserialize(line)
                                .decoration(TextDecoration.ITALIC, false);
                        return LegacyComponentSerializer.legacySection().serialize(lineComponent);
                    })
                    .toList();

            meta.setDisplayName(displayName);
            meta.setLore(loreComponents);

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(waystoneCoreKey, PersistentDataType.STRING, WAYSTONE_CORE_IDENTIFIER);

            head.setItemMeta(meta);
        }

        return head;
    }

    private ItemStack getSkull(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            try {
                String json = new String(
                        Base64.getDecoder().decode(texture),
                        StandardCharsets.UTF_8
                );
                int i = json.indexOf("\"url\":\"") + 7;
                int j = json.indexOf('"', i);
                String skinUrl = json.substring(i, j);

                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "Waystone");
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URI(skinUrl).toURL());
                profile.setTextures(textures);

                meta.setOwnerProfile(profile);
                head.setItemMeta(meta);
            } catch (Exception e) {
                plugin.getLogger().warning("Error applying texture to waystone core: " + e.getMessage());
            }
        }

        return head;
    }

    private boolean isWaystoneCore(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(waystoneCoreKey, PersistentDataType.STRING) &&
                WAYSTONE_CORE_IDENTIFIER.equals(container.get(waystoneCoreKey, PersistentDataType.STRING));
    }

    private void createWaystoneBarrierPillar(Location location) {
        Location barrier1 = location.clone().add(0, 0, 0);
        Location barrier2 = location.clone().add(0, 1, 0);

        barrier1.getBlock().setType(Material.BARRIER);
        barrier2.getBlock().setType(Material.BARRIER);
    }

    private void removeWaystoneBarrierPillar(Location location) {
        Location barrier1 = location.clone().add(0, 0, 0);
        Location barrier2 = location.clone().add(0, 1, 0);

        if (barrier1.getBlock().getType() == Material.BARRIER) {
            barrier1.getBlock().setType(Material.AIR);
        }
        if (barrier2.getBlock().getType() == Material.BARRIER) {
            barrier2.getBlock().setType(Material.AIR);
        }
    }

    private boolean hasValidBarrierPillar(Location location) {
        Location barrier1 = location.clone().add(0, 0, 0);
        Location barrier2 = location.clone().add(0, 1, 0);

        return barrier1.getBlock().getType() == Material.BARRIER &&
                barrier2.getBlock().getType() == Material.BARRIER;
    }

    private void placePendingWaystoneCore(Location location) {
        Block block = location.getBlock();
        block.setType(Material.PLAYER_HEAD);

        BlockState state = block.getState();
        if (state instanceof Skull skull) {
            try {
                String json = new String(
                        Base64.getDecoder().decode(waystoneCoreTexture),
                        StandardCharsets.UTF_8
                );
                int i = json.indexOf("\"url\":\"") + 7;
                int j = json.indexOf('"', i);
                String skinUrl = json.substring(i, j);

                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "Waystone");
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URI(skinUrl).toURL());
                profile.setTextures(textures);

                skull.setOwnerProfile(profile);
                skull.update();
            } catch (Exception e) {
                plugin.getLogger().warning("Error applying texture to pending waystone core: " + e.getMessage());
            }
        }
    }

    private void removePendingWaystoneCore(Location location) {
        Block block = location.getBlock();
        if (block.getType() == Material.PLAYER_HEAD) {
            block.setType(Material.AIR);
        }
    }

    private void dropWaystoneCoreFromPending(Location location) {
        removePendingWaystoneCore(location);
        ItemStack waystoneCoreItem = createWaystoneCore();
        location.getWorld().dropItemNaturally(location, waystoneCoreItem);
    }

    private boolean isPendingWaystoneCore(Location location) {
        for (PendingWaystone pending : pendingWaystones.values()) {
            if (pending.location().equals(location)) {
                return true;
            }
        }
        return false;
    }

    private void registerWaystoneCoreRecipe() {
        ItemStack result = createWaystoneCore();

        NamespacedKey recipeKey = new NamespacedKey(plugin, "waystone_core_recipe");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        recipe.shape(
                waystoneRecipeShape1,
                waystoneRecipeShape2,
                waystoneRecipeShape3
        );

        for (Map.Entry<Character, Material> entry : waystoneRecipeIngredients.entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }

        try {
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Error registering waystone core recipe: " + e.getMessage());
        }
    }

    private Component processColorCodes(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Component.text(input == null ? "" : input);
        }

        if (hasValidMiniMessageTags(input)) {
            try {
                return MiniMessage.miniMessage().deserialize(input);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse MiniMessage in waystone name, falling back to legacy codes: " + input);
            }
        }

        String processed = input.replace('&', '§');
        return LegacyComponentSerializer.legacySection().deserialize(processed);
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
        waystoneUpdateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<Map.Entry<Location, Waystone>> waystoneList = new ArrayList<>(waystones.entrySet());
            for (Map.Entry<Location, Waystone> entry : waystoneList) {
                Location loc = entry.getKey();
                Waystone waystone = entry.getValue();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!isValidWaystoneStructure(loc, waystone)) {
                        if (!hasValidBarrierPillar(loc)) {
                            createWaystoneBarrierPillar(loc);
                        }
                        if (waystone.getHologram() == null || waystone.getHologram().isDead()) {
                            TextDisplay newHologram = createHologram(loc, waystone.getName());
                            waystone.setHologram(newHologram);
                            waystone.setNameVisible(waystone.isNameVisible());
                        }
                        if (waystone.getBlockDisplays().isEmpty() ||
                                waystone.getBlockDisplays().stream().anyMatch(bd -> bd == null || bd.isDead())) {
                            waystone.getBlockDisplays().forEach(bd -> {
                                if (bd != null && !bd.isDead()) bd.remove();
                            });
                            waystone.getBlockDisplays().clear();
                            createWaystoneBlockDisplays(loc, waystone);
                        }

                        if (!isValidWaystoneStructure(loc, waystone)) {
                            removeWaystoneEntities(waystone);
                            removeWaystoneBarrierPillar(loc);
                            waystones.remove(loc);
                            removeWaystone(waystone);
                        }
                    } else {
                        if (enableParticles && Math.random() < 0.05 && hasPlayersNearby(loc)) {
                            spawnAmbientParticles(loc);
                        }
                    }
                });
            }
        }, 0L, holoRefreshRate);

        teleportParticlesTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> playerTeleportLocations.forEach((playerId, targetLocation) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && targetLocation != null) {
                if (enableParticles) {
                    spawnTeleportParticles(player.getLocation(), targetLocation);
                }
            }
        }), 2L, 2L);
    }

    private void removeWaystoneEntities(Waystone waystone) {
        TextDisplay hologram = waystone.getHologram();
        if (hologram != null) {
            hologram.remove();
            waystone.setHologram(null);
        }

        waystone.getBlockDisplays().forEach(bd -> {
            if (bd != null && !bd.isDead()) {
                bd.remove();
            }
        });
        waystone.getBlockDisplays().clear();
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

    private boolean isValidWaystoneStructure(Location loc, Waystone waystone) {
        return hasValidBarrierPillar(loc) &&
                waystone.getHologram() != null && !waystone.getHologram().isDead() &&
                !waystone.getBlockDisplays().isEmpty() &&
                waystone.getBlockDisplays().stream().allMatch(bd -> bd != null && !bd.isDead());
    }

    private boolean hasPlayersNearby(Location location) {
        return !location.getWorld().getNearbyEntities(location, particleViewDistance, particleViewDistance, particleViewDistance)
                .stream()
                .filter(entity -> entity instanceof Player)
                .toList()
                .isEmpty();
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
        if (enableSounds && enableCreationEffects) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
            location.getWorld().playSound(location, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.BLOCKS, 0.5f, 1.5f);
        }
    }

    private void playWaystoneDeactivateSound(Location location) {
        if (enableSounds && enableCreationEffects) {
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
        if (enableSounds && enableCreationEffects) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0f, 1.5f);
            location.getWorld().playSound(location, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    private void spawnCreationParticles(Location location) {
        if (enableParticles && enableCreationEffects) {
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

    private void cleanupExistingWaystoneEntities(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        String locationTag = "waystone_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        String holoTag = "waystone_holo_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

        Chunk chunk = location.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof TextDisplay || entity instanceof BlockDisplay) {
                Set<String> tags = entity.getScoreboardTags();

                if (tags.contains(locationTag) || tags.contains(holoTag) ||
                        (tags.contains("vitaminwaystone") && isCloseToWaystoneLocation(entity.getLocation(), location))) {
                    entity.remove();
                }
            }
        }
    }

    private boolean isCloseToWaystoneLocation(Location entityLoc, Location waystoneLoc) {
        return entityLoc.getWorld().equals(waystoneLoc.getWorld()) &&
                entityLoc.distanceSquared(waystoneLoc) < 4.0;
    }

    private void loadWaystones() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseHandler.WaystoneData> dataList = DatabaseHandler.loadWaystones();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (DatabaseHandler.WaystoneData data : dataList) {
                    Location loc = data.location();
                    Waystone waystone = new Waystone(data.id(), loc, data.name(), data.creator(), data.baseMaterial());
                    waystone.setDiscoverable(data.isPublic());
                    waystone.setGlobal(data.isGlobal());
                    waystone.setIconData(data.iconData());
                    waystone.setRegisteredPlayers(DatabaseHandler.getRegisteredPlayers(data.id()));
                    waystone.setAllowedPlayers(DatabaseHandler.getWaystonePermissions(data.id()));
                    waystones.put(loc, waystone);

                    createWaystoneBarrierPillar(loc);
                    cleanupExistingWaystoneEntities(loc);
                    createWaystoneBlockDisplays(loc, waystone);
                    TextDisplay hologram = createHologram(loc, data.name());
                    waystone.setHologram(hologram);

                    waystone.setNameVisible(data.nameVisible());
                }
                TextHandler.get().logTranslated("waystone.loaded_waystones", waystones.size());
            });
        });
    }

    private void createWaystoneBlockDisplays(Location loc, Waystone waystone) {
        List<BlockDisplay> displays = BlockDisplayUtils.createWaystoneBlockDisplays(loc, waystone.getBaseMaterial());
        waystone.setBlockDisplays(displays);
    }

    private TextDisplay createHologram(Location loc, String name) {
        TextDisplay hologram = (TextDisplay) loc.getWorld().spawnEntity(
                loc.clone().add(0.5, 2.2, 0.5), EntityType.TEXT_DISPLAY);

        Component nameComponent = processColorCodes(name);
        String nameText = LegacyComponentSerializer.legacySection().serialize(nameComponent);
        hologram.setCustomName(nameText);
        hologram.setCustomNameVisible(true);

        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setSeeThrough(true);
        hologram.setShadowed(false);
        hologram.setBrightness(new Display.Brightness(15, 15));
        hologram.addScoreboardTag("vitaminwaystone");
        hologram.addScoreboardTag("waystone_holo_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
        hologram.setPersistent(true);

        return hologram;
    }

    private void saveWaystone(Waystone waystone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseHandler.WaystoneData data = new DatabaseHandler.WaystoneData(
                    waystone.getId(), waystone.getLocation(), waystone.getName(), waystone.getCreator(),
                    waystone.isDiscoverable(), waystone.isGlobal(), waystone.getIconData(), waystone.isNameVisible(),
                    waystone.getBaseMaterial());
            int id = DatabaseHandler.saveWaystone(data);
            waystone.setId(id);
            for (UUID playerId : waystone.getRegisteredPlayers()) {
                DatabaseHandler.registerPlayerToWaystone(id, playerId);
            }
            for (UUID playerId : waystone.getAllowedPlayers()) {
                DatabaseHandler.addWaystonePermission(id, playerId);
            }
        });
    }

    private void removeWaystone(Waystone waystone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                DatabaseHandler.removeWaystone(waystone.getId())
        );
    }

    private void saveWaystoneSettings(Waystone waystone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseHandler.updateWaystoneSettings(waystone.getId(), waystone.isDiscoverable(), waystone.isGlobal(), waystone.isNameVisible());

            for (UUID playerId : waystone.getAllowedPlayers()) {
                DatabaseHandler.addWaystonePermission(waystone.getId(), playerId);
            }
        });
    }

    private GuiItem createGlassPane(Material glassType) {
        ItemStack glass = new ItemStack(glassType);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return new GuiItem(glass);
    }

    private void openWaystoneEditGUI(Player player, Waystone waystone) {
        editingWaystones.put(player.getUniqueId(), waystone);

        Component title = processColorCodes(plugin.getConfig().getString("waystone.gui.edit.title", "Edit Waystone"));

        Gui gui = Gui.gui()
                .title(title)
                .rows(3)
                .disableAllInteractions()
                .create();

        GuiItem glassPane = createGlassPane(Material.WHITE_STAINED_GLASS_PANE);
        gui.getFiller().fill(glassPane);

        // Visibility Item (slot 9)
        ItemStack visibilityItem;
        if (waystone.isGlobal()) {
            visibilityItem = new ItemStack(Material.BLUE_DYE);
        } else if (waystone.isDiscoverable()) {
            visibilityItem = new ItemStack(Material.LIME_DYE);
        } else {
            visibilityItem = new ItemStack(Material.RED_DYE);
        }

        ItemMeta visibilityMeta = visibilityItem.getItemMeta();
        if (visibilityMeta != null) {
            String visibilityNamePath = waystone.isGlobal() ? "waystone.gui.edit.visibility.global.name" :
                    waystone.isDiscoverable() ? "waystone.gui.edit.visibility.discoverable.name" :
                            "waystone.gui.edit.visibility.private.name";
            String visibilityLorePath = waystone.isGlobal() ? "waystone.gui.edit.visibility.global.lore" :
                    waystone.isDiscoverable() ? "waystone.gui.edit.visibility.discoverable.lore" :
                            "waystone.gui.edit.visibility.private.lore";

            Component visibilityNameComponent = processColorCodes(plugin.getConfig().getString(visibilityNamePath,
                    waystone.isGlobal() ? "Global Waystone" : waystone.isDiscoverable() ? "Discoverable Waystone" : "Private Waystone"))
                    .decoration(TextDecoration.ITALIC, false);
            String visibilityName = LegacyComponentSerializer.legacySection().serialize(visibilityNameComponent);
            visibilityMeta.setDisplayName(visibilityName);

            List<String> visibilityLoreConfig = plugin.getConfig().getStringList(visibilityLorePath);
            List<String> visibilityLore = new ArrayList<>();
            for (String loreLine : visibilityLoreConfig) {
                Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                visibilityLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
            }
            visibilityMeta.setLore(visibilityLore);
            visibilityItem.setItemMeta(visibilityMeta);
        }

        gui.setItem(9, new GuiItem(visibilityItem, (event) -> handleEditGUIClick(9, player, waystone)));

        // Rename Item (slot 11)
        ItemStack renameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta renameMeta = renameItem.getItemMeta();
        if (renameMeta != null) {
            String renameName = plugin.getConfig().getString("waystone.gui.edit.rename.name", "Rename Waystone");
            Component renameNameComponent = processColorCodes(renameName).decoration(TextDecoration.ITALIC, false);
            String renameNameText = LegacyComponentSerializer.legacySection().serialize(renameNameComponent);
            renameMeta.setDisplayName(renameNameText);

            List<String> renameLoreConfig = plugin.getConfig().getStringList("waystone.gui.edit.rename.lore");
            List<String> renameLore = new ArrayList<>();
            for (String loreLine : renameLoreConfig) {
                String processedLine = loreLine.replace("{name}", waystone.getName());
                Component loreComponent = processColorCodes(processedLine).decoration(TextDecoration.ITALIC, false);
                renameLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
            }
            renameMeta.setLore(renameLore);
            renameItem.setItemMeta(renameMeta);
        }

        gui.setItem(11, new GuiItem(renameItem, (event) -> handleEditGUIClick(11, player, waystone)));

        // Icon Item (slot 13)
        ItemStack iconItem;
        if (waystone.getIconData() != null) {
            ItemStack customIcon = deserializeItemStack(waystone.getIconData());
            if (customIcon != null) {
                iconItem = customIcon.clone();
            } else {
                iconItem = new ItemStack(Material.PAINTING);
            }
        } else {
            iconItem = new ItemStack(Material.PAINTING);
        }

        ItemMeta iconMeta = iconItem.getItemMeta();
        if (iconMeta != null) {
            Component iconNameComponent = processColorCodes(plugin.getConfig().getString("waystone.gui.edit.change_icon.name", "Change Icon"))
                    .decoration(TextDecoration.ITALIC, false);
            String iconNameText = LegacyComponentSerializer.legacySection().serialize(iconNameComponent);
            iconMeta.setDisplayName(iconNameText);

            List<String> iconLoreConfig = plugin.getConfig().getStringList("waystone.gui.edit.change_icon.lore");
            List<String> iconLore = new ArrayList<>();
            for (String loreLine : iconLoreConfig) {
                Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                iconLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
            }
            if (iconLore.isEmpty()) {
                Component defaultLore = processColorCodes("<gray>Click to change waystone icon").decoration(TextDecoration.ITALIC, false);
                iconLore.add(LegacyComponentSerializer.legacySection().serialize(defaultLore));
            }
            iconMeta.setLore(iconLore);
            iconItem.setItemMeta(iconMeta);
        }

        gui.setItem(13, new GuiItem(iconItem, (event) -> handleEditGUIClick(13, player, waystone)));

        // Players Item (slot 15)
        ItemStack playersItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playersMeta = playersItem.getItemMeta();
        if (playersMeta != null) {
            if (waystone.isDiscoverable() || waystone.isGlobal()) {
                String playersName = plugin.getConfig().getString("waystone.gui.edit.visibility.discoverable.name", "All Players can use");
                Component playersNameComponent = processColorCodes(playersName).decoration(TextDecoration.ITALIC, false);
                String playersNameText = LegacyComponentSerializer.legacySection().serialize(playersNameComponent);
                playersMeta.setDisplayName(playersNameText);
            } else {
                String playersName = plugin.getConfig().getString("waystone.gui.edit.manage_players.name", "Manage Players");
                Component playersNameComponent = processColorCodes(playersName).decoration(TextDecoration.ITALIC, false);
                String playersNameText = LegacyComponentSerializer.legacySection().serialize(playersNameComponent);
                playersMeta.setDisplayName(playersNameText);

                List<String> playersLoreConfig = plugin.getConfig().getStringList("waystone.gui.edit.manage_players.lore");
                List<String> playersLore = new ArrayList<>();
                for (String loreLine : playersLoreConfig) {
                    String processedLine = loreLine.replace("{count}", String.valueOf(waystone.getAllowedPlayers().size()));
                    Component loreComponent = processColorCodes(processedLine).decoration(TextDecoration.ITALIC, false);
                    playersLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
                }
                playersMeta.setLore(playersLore);
            }
            playersItem.setItemMeta(playersMeta);
        }

        gui.setItem(15, new GuiItem(playersItem, (event) -> handleEditGUIClick(15, player, waystone)));

        // Delete Item (slot 17)
        ItemStack deleteItem = new ItemStack(Material.TNT);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        if (deleteMeta != null) {
            String deleteName = plugin.getConfig().getString("waystone.gui.edit.delete.name", "Remove Waystone");
            Component deleteNameComponent = processColorCodes(deleteName).decoration(TextDecoration.ITALIC, false);
            String deleteNameText = LegacyComponentSerializer.legacySection().serialize(deleteNameComponent);
            deleteMeta.setDisplayName(deleteNameText);

            List<String> deleteLoreConfig = plugin.getConfig().getStringList("waystone.gui.edit.delete.lore");
            List<String> deleteLore = new ArrayList<>();
            for (String loreLine : deleteLoreConfig) {
                Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                deleteLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
            }
            deleteMeta.setLore(deleteLore);
            deleteItem.setItemMeta(deleteMeta);
        }

        gui.setItem(17, new GuiItem(deleteItem, (event) -> handleEditGUIClick(17, player, waystone)));

        // Name Visibility Item (slot 4)
        ItemStack nameVisibilityItem = new ItemStack(waystone.isNameVisible() ? Material.ENDER_EYE : Material.BARRIER);
        ItemMeta nameVisibilityMeta = nameVisibilityItem.getItemMeta();
        if (nameVisibilityMeta != null) {
            String nameVisibilityName = waystone.isNameVisible() ?
                    plugin.getConfig().getString("waystone.gui.edit.name_visible.visible.name", "Name Visible") :
                    plugin.getConfig().getString("waystone.gui.edit.name_visible.hidden.name", "Name Hidden");
            Component nameVisibilityNameComponent = processColorCodes(nameVisibilityName).decoration(TextDecoration.ITALIC, false);
            String nameVisibilityNameText = LegacyComponentSerializer.legacySection().serialize(nameVisibilityNameComponent);
            nameVisibilityMeta.setDisplayName(nameVisibilityNameText);

            List<String> nameVisibilityLoreConfig = plugin.getConfig().getStringList(waystone.isNameVisible() ?
                    "waystone.gui.edit.name_visible.visible.lore" : "waystone.gui.edit.name_visible.hidden.lore");
            List<String> nameVisibilityLore = new ArrayList<>();
            for (String loreLine : nameVisibilityLoreConfig) {
                Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                nameVisibilityLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
            }
            if (nameVisibilityLore.isEmpty()) {
                Component defaultLore = processColorCodes(waystone.isNameVisible() ?
                        "<gray>Click to hide waystone name" : "<gray>Click to show waystone name")
                        .decoration(TextDecoration.ITALIC, false);
                nameVisibilityLore.add(LegacyComponentSerializer.legacySection().serialize(defaultLore));
            }
            nameVisibilityMeta.setLore(nameVisibilityLore);
            nameVisibilityItem.setItemMeta(nameVisibilityMeta);
        }

        gui.setItem(4, new GuiItem(nameVisibilityItem, (event) -> handleEditGUIClick(4, player, waystone)));

        // Close Item (slot 22)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            String closeName = plugin.getConfig().getString("waystone.gui.edit.close.name", "<gray>Close");
            Component closeNameComponent = processColorCodes(closeName).decoration(TextDecoration.ITALIC, false);
            String closeNameText = LegacyComponentSerializer.legacySection().serialize(closeNameComponent);
            closeMeta.setDisplayName(closeNameText);
            closeItem.setItemMeta(closeMeta);
        }

        gui.setItem(22, new GuiItem(closeItem, (event) -> handleEditGUIClick(22, player, waystone)));

        gui.open(player);
    }

    private void openPlayerManagementGUI(Player player, Waystone waystone) {
        Component title = processColorCodes(plugin.getConfig().getString("waystone.gui.player_management.title", "<dark_green>Manage Players"));

        Gui gui = Gui.gui()
                .title(title)
                .rows(6)
                .disableAllInteractions()
                .create();

        GuiItem glassPane = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 45; i < 54; i++) {
            if (i != 45 && i != 53) {
                gui.setItem(i, glassPane);
            }
        }

        Set<UUID> allowedPlayers = waystone.getAllowedPlayers();
        int slot = 0;

        for (UUID playerId : allowedPlayers) {
            if (slot >= 45) break;

            Player allowedPlayer = Bukkit.getPlayer(playerId);
            String playerName = allowedPlayer != null ? allowedPlayer.getName() : "Unknown Player";

            ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta playerMeta = playerItem.getItemMeta();

            if (playerMeta instanceof SkullMeta skullMeta && allowedPlayer != null) {
                skullMeta.setOwningPlayer(allowedPlayer);
            }

            if (playerMeta != null) {
                Component playerNameComponent = Component.text(playerName, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);
                String playerNameText = LegacyComponentSerializer.legacySection().serialize(playerNameComponent);
                playerMeta.setDisplayName(playerNameText);

                List<String> removeLoreConfig = plugin.getConfig().getStringList("waystone.gui.player_management.remove_player.lore");
                List<String> playerLore = new ArrayList<>();
                for (String loreLine : removeLoreConfig) {
                    String processedLine = loreLine.replace("{uuid}", playerId.toString());
                    Component loreComponent = processColorCodes(processedLine).decoration(TextDecoration.ITALIC, false);
                    playerLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
                }
                playerMeta.setLore(playerLore);
                playerItem.setItemMeta(playerMeta);
            }

            final int currentSlot = slot;
            gui.setItem(slot++, new GuiItem(playerItem, (event) -> handlePlayerManagementClick(player, waystone, playerItem, currentSlot)));
        }

        // Add Item (slot 45)
        ItemStack addItem = new ItemStack(Material.EMERALD);
        ItemMeta addMeta = addItem.getItemMeta();
        if (addMeta != null) {
            String addName = plugin.getConfig().getString("waystone.gui.player_management.add_player.name", "Add Player");
            Component addNameComponent = processColorCodes(addName).decoration(TextDecoration.ITALIC, false);
            String addNameText = LegacyComponentSerializer.legacySection().serialize(addNameComponent);
            addMeta.setDisplayName(addNameText);

            List<String> addLoreConfig = plugin.getConfig().getStringList("waystone.gui.player_management.add_player.lore");
            List<String> addLore = new ArrayList<>();
            for (String loreLine : addLoreConfig) {
                Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                addLore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
            }
            addMeta.setLore(addLore);
            addItem.setItemMeta(addMeta);
        }

        gui.setItem(45, new GuiItem(addItem, (event) -> handlePlayerManagementClick(player, waystone, addItem, 45)));

        // Back Item (slot 53)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            String backName = plugin.getConfig().getString("waystone.gui.player_management.back.name", "Back");
            Component backNameComponent = processColorCodes(backName).decoration(TextDecoration.ITALIC, false);
            String backNameText = LegacyComponentSerializer.legacySection().serialize(backNameComponent);
            backMeta.setDisplayName(backNameText);
            backItem.setItemMeta(backMeta);
        }

        gui.setItem(53, new GuiItem(backItem, (event) -> handlePlayerManagementClick(player, waystone, backItem, 53)));

        gui.open(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (isWaystoneCore(item)) {
            if (!player.hasPermission("vitamin.module.waystone") ||
                    !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.waystone")) {
                event.setCancelled(true);
                return;
            }

            Location blockLocation = event.getBlock().getLocation();

            if (restrictedWorlds.contains(blockLocation.getWorld().getName())) {
                TextHandler.get().sendMessage(player, "waystone.restricted_world");
                event.setCancelled(true);
                return;
            }

            // Cancel placement of another Core waystone if you are in the process of creating a waystone
            if (pendingWaystones.containsKey(player.getUniqueId())) {
                TextHandler.get().sendMessage(player, "waystone.creation_in_progress");
                event.setCancelled(true);
                return;
            }

            if (waystones.containsKey(blockLocation)) {
                TextHandler.get().sendMessage(player, "waystone.already_exists");
                event.setCancelled(true);
                return;
            }

            // Cancel creation if the upper block is not air
            Location upperBlock = blockLocation.clone().add(0, 1, 0);
            if (upperBlock.getBlock().getType() != Material.AIR) {
                TextHandler.get().sendMessage(player, "waystone.upper_block_not_air");
                event.setCancelled(true);
                return;
            }

            // Prevent new waystones from being created within configured distance
            if (isTooCloseToExistingWaystone(blockLocation)) {
                double minDistance = Math.sqrt(minWaystoneDistanceSquared);
                TextHandler.get().sendMessage(player, "waystone.too_close_to_existing", String.valueOf((int) minDistance));
                event.setCancelled(true);
                return;
            }

            if (!canCreateWaystone(player)) {
                int limit = getPlayerWaystoneLimit(player);
                TextHandler.get().sendMessage(player, "waystone.limit_reached", String.valueOf(limit));
                event.setCancelled(true);
                return;
            }

            // Check integration permissions
            if (!canCreateWaystoneWithIntegrations(player, blockLocation)) {
                TextHandler.get().sendMessage(player, "waystone.no_build_permission");
                event.setCancelled(true);
                return;
            }

            UUID playerId = player.getUniqueId();

            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            Material baseMaterial = Material.STONE;
            if (offHandItem != null && offHandItem.getType().isBlock() &&
                    BlockDisplayUtils.hasTheme(offHandItem.getType())) {
                baseMaterial = offHandItem.getType();
            }

            Bukkit.getScheduler().runTask(plugin, () -> placePendingWaystoneCore(blockLocation));

            pendingWaystones.put(playerId, new PendingWaystone(blockLocation, System.currentTimeMillis(), baseMaterial));

            if (enableCreationEffects) {
                playWaystoneActivateSound(blockLocation);
                if (enableParticles) {
                    blockLocation.getWorld().spawnParticle(
                            Particle.GLOW,
                            blockLocation.clone().add(0.5, 1.5, 0.5),
                            20,
                            0.3, 0.5, 0.3,
                            0.05);
                }
            }

            TextHandler.get().sendMessage(player, "waystone.enter_new_name");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractWithBarrier(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BARRIER) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.waystone") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.waystone")) {
            return;
        }

        Location clickedLocation = event.getClickedBlock().getLocation();

        Waystone targetWaystone;
        Location waystoneLocation = null;

        if (waystones.containsKey(clickedLocation)) {
            targetWaystone = waystones.get(clickedLocation);
            waystoneLocation = clickedLocation;
        } else if (waystones.containsKey(clickedLocation.clone().add(0, -1, 0))) {
            targetWaystone = waystones.get(clickedLocation.clone().add(0, -1, 0));
            waystoneLocation = clickedLocation.clone().add(0, -1, 0);
        } else {
            targetWaystone = null;
        }

        if (targetWaystone == null) return;

        event.setCancelled(true);
        UUID playerId = player.getUniqueId();

        if (player.isSneaking() && (targetWaystone.getCreator().equals(playerId) || isAdmin(player))) {
            openWaystoneEditGUI(player, targetWaystone);
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }
            return;
        }

        // Check integration permissions for usage
        if (!canUseWaystoneWithIntegrations(player, waystoneLocation)) {
            TextHandler.get().sendMessage(player, "waystone.no_interact_permission");
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
            }
            return;
        }

        if (!targetWaystone.isPlayerAllowed(playerId) && !isAdmin(player)) {
            TextHandler.get().sendMessage(player, "waystone.no_permission");
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
            }
            return;
        }

        if (!targetWaystone.isRegistered(playerId)) {
            targetWaystone.registerPlayer(playerId);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    DatabaseHandler.registerPlayerToWaystone(targetWaystone.getId(), playerId)
            );

            String miniMessageFormattedName = convertToMiniMessageFormat(targetWaystone.getName());
            TextHandler.get().sendMessage(player, "waystone.registered", miniMessageFormattedName);

            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7f, 1.2f);
            }
            if (enableParticles) {
                waystoneLocation.getWorld().spawnParticle(
                        Particle.COMPOSTER,
                        waystoneLocation.clone().add(0.5, 1.8, 0.5),
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Location clickedLocation = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;

        if (clickedLocation == null) return;

        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!tool.getType().name().endsWith("_PICKAXE")) return;

            Waystone targetWaystone = null;
            Location waystoneLocation = null;

            if (waystones.containsKey(clickedLocation)) {
                targetWaystone = waystones.get(clickedLocation);
                waystoneLocation = clickedLocation;
            } else if (waystones.containsKey(clickedLocation.clone().add(0, -1, 0))) {
                targetWaystone = waystones.get(clickedLocation.clone().add(0, -1, 0));
                waystoneLocation = clickedLocation.clone().add(0, -1, 0);
            }

            if (targetWaystone == null) return;

            event.setCancelled(true);

            UUID playerId = player.getUniqueId();
            boolean isCreator = targetWaystone.getCreator().equals(playerId);
            boolean isAdmin = player.hasPermission("vitamin.module.waystone.admin");
            boolean isOp = player.isOp();

            // Check integration permissions for breaking
            if (!canBreakWaystoneWithIntegrations(player, waystoneLocation)) {
                TextHandler.get().sendMessage(player, "waystone.no_break_permission");
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
                }
                return;
            }

            if (onlyCreatorCanBreak && !isCreator) {
                if (!(isAdmin && (isOp || targetWaystone.isAdminCreated()))) {
                    TextHandler.get().sendMessage(player, "waystone.only_creator_can_break");
                    if (enableSounds) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
                    }
                    return;
                }
            }

            if (targetWaystone.isAdminCreated() && !(isOp || isCreator)) {
                TextHandler.get().sendMessage(player, "waystone.only_operator_or_creator_can_break");
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
                }
                return;
            }

            removeWaystoneEntities(targetWaystone);
            removeWaystoneBarrierPillar(waystoneLocation);
            waystones.remove(waystoneLocation);
            removeWaystone(targetWaystone);

            ItemStack waystoneCoreItem = createWaystoneCore();
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(waystoneCoreItem);
            } else {
                waystoneLocation.getWorld().dropItemNaturally(waystoneLocation, waystoneCoreItem);
            }

            playWaystoneBreakSound(waystoneLocation);
            if (enableParticles) {
                waystoneLocation.getWorld().spawnParticle(
                        Particle.LAVA,
                        waystoneLocation.clone().add(0.5, 1.5, 0.5),
                        15,
                        0.4, 0.8, 0.4,
                        0.15);
            }

            String miniMessageFormattedName = convertToMiniMessageFormat(targetWaystone.getName());
            TextHandler.get().sendMessage(player, "waystone.destroyed", miniMessageFormattedName);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (addingPlayerToWaystone.containsKey(playerId)) {
            event.setCancelled(true);
            String playerName = event.getMessage().trim();

            Waystone waystone = editingWaystones.get(playerId);
            if (waystone != null) {
                Player targetPlayer = Bukkit.getServer().getPlayer(playerName);
                if (targetPlayer != null) {
                    waystone.addAllowedPlayer(targetPlayer.getUniqueId());
                    saveWaystoneSettings(waystone);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        TextHandler.get().sendMessage(player, "waystone.player_added", targetPlayer.getName());
                        openPlayerManagementGUI(player, waystone);
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        TextHandler.get().sendMessage(player, "waystone.player_not_found");
                        openPlayerManagementGUI(player, waystone);
                    });
                }
            }
            addingPlayerToWaystone.remove(playerId);
            return;
        }

        if (renamingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            String newName = event.getMessage().trim();

            if (newName.length() > nameCharacterLimit) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    TextHandler.get().sendMessage(player, "waystone.name_too_long", String.valueOf(nameCharacterLimit));
                    TextHandler.get().sendMessage(player, "waystone.rename_prompt");
                });
                return;
            }

            Waystone waystone = renamingWaystones.remove(playerId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (waystones.containsKey(waystone.getLocation()) && waystone.getHologram() != null) {
                    Component nameComponent = processColorCodes(newName);
                    String nameText = LegacyComponentSerializer.legacySection().serialize(nameComponent);
                    waystone.getHologram().setCustomName(nameText);
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

                Bukkit.getScheduler().runTaskLater(plugin, () -> openWaystoneEditGUI(player, waystone), 1L);
            });
            return;
        }

        if (pendingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            PendingWaystone pending = pendingWaystones.get(playerId);
            Location loc = pending.location();

            if (!canCreateWaystone(player)) {
                int limit = getPlayerWaystoneLimit(player);
                TextHandler.get().sendMessage(player, "waystone.limit_reached", String.valueOf(limit));
                pendingWaystones.remove(playerId);

                dropWaystoneCoreFromPending(loc);

                if (enableCreationEffects) {
                    playWaystoneDeactivateSound(loc);
                }
                return;
            }

            String name = event.getMessage().trim();

            if (name.length() > nameCharacterLimit) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    TextHandler.get().sendMessage(player, "waystone.name_too_long", String.valueOf(nameCharacterLimit));
                    TextHandler.get().sendMessage(player, "waystone.enter_new_name");
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (waystones.containsKey(loc)) {
                    TextHandler.get().sendMessage(player, "waystone.already_exists");
                    dropWaystoneCoreFromPending(loc);

                    if (enableCreationEffects) {
                        playWaystoneDeactivateSound(loc);
                    }
                    return;
                }

                if (!canCreateWaystone(player)) {
                    int limit = getPlayerWaystoneLimit(player);
                    TextHandler.get().sendMessage(player, "waystone.limit_reached", String.valueOf(limit));
                    dropWaystoneCoreFromPending(loc);

                    if (enableCreationEffects) {
                        playWaystoneDeactivateSound(loc);
                    }
                    return;
                }

                Location upperBlock = loc.clone().add(0, 1, 0);
                if (upperBlock.getBlock().getType() != Material.AIR) {
                    TextHandler.get().sendMessage(player, "waystone.upper_block_not_air");
                    dropWaystoneCoreFromPending(loc);

                    if (enableCreationEffects) {
                        playWaystoneDeactivateSound(loc);
                    }
                    return;
                }

                removePendingWaystoneCore(loc);

                Material baseMaterial = pending.baseMaterial();
                Waystone waystone = new Waystone(-1, loc, name, playerId, baseMaterial);
                waystone.registerPlayer(playerId);

                createWaystoneBarrierPillar(loc);
                createWaystoneBlockDisplays(loc, waystone);

                TextDisplay hologram = createHologram(loc, name);
                waystone.setHologram(hologram);

                waystones.put(loc, waystone);
                saveWaystone(waystone);

                if (enableCreationEffects) {
                    playWaystoneCreateSound(loc);
                    spawnCreationParticles(loc);
                }

                String miniMessageFormattedName = convertToMiniMessageFormat(name);
                TextHandler.get().sendMessage(player, "waystone.created", miniMessageFormattedName);
            });
            pendingWaystones.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();

        if (isPendingWaystoneCore(loc)) {
            event.setCancelled(true);

            for (Map.Entry<UUID, PendingWaystone> entry : pendingWaystones.entrySet()) {
                if (entry.getValue().location().equals(loc)) {
                    UUID playerId = entry.getKey();
                    pendingWaystones.remove(playerId);

                    event.getBlock().setType(Material.AIR);

                    if (enableCreationEffects) {
                        playWaystoneDeactivateSound(loc);
                    }

                    Player targetPlayer = Bukkit.getPlayer(playerId);
                    if (targetPlayer != null) {
                        TextHandler.get().sendMessage(targetPlayer, "waystone.creation_canceled");
                    }

                    ItemStack waystoneCoreItem = createWaystoneCore();
                    loc.getWorld().dropItemNaturally(loc, waystoneCoreItem);
                    return;
                }
            }
        }

        for (Location waystoneLocation : waystones.keySet()) {
            if (loc.equals(waystoneLocation) || loc.equals(waystoneLocation.clone().add(0, 1, 0))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (cancelTeleportOnMove && pendingTeleports.containsKey(playerId)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                BukkitTask task = pendingTeleports.remove(playerId);
                if (task != null) {
                    task.cancel();
                }
                playerTeleportLocations.remove(playerId);
                refundCost(player);

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
                if (!canCreateWaystone(player)) {
                    TextHandler.get().sendMessage(player, "waystone.auto_creation_canceled_limit");
                    pendingWaystones.remove(playerId);

                    dropWaystoneCoreFromPending(loc);

                    if (enableCreationEffects) {
                        playWaystoneDeactivateSound(loc);
                    }
                    return;
                }

                removePendingWaystoneCore(loc);

                Material baseMaterial = pending.baseMaterial();
                Waystone waystone = new Waystone(-1, loc, defaultWaystoneName, playerId, baseMaterial);
                waystone.registerPlayer(playerId);

                createWaystoneBarrierPillar(loc);
                createWaystoneBlockDisplays(loc, waystone);

                TextDisplay hologram = createHologram(loc, defaultWaystoneName);
                waystone.setHologram(hologram);

                waystones.put(loc, waystone);
                saveWaystone(waystone);

                if (enableCreationEffects) {
                    playWaystoneCreateSound(loc);
                    spawnCreationParticles(loc);
                }

                String miniMessageFormattedName = convertToMiniMessageFormat(defaultWaystoneName);
                TextHandler.get().sendMessage(player, "waystone.created_with_default", miniMessageFormattedName);
                pendingWaystones.remove(playerId);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        if (pendingWaystones.containsKey(playerId)) {
            PendingWaystone pending = pendingWaystones.remove(playerId);
            dropWaystoneCoreFromPending(pending.location());
        }

        renamingWaystones.remove(playerId);
        editingWaystones.remove(playerId);
        addingPlayerToWaystone.remove(playerId);
        playerTeleportLocations.remove(playerId);
        changingIconWaystones.remove(playerId);

        if (pendingTeleports.containsKey(playerId)) {
            pendingTeleports.get(playerId).cancel();
            pendingTeleports.remove(playerId);
            refundCost(Bukkit.getPlayer(playerId));
        }
    }

    private void openWaystoneInventory(Player player) {
        Location playerLocation = player.getLocation();
        Waystone currentWaystone = getCurrentWaystone(playerLocation);

        Component titleComponent = processColorCodes(plugin.getConfig().getString("waystone.gui.discovered.title", "Discovered Waystones"));

        List<Waystone> availableWaystones;
        if (isAdmin(player)) {
            availableWaystones = waystones.values().stream()
                    .filter(waystone -> !waystone.equals(currentWaystone))
                    .collect(Collectors.toList());
        } else {
            availableWaystones = waystones.values().stream()
                    .filter(waystone -> (waystone.isGlobal() || waystone.isRegistered(player.getUniqueId())) &&
                            waystone.isPlayerAllowed(player.getUniqueId()) &&
                            !waystone.equals(currentWaystone))
                    .collect(Collectors.toList());
        }

        int rows = inventorySize / 9;
        int pageSize = inventorySize - 9;

        ScrollingGui gui = Gui.scrolling()
                .title(titleComponent)
                .rows(rows)
                .pageSize(pageSize)
                .disableAllInteractions()
                .create();

        for (Waystone waystone : availableWaystones) {
            ItemStack item = createWaystoneItem(waystone);
            gui.addItem(new GuiItem(item, (event) -> handleWaystoneInventoryClick(player, item, event.getClick())));
        }

        int lastRowStartSlot = (rows - 1) * 9;
        GuiItem glassPane = createGlassPane(Material.WHITE_STAINED_GLASS_PANE);

        for (int i = 0; i < 9; i++) {
            gui.setItem(lastRowStartSlot + i, glassPane);
        }

        if (availableWaystones.isEmpty()) {
            ItemStack infoItem = createInfoItem(currentWaystone != null);
            gui.setItem(lastRowStartSlot + 4, new GuiItem(infoItem));
        } else {
            if (availableWaystones.size() > pageSize) {
                // Scroll Up button
                ItemStack scrollUpItem = createNavigationItem(scrollUpMaterial, scrollUpName, scrollUpLore);
                gui.setItem(lastRowStartSlot + 1, new GuiItem(scrollUpItem, (event) -> {
                    gui.previous();
                    if (enableSounds) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.5f, 1.2f);
                    }
                }));

                // Scroll Down button
                ItemStack scrollDownItem = createNavigationItem(scrollDownMaterial, scrollDownName, scrollDownLore);
                gui.setItem(lastRowStartSlot + 7, new GuiItem(scrollDownItem, (event) -> {
                    gui.next();
                    if (enableSounds) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.5f, 0.8f);
                    }
                }));
            }

            // Close button
            ItemStack closeItem = createNavigationItem(closeMaterial, closeName, closeLore);
            gui.setItem(lastRowStartSlot + 4, new GuiItem(closeItem, (event) -> {
                player.closeInventory();
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
            }));
        }

        gui.open(player);
    }

    private Waystone getCurrentWaystone(Location playerLocation) {
        return waystones.values().stream()
                .filter(waystone -> {
                    Location waystoneLocation = waystone.getLocation();
                    return waystoneLocation.getWorld().equals(playerLocation.getWorld()) &&
                            waystoneLocation.distanceSquared(playerLocation) <= 25;
                })
                .findFirst()
                .orElse(null);
    }

    private ItemStack createInfoItem(boolean hasCurrentWaystone) {
        ItemStack infoItem = new ItemStack(infoItemMaterial);
        ItemMeta meta = infoItem.getItemMeta();

        if (meta != null) {
            if (hasCurrentWaystone) {
                Component nameComponent = processColorCodes(noOtherWaystonesName)
                        .decoration(TextDecoration.ITALIC, false);
                String nameText = LegacyComponentSerializer.legacySection().serialize(nameComponent);
                meta.setDisplayName(nameText);

                List<String> lore = new ArrayList<>();
                for (String loreLine : noOtherWaystonesLore) {
                    Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                    lore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
                }
                meta.setLore(lore);
            } else {
                Component nameComponent = processColorCodes(noWaystonesName)
                        .decoration(TextDecoration.ITALIC, false);
                String nameText = LegacyComponentSerializer.legacySection().serialize(nameComponent);
                meta.setDisplayName(nameText);

                List<String> lore = new ArrayList<>();
                for (String loreLine : noWaystonesLore) {
                    Component loreComponent = processColorCodes(loreLine).decoration(TextDecoration.ITALIC, false);
                    lore.add(LegacyComponentSerializer.legacySection().serialize(loreComponent));
                }
                meta.setLore(lore);
            }

            infoItem.setItemMeta(meta);
        }

        return infoItem;
    }

    private ItemStack createNavigationItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            Component nameComponent = processColorCodes(name).decoration(TextDecoration.ITALIC, false);
            String nameText = LegacyComponentSerializer.legacySection().serialize(nameComponent);
            meta.setDisplayName(nameText);

            if (lore != null && !lore.isEmpty()) {
                List<String> loreComponents = lore.stream()
                        .map(line -> {
                            Component loreComponent = processColorCodes(line).decoration(TextDecoration.ITALIC, false);
                            return LegacyComponentSerializer.legacySection().serialize(loreComponent);
                        })
                        .collect(Collectors.toList());
                meta.setLore(loreComponents);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static Component processColorCodesStatic(String input) {
        return instance != null ? instance.processColorCodes(input) : Component.text(input);
    }

    public ItemStack getWaystoneCoreItem() {
        return createWaystoneCore();
    }

    @Override
    public void cancelTasks() {
        if (waystoneUpdateTask != null) {
            waystoneUpdateTask.cancel();
            waystoneUpdateTask = null;
        }
        if (teleportParticlesTask != null) {
            teleportParticlesTask.cancel();
            teleportParticlesTask = null;
        }
    }

    private static class Waystone {
        private volatile int id;
        private final Location location;
        private volatile String name;
        private final UUID creator;
        private final Set<UUID> registeredPlayers = ConcurrentHashMap.newKeySet();
        private volatile TextDisplay hologram;
        private volatile List<BlockDisplay> blockDisplays = new ArrayList<>();
        private final boolean isAdminCreated;
        private volatile boolean isPublic = true;
        private volatile boolean isGlobal = false;
        private final Set<UUID> allowedPlayers = ConcurrentHashMap.newKeySet();
        private volatile String iconData;
        private volatile boolean nameVisible = true;
        private final Material baseMaterial;

        public Waystone(int id, Location location, String name, UUID creator, Material baseMaterial) {
            this.id = id;
            this.location = location;
            this.name = name;
            this.creator = creator;
            this.isAdminCreated = Bukkit.getPlayer(creator) != null &&
                    Objects.requireNonNull(Bukkit.getPlayer(creator)).hasPermission("vitamin.module.waystone.admin");
            this.iconData = null;
            this.nameVisible = true;
            this.baseMaterial = baseMaterial != null ? baseMaterial : Material.STONE;
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
        public List<BlockDisplay> getBlockDisplays() { return blockDisplays; }
        public void setBlockDisplays(List<BlockDisplay> blockDisplays) { this.blockDisplays = blockDisplays; }
        public Set<UUID> getRegisteredPlayers() { return new HashSet<>(registeredPlayers); }
        public void setRegisteredPlayers(Set<UUID> players) {
            registeredPlayers.clear();
            registeredPlayers.addAll(players);
        }
        public boolean isAdminCreated() { return isAdminCreated; }

        public boolean isDiscoverable() { return isPublic; }
        public void setDiscoverable(boolean isPublic) { this.isPublic = isPublic; }

        public boolean isGlobal() { return isGlobal; }
        public void setGlobal(boolean isGlobal) { this.isGlobal = isGlobal; }

        public Set<UUID> getAllowedPlayers() { return new HashSet<>(allowedPlayers); }
        public void setAllowedPlayers(Set<UUID> players) {
            allowedPlayers.clear();
            allowedPlayers.addAll(players);
        }

        public void addAllowedPlayer(UUID playerId) { allowedPlayers.add(playerId); }
        public void removeAllowedPlayer(UUID playerId) { allowedPlayers.remove(playerId); }
        public boolean isPlayerAllowed(UUID playerId) {
            return isPublic || creator.equals(playerId) || allowedPlayers.contains(playerId);
        }

        public String getIconData() { return iconData; }
        public void setIconData(String iconData) { this.iconData = iconData; }

        public boolean isNameVisible() { return nameVisible; }
        public void setNameVisible(boolean nameVisible) {
            this.nameVisible = nameVisible;
            if (hologram != null && !hologram.isDead()) {
                if (nameVisible) {
                    Component nameComponent = WaystoneModule.processColorCodesStatic(name);
                    String nameText = LegacyComponentSerializer.legacySection().serialize(nameComponent);
                    hologram.setCustomName(nameText);
                    hologram.setCustomNameVisible(true);
                } else {
                    hologram.setCustomNameVisible(false);
                }
            }
        }

        public Material getBaseMaterial() { return baseMaterial; }
    }

    private record PendingWaystone(Location location, long creationTime, Material baseMaterial) {}
}