package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.integration.GriefPreventionIntegrationHandler;
import com.soystargaze.vitamin.integration.LandsIntegrationHandler;
import com.soystargaze.vitamin.integration.LootinIntegrationHandler;
import com.soystargaze.vitamin.integration.WorldGuardIntegrationHandler;
import com.soystargaze.vitamin.utils.LogUtils;
import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.EntityDismountEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class CarryOnModule implements Listener {

    private final JavaPlugin plugin;
    private final double maxCarryWeight;
    private final NamespacedKey storedBlockKey;
    private final NamespacedKey chestIdKey;
    private WorldGuardIntegrationHandler wgIntegration;
    private LandsIntegrationHandler landsIntegration;
    private LootinIntegrationHandler lootinIntegration;
    private final boolean allowStacking;
    private GriefPreventionIntegrationHandler gpIntegration;
    private final boolean preventContainerStacking;

    public CarryOnModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.maxCarryWeight = plugin.getConfig().getDouble("carry_on.max_weight", 100.0);
        this.allowStacking = plugin.getConfig().getBoolean("carry_on.allow_stacking", false);
        this.preventContainerStacking = plugin.getConfig().getBoolean("carry_on.prevent_container_stacking", true);
        this.storedBlockKey = new NamespacedKey(plugin, "stored_block");
        this.chestIdKey = new NamespacedKey(plugin, "chest_id");

        setupIntegrations(plugin);
    }

    private void setupIntegrations(JavaPlugin plugin) {
        // Initiation WorldGuard
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            TextHandler.get().logTranslated("plugin.integration.worldguard_detected");
            wgIntegration = new WorldGuardIntegrationHandler(plugin);
        }

        // Initiation Lands
        if (plugin.getServer().getPluginManager().getPlugin("Lands") != null) {
            TextHandler.get().logTranslated("plugin.integration.lands_detected");
            landsIntegration = new LandsIntegrationHandler(plugin);
        }

        // Initiation Lootin
        if (plugin.getServer().getPluginManager().getPlugin("Lootin") != null) {
            TextHandler.get().logTranslated("plugin.integration.lootin_detected");
            lootinIntegration = new LootinIntegrationHandler(plugin);
        }

        // Initiation GriefPrevention
        if (plugin.getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            TextHandler.get().logTranslated("plugin.integration.griefprevention_detected");
            try {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (me.ryanhamshire.GriefPrevention.GriefPrevention.instance != null) {
                        gpIntegration = new GriefPreventionIntegrationHandler(plugin);
                        TextHandler.get().logTranslated("plugin.integration.griefprevention_integration_success");
                    } else {
                        TextHandler.get().logTranslated("plugin.integration.griefprevention_integration_failed");
                    }
                }, 20L);
            } catch (Exception e) {
                TextHandler.get().logTranslated("plugin.integration.griefprevention_integration_failed", e.getMessage());
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity().getScoreboardTags().contains("being_carried")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPickup(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.carry_on") ||
                !player.hasPermission("vitamin.module.carry_on.entity") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.carry_on")) {
            return;
        }

        if (!player.getPassengers().isEmpty()) {
            releaseEntity(player);
            event.setCancelled(true);
            return;
        }

        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame) return;

        if (!player.isSneaking() ||
                !player.getInventory().getItemInMainHand().getType().isAir() ||
                !player.getInventory().getItemInOffHand().getType().isAir()) {
            return;
        }

        if (entity instanceof Player) {
            boolean allowPickupPlayers = plugin.getConfig().getBoolean("carry_on.allow_player_pickup", false);

            if (!allowPickupPlayers || !player.hasPermission("vitamin.module.carry_on.entity.player")) {
                TextHandler.get().sendMessage(player, "carry_on.cannot_pickup_players");
                event.setCancelled(true);
                return;
            }

            if (!allowStacking) {
                if (!player.getPassengers().isEmpty()) {
                    TextHandler.get().sendMessage(player, "carry_on.cannot_carry_while_carrying");
                    event.setCancelled(true);
                    return;
                }
                if (player.getVehicle() != null) {
                    TextHandler.get().sendMessage(player, "carry_on.cannot_carry_while_being_carried");
                    event.setCancelled(true);
                    return;
                }
                if (!entity.getPassengers().isEmpty()) {
                    TextHandler.get().sendMessage(player, "carry_on.cannot_carry_someone_carrying");
                    event.setCancelled(true);
                    return;
                }
                if (entity.getVehicle() != null) {
                    TextHandler.get().sendMessage(player, "carry_on.cannot_carry_someone_being_carried");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (!(entity instanceof LivingEntity livingEntity)) {
            TextHandler.get().sendMessage(player, "carry_on.cannot_pickup_entity");
            event.setCancelled(true);
            return;
        }

        if (entity.getScoreboardTags().contains("being_carried")) {
            TextHandler.get().sendMessage(player, "carry_on.cannot_pickup_entity");
            event.setCancelled(true);
            return;
        }

        AttributeInstance maxHPInstance = livingEntity.getAttribute(Vitamin.getInstance().getVersionAdapter().getMaxHPAttribute());
        if (maxHPInstance == null) {
            event.setCancelled(true);
            return;
        }

        double entityWeight = maxHPInstance.getBaseValue() * 2;
        if (entityWeight > maxCarryWeight) {
            TextHandler.get().sendMessage(player, "carry_on.entity_too_heavy");
            event.setCancelled(true);
            return;
        }

        if (wgIntegration != null) {
            if (!wgIntegration.canInteractEntity(player, entity.getLocation())) {
                TextHandler.get().sendMessage(player, "carry_on.no_permissions");
                event.setCancelled(true);
                return;
            }
        }

        if (landsIntegration != null) {
            if (!landsIntegration.canInteract(player, entity.getLocation())) {
                TextHandler.get().sendMessage(player, "carry_on.no_permissions");
                event.setCancelled(true);
                return;
            }
        }

        if (gpIntegration != null) {
            try {
                if (!gpIntegration.canInteract(player, entity.getLocation(), event)) {
                    TextHandler.get().sendMessage(player, "carry_on.no_permissions");
                    event.setCancelled(true);
                    return;
                }
            } catch (Exception e) {
                TextHandler.get().logTranslated("plugin.integration.griefprevention_error", e.getMessage());
                event.setCancelled(true);
                return;
            }
        }

        int slownessLevel = (int) Math.ceil(entityWeight / 15.0);
        applySlowness(player, slownessLevel);

        player.addPassenger(entity);
        entity.setGravity(false);
        entity.addScoreboardTag("being_carried");

        if (entity instanceof Monster monster) {
            monster.setAI(false);
        }

        TextHandler.get().sendMessage(player, "carry_on.picked_up_entity", entityWeight, maxCarryWeight);
        LogUtils.logEntityPickup(player.getName(), entity.getType().name(), entity.getLocation());
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPickup(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.carry_on") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.carry_on")) {
            return;
        }

        if (!player.isSneaking() || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (!player.getInventory().getItemInMainHand().getType().isAir() ||
                !player.getInventory().getItemInOffHand().getType().isAir()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container)) return;

        String containerType = block.getType().name().toLowerCase();
        String specificPerm = "vitamin.module.carry_on.container." + containerType;
        if (!player.hasPermission(specificPerm)) {
            TextHandler.get().sendMessage(player, "carry_on.no_permission_" + containerType);
            event.setCancelled(true);
            return;
        }

        if (lootinIntegration != null && lootinIntegration.isLootinContainer(block.getState())) {
            boolean allow = plugin.getConfig().getBoolean("carry_on.allow_lootin_pickup", false);
            if (!allow) {
                TextHandler.get().sendMessage(player, "carry_on.no_lootin_pickup");
                event.setCancelled(true);
                return;
            }
        }

        // WorldGuard
        if (wgIntegration != null && !wgIntegration.canInteractContainer(player, block.getLocation())) {
            TextHandler.get().sendMessage(player, "carry_on.no_permissions");
            event.setCancelled(true);
            return;
        }

        // Lands
        if (landsIntegration != null && !landsIntegration.canBreak(player, block.getLocation(), block.getType())) {
            TextHandler.get().sendMessage(player, "carry_on.no_permissions");
            event.setCancelled(true);
            return;
        }

        // GriefPrevention
        if (gpIntegration != null) {
            try {
                if (!gpIntegration.hasContainerPermissions(player, block.getLocation(), event)) {
                    TextHandler.get().sendMessage(player, "carry_on.no_permissions");
                    event.setCancelled(true);
                    return;
                }
            } catch (Exception e) {
                TextHandler.get().logTranslated("plugin.integration.griefprevention_error", e.getMessage());
                event.setCancelled(true);
                return;
            }
        }

        if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
            pickupChestAsIndividual(player, block);
        } else {
            pickupSingleContainer(player, block);
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockDrop(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.carry_on") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.carry_on")) {
            return;
        }

        // entity drop
        if (!player.getPassengers().isEmpty() &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            releaseEntity(player);
            event.setCancelled(true);
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.hasItemMeta()) return;
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!meta.getPersistentDataContainer().has(storedBlockKey, PersistentDataType.STRING)) return;

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !targetBlock.getType().isAir()) return;

        // WorldGuard
        if (wgIntegration != null && !wgIntegration.canInteractContainer(player, targetBlock.getLocation())) {
            TextHandler.get().sendMessage(player, "carry_on.no_permissions");
            event.setCancelled(true);
            return;
        }

        // Lands temporary block break check
        Material blockType = Material.valueOf(
                meta.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING)
        );
        if (landsIntegration != null && !landsIntegration.canBreak(player, targetBlock.getLocation(), blockType)) {
            TextHandler.get().sendMessage(player, "carry_on.no_permissions");
            event.setCancelled(true);
            return;
        }

        // GriefPrevention
        if (gpIntegration != null) {
            try {
                if (!gpIntegration.hasContainerPermissions(player, targetBlock.getLocation(), event)) {
                    TextHandler.get().sendMessage(player, "carry_on.no_permissions");
                    event.setCancelled(true);
                    return;
                }
            } catch (Exception e) {
                TextHandler.get().logTranslated("plugin.integration.griefprevention_error", e.getMessage());
                event.setCancelled(true);
                return;
            }
        }

        if (blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST) {
            if (meta.getPersistentDataContainer().has(chestIdKey, PersistentDataType.STRING)) {
                placeChest(player, targetBlock, meta);
            } else {
                placeSingleContainer(player, targetBlock, meta);
            }
        } else {
            placeSingleContainer(player, targetBlock, meta);
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!player.getPassengers().isEmpty()) {
            releaseEntity(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!player.getPassengers().isEmpty()) {
            releaseEntity(player);
        }
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        Entity dismounted = event.getDismounted();
        Entity entity = event.getEntity();

        if (!(dismounted instanceof Player player) || !(entity instanceof Player mountedPlayer)) {
            return;
        }

        if (mountedPlayer.getScoreboardTags().contains("being_carried")) {
            mountedPlayer.setGravity(true);
            mountedPlayer.removeScoreboardTag("being_carried");
            player.removePassenger(mountedPlayer);
            mountedPlayer.teleport(player.getLocation().add(0, 0.5, 0));
            removeSlowness(player);
            TextHandler.get().sendMessage(player, "carry_on.entity_dropped");
            TextHandler.get().sendMessage(mountedPlayer, "carry_on.you_dismounted");
        }
    }

    private void pickupSingleContainer(Player player, Block block) {
        Container container = (Container) block.getState();
        if (preventContainerStacking && !player.hasPermission("vitamin.module.carry_on.stacking_bypass") && hasNestedContainer(container.getInventory())) {
            TextHandler.get().sendMessage(player, "carry_on.no_stacking_allowed");
            return;
        }

        UUID containerId = UUID.randomUUID();
        saveContainerBackup(containerId, player, block);
        saveChestContents(containerId, container.getInventory());

        ItemStack blockItem = new ItemStack(block.getType());
        if (!(blockItem.getItemMeta() instanceof BlockStateMeta meta)) return;

        Container tempContainer = (Container) block.getState();
        meta.setBlockState(tempContainer);
        meta.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, block.getType().name());
        meta.getPersistentDataContainer().set(chestIdKey, PersistentDataType.STRING, containerId.toString());
        blockItem.setItemMeta(meta);

        String containerType = block.getType().name();
        block.setType(Material.AIR);
        player.getInventory().addItem(blockItem);
        TextHandler.get().sendMessage(player, "carry_on.picked_up_block",
                block.getType().name().toLowerCase().replace("_", " "));
        LogUtils.logContainerPickup(
                player.getName(),
                containerType,
                containerId.toString(),
                block.getLocation()
        );
    }

    private void pickupChestAsIndividual(Player player, Block block) {
        Material originalType = block.getType();
        Container originalState = (Container) block.getState();
        Inventory chestInventory = originalState.getInventory();

        boolean isDouble = isPartOfDoubleChest(block);
        Block partner = null;
        ItemStack[] otherContents = null;
        ItemStack[] thisContents;

        if (isDouble) {
            partner = findCorrectDoubleChestPartner(block);
            if (partner != null) {
                DoubleChestInventory doubleInv = (DoubleChestInventory) chestInventory;
                thisContents = extractIndividualChestContents(block, doubleInv);
                otherContents = extractIndividualChestContents(partner, doubleInv);
            } else {
                thisContents = chestInventory.getContents();
            }
        } else {
            thisContents = chestInventory.getContents();
        }

        if (preventContainerStacking && !player.hasPermission("vitamin.module.carry_on.stacking_bypass") && hasNestedContainer(thisContents)) {
            TextHandler.get().sendMessage(player, "carry_on.no_stacking_allowed");
            return;
        }

        UUID chestId = UUID.randomUUID();
        saveContainerBackup(chestId, player, block);

        ItemStack blockItem = new ItemStack(originalType);
        if (!(blockItem.getItemMeta() instanceof BlockStateMeta meta)) return;

        meta.setBlockState(originalState);
        meta.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, originalType.name());
        meta.getPersistentDataContainer().set(chestIdKey, PersistentDataType.STRING, chestId.toString());
        blockItem.setItemMeta(meta);

        String containerType = originalType.name();

        if (isDouble && partner != null) {
            final Block finalPartner = partner;
            final ItemStack[] finalOtherContents = otherContents;
            saveChestContents(chestId, thisContents);
            block.setType(Material.AIR);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (finalPartner.getType() == originalType) {
                    Container otherContainer = (Container) finalPartner.getState();
                    Inventory otherInventory = otherContainer.getInventory();
                    otherInventory.clear();
                    for (int i = 0; i < Math.min(27, finalOtherContents.length); i++) {
                        if (finalOtherContents[i] != null) {
                            otherInventory.setItem(i, finalOtherContents[i]);
                        }
                    }
                    otherContainer.update();
                }
            }, 2L);
        } else {
            saveChestContents(chestId, chestInventory);
            block.setType(Material.AIR);
        }

        player.getInventory().addItem(blockItem);
        TextHandler.get().sendMessage(player, "carry_on.picked_up_chest");
        LogUtils.logContainerPickup(
                player.getName(),
                containerType,
                chestId.toString(),
                block.getLocation()
        );
    }

    private void saveContainerBackup(UUID containerId, Player player, Block block) {
        String sql = """
        INSERT INTO container_backups\s
        (chest_id, player_uuid, player_name, container_type, pickup_timestamp, world_name, x_coord, y_coord, z_coord)\s
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
       \s""";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, containerId.toString());
            ps.setString(2, player.getUniqueId().toString());
            ps.setString(3, player.getName());
            ps.setString(4, block.getType().name());
            ps.setLong(5, System.currentTimeMillis());
            ps.setString(6, block.getWorld().getName());
            ps.setInt(7, block.getX());
            ps.setInt(8, block.getY());
            ps.setInt(9, block.getZ());

            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.backup.save_error", e);
        }
    }

    private boolean isPartOfDoubleChest(Block block) {
        Chest chestData = (Chest) block.getBlockData();
        return chestData.getType() != Chest.Type.SINGLE;
    }

    private Block findCorrectDoubleChestPartner(Block block) {
        if (!isPartOfDoubleChest(block)) {
            return null;
        }

        Container container = (Container) block.getState();
        Inventory blockInventory = container.getInventory();

        // Just check the four cardinal directions
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);

            // Must be the same type of block
            if (relative.getType() != block.getType()) {
                continue;
            }

            // Must be a part of a double chest
            if (!isPartOfDoubleChest(relative)) {
                continue;
            }

            // Verification that they have the SAME inventory
            Container relativeContainer = (Container) relative.getState();
            Inventory relativeInventory = relativeContainer.getInventory();

            // KEY: Double chests have the same inventory
            if (blockInventory == relativeInventory && blockInventory instanceof DoubleChestInventory) {
                Chest blockChestData = (Chest) block.getBlockData();
                Chest relativeChestData = (Chest) relative.getBlockData();

                boolean validPair =
                        (blockChestData.getType() == Chest.Type.LEFT && relativeChestData.getType() == Chest.Type.RIGHT) ||
                                (blockChestData.getType() == Chest.Type.RIGHT && relativeChestData.getType() == Chest.Type.LEFT);

                if (validPair) {
                    return relative;
                }
            }
        }

        return null;
    }

    private ItemStack[] extractIndividualChestContents(Block block, DoubleChestInventory doubleChestInventory) {
        ItemStack[] contents = new ItemStack[27];

        Chest chestData = (Chest) block.getBlockData();

        // DoubleChestInventory: slots 0-26 = left, slots 27-53 = right
        int startSlot = 0;
        if (chestData.getType() == Chest.Type.RIGHT) {
            startSlot = 27;
        }

        for (int i = 0; i < 27; i++) {
            ItemStack item = doubleChestInventory.getItem(startSlot + i);
            contents[i] = item != null ? item.clone() : null;
        }

        return contents;
    }

    private void placeSingleContainer(Player player, Block block, BlockStateMeta meta) {
        Material blockType = Material.valueOf(
                meta.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING)
        );
        block.setType(blockType);
        Container container = (Container) block.getState();
        container.setBlockData(meta.getBlockState().getBlockData());
        container.update(true, false);

        String chestIdStr = meta.getPersistentDataContainer().get(chestIdKey, PersistentDataType.STRING);
        if (chestIdStr != null) {
            UUID chestId = UUID.fromString(chestIdStr);
            Inventory containerInventory = container.getInventory();
            loadChestContents(chestId, containerInventory);
        }

        container.update(true, false);
        TextHandler.get().sendMessage(player, "carry_on.placed_block");
        consumeItemInHand(player);
    }

    private void placeChest(Player player, Block block, BlockStateMeta meta) {
        Material blockType = Material.valueOf(
                meta.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING)
        );

        block.setType(blockType);
        Chest chestData = (Chest) block.getBlockData();
        chestData.setType(Chest.Type.SINGLE);
        block.setBlockData(chestData);

        Container container = (Container) block.getState();
        container.update(true, false);

        String chestIdStr = meta.getPersistentDataContainer().get(chestIdKey, PersistentDataType.STRING);
        if (chestIdStr != null) {
            UUID chestId = UUID.fromString(chestIdStr);
            Inventory chestInventory = container.getInventory();
            loadChestContents(chestId, chestInventory);
        }

        container.update(true, false);
        TextHandler.get().sendMessage(player, "carry_on.placed_chest");
        consumeItemInHand(player);
    }

    private void consumeItemInHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private void releaseEntity(Player player) {
        if (player.getPassengers().isEmpty()) return;
        player.getPassengers().forEach(entity -> {
            entity.setGravity(true);
            entity.removeScoreboardTag("being_carried");
            if (entity instanceof Monster monster) {
                monster.setAI(true);
            }
            if (entity instanceof Player mountedPlayer) {
                mountedPlayer.setGravity(true);
            }
            player.removePassenger(entity);
            entity.teleport(player.getLocation().add(0, 0.5, 0));
            Bukkit.getScheduler().runTaskLater(plugin, entity::eject, 1L);
            LogUtils.logEntityDrop(player.getName(), entity.getType().name(), entity.getLocation());
        });
        removeSlowness(player);
        TextHandler.get().sendMessage(player, "carry_on.entity_dropped");
    }

    private void applySlowness(Player player, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, level - 1, false, false));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getPassengers().isEmpty()) {
                    removeSlowness(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void removeSlowness(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    private void saveChestContents(UUID chestId, Inventory chestInventory) {
        String sql = "INSERT INTO chest_contents (chest_id, slot, item_data) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int slot = 0; slot < chestInventory.getSize(); slot++) {
                ItemStack item = chestInventory.getItem(slot);
                if (item != null) {
                    String itemData = serializeItemStack(item);
                    ps.setString(1, chestId.toString());
                    ps.setInt(2, slot);
                    ps.setString(3, itemData);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.chest.save_error", e);
        }
    }

    private void saveChestContents(UUID chestId, ItemStack[] contents) {
        String sql = "INSERT INTO chest_contents (chest_id, slot, item_data) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack item = contents[slot];
                if (item != null) {
                    String itemData = serializeItemStack(item);
                    ps.setString(1, chestId.toString());
                    ps.setInt(2, slot);
                    ps.setString(3, itemData);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.chest.save_error", e);
        }
    }

    private void loadChestContents(UUID chestId, Inventory chestInventory) {
        String sql = "SELECT slot, item_data FROM chest_contents WHERE chest_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chestId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int slot = rs.getInt("slot");
                    String itemData = rs.getString("item_data");
                    ItemStack item = deserializeItemStack(itemData);
                    if (slot < chestInventory.getSize()) {
                        chestInventory.setItem(slot, item);
                    }
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.chest.load_error", e);
        }
    }

    private String serializeItemStack(ItemStack item) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        return config.saveToString();
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

    private boolean hasNestedContainer(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta meta &&
                    meta.getPersistentDataContainer().has(storedBlockKey, PersistentDataType.STRING)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNestedContainer(ItemStack[] contents) {
        for (ItemStack item : contents) {
            if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta meta &&
                    meta.getPersistentDataContainer().has(storedBlockKey, PersistentDataType.STRING)) {
                return true;
            }
        }
        return false;
    }
}