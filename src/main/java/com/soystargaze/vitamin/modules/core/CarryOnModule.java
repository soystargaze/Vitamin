package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.integration.GriefPreventionIntegrationHandler;
import com.soystargaze.vitamin.integration.LandsIntegrationHandler;
import com.soystargaze.vitamin.integration.LootinIntegrationHandler;
import com.soystargaze.vitamin.integration.WorldGuardIntegrationHandler;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class CarryOnModule implements Listener {

    private final JavaPlugin plugin;
    private final double maxCarryWeight;
    private final NamespacedKey storedBlockKey;
    private final NamespacedKey chestPartKey;
    private final NamespacedKey chestIdKey;
    private WorldGuardIntegrationHandler wgIntegration;
    private LandsIntegrationHandler landsIntegration;
    private LootinIntegrationHandler lootinIntegration;
    private final boolean allowStacking;
    private GriefPreventionIntegrationHandler gpIntegration;

    public CarryOnModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.maxCarryWeight = plugin.getConfig().getDouble("carry_on.max_weight", 100.0);
        this.allowStacking = plugin.getConfig().getBoolean("carry_on.allow_stacking", false);
        this.storedBlockKey = new NamespacedKey(plugin, "stored_block");
        this.chestPartKey = new NamespacedKey(plugin, "chest_part");
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
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPickup(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.carry_on") ||
                !player.hasPermission("vitamin.module.carry_on.chest") ||
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

        if (lootinIntegration != null && lootinIntegration.isLootinContainer(block.getState())) {
            boolean allow = plugin.getConfig().getBoolean("carry_on.allow_lootin_pickup", false);
            if (!allow) {
                TextHandler.get().sendMessage(player, "carry_on.no_lootin_pickup");
                event.setCancelled(true);
                return;
            }
        }

        if (wgIntegration != null && !wgIntegration.canInteractContainer(player, block.getLocation())) {
            TextHandler.get().sendMessage(player, "carry_on.no_permissions");
            event.setCancelled(true);
            return;
        }

        if (landsIntegration != null) {
            if (!landsIntegration.canBreak(player, block.getLocation(), block.getType())) {
                TextHandler.get().sendMessage(player, "carry_on.no_permissions");
                event.setCancelled(true);
                return;
            }
        }

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
            Chest chestData = (Chest) block.getBlockData();
            if (chestData.getType() == Chest.Type.SINGLE) {
                pickupSingleChest(player, block);
            } else {
                pickupDoubleChest(player, block);
            }
        } else {
            pickupSingleContainer(player, block);
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityOrBlockDrop(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.carry_on") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.carry_on")) {
            return;
        }

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

        Material blockType = Material.valueOf(
                meta.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING)
        );

        // Lands temporary block break check
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

        String chestPart = meta.getPersistentDataContainer().get(chestPartKey, PersistentDataType.STRING);

        if ((blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST) && chestPart != null && !chestPart.equals("SINGLE")) {
            Block otherHalfLocation = null;
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adjacent = targetBlock.getRelative(face);
                if (adjacent.getType().isAir()) {
                    otherHalfLocation = adjacent;
                    break;
                }
            }

            if (otherHalfLocation != null) {
                // WorldGuard
                if (wgIntegration != null && !wgIntegration.canInteractContainer(player, otherHalfLocation.getLocation())) {
                    TextHandler.get().sendMessage(player, "carry_on.no_permissions_double_chest");
                    event.setCancelled(true);
                    return;
                }

                // Lands temporally block break check
                if (landsIntegration != null && !landsIntegration.canBreak(player, otherHalfLocation.getLocation(), blockType)) {
                    TextHandler.get().sendMessage(player, "carry_on.no_permissions_double_chest");
                    event.setCancelled(true);
                    return;
                }

                // GriefPrevention
                if (gpIntegration != null && !gpIntegration.hasContainerPermissions(player, otherHalfLocation.getLocation(), event)) {
                    TextHandler.get().sendMessage(player, "carry_on.no_permissions_double_chest");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if ((blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST) && chestPart != null) {
            if (chestPart.equals("SINGLE")) {
                if (canPlaceChestAt(targetBlock, blockType)) {
                    placeSingleChest(player, targetBlock, meta);
                } else {
                    TextHandler.get().sendMessage(player, "carry_on.cannot_place_chest");
                }
            } else {
                if (tryPlaceDoubleChest(player, targetBlock, meta, chestPart)) {
                    TextHandler.get().sendMessage(player, "carry_on.placed_double_chest");
                }
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

    private boolean canPlaceChestAt(Block targetBlock, Material ignored) {
        return targetBlock.getType().isAir();
    }

    private void pickupSingleContainer(Player player, Block block) {
        ItemStack blockItem = new ItemStack(block.getType());
        if (!(blockItem.getItemMeta() instanceof BlockStateMeta meta)) return;
        Container container = (Container) block.getState();
        meta.setBlockState(container);
        meta.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, block.getType().name());
        blockItem.setItemMeta(meta);
        block.setType(Material.AIR);
        player.getInventory().addItem(blockItem);
        TextHandler.get().sendMessage(player, "carry_on.picked_up_block",
                block.getType().name().toLowerCase().replace("_", " "));
    }

    private void pickupSingleChest(Player player, Block block) {
        UUID chestId = UUID.randomUUID();
        Container container = (Container) block.getState();
        Inventory chestInventory = container.getInventory();
        saveChestContents(chestId, chestInventory);

        ItemStack blockItem = new ItemStack(block.getType());
        if (!(blockItem.getItemMeta() instanceof BlockStateMeta meta)) return;
        meta.setBlockState(container);
        meta.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, block.getType().name());
        meta.getPersistentDataContainer().set(chestPartKey, PersistentDataType.STRING, "SINGLE");
        meta.getPersistentDataContainer().set(chestIdKey, PersistentDataType.STRING, chestId.toString());
        blockItem.setItemMeta(meta);

        chestInventory.clear();
        block.setType(Material.AIR);

        player.getInventory().addItem(blockItem);
        TextHandler.get().sendMessage(player, "carry_on.picked_up_block",
                block.getType().name().toLowerCase().replace("_", " "));
    }

    private void pickupDoubleChest(Player player, Block block) {
        Block otherHalf = getConnectedChestBlock(block);
        if (otherHalf == null) return;

        UUID chestId = UUID.randomUUID();
        Container firstContainer = (Container) block.getState();
        Container secondContainer = (Container) otherHalf.getState();
        Inventory firstInventory = firstContainer.getInventory();
        ItemStack[] combinedContents = new ItemStack[54];
        System.arraycopy(firstInventory.getContents(), 0, combinedContents, 0, 27);
        System.arraycopy(secondContainer.getInventory().getContents(), 0, combinedContents, 27, 27);
        saveChestContents(chestId, combinedContents);

        ItemStack firstHalf = new ItemStack(block.getType());
        if (!(firstHalf.getItemMeta() instanceof BlockStateMeta metaFirst)) return;
        metaFirst.setBlockState(firstContainer);
        metaFirst.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, block.getType().name());
        metaFirst.getPersistentDataContainer().set(chestPartKey, PersistentDataType.STRING, "LEFT");
        metaFirst.getPersistentDataContainer().set(chestIdKey, PersistentDataType.STRING, chestId.toString());
        firstHalf.setItemMeta(metaFirst);

        ItemStack secondHalf = new ItemStack(block.getType());
        if (!(secondHalf.getItemMeta() instanceof BlockStateMeta metaSecond)) return;
        metaSecond.setBlockState(secondContainer);
        metaSecond.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, block.getType().name());
        metaSecond.getPersistentDataContainer().set(chestPartKey, PersistentDataType.STRING, "RIGHT");
        metaSecond.getPersistentDataContainer().set(chestIdKey, PersistentDataType.STRING, chestId.toString());
        secondHalf.setItemMeta(metaSecond);

        firstInventory.clear();
        secondContainer.getInventory().clear();
        block.setType(Material.AIR);
        otherHalf.setType(Material.AIR);

        player.getInventory().addItem(firstHalf, secondHalf);
        TextHandler.get().sendMessage(player, "carry_on.picked_up_double_chest");
    }

    private Block getConnectedChestBlock(Block block) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (relative.getType() == block.getType()) {
                Chest relativeData = (Chest) relative.getBlockData();
                if (relativeData.getType() != Chest.Type.SINGLE) {
                    return relative;
                }
            }
        }
        return null;
    }

    private void placeSingleContainer(Player player, Block block, BlockStateMeta meta) {
        Material blockType = Material.valueOf(
                meta.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING)
        );
        block.setType(blockType);
        Container container = (Container) block.getState();
        container.setBlockData(meta.getBlockState().getBlockData());
        container.update(true, false);
        TextHandler.get().sendMessage(player, "carry_on.placed_block");
        consumeItemInHand(player);
    }

    private void placeSingleChest(Player player, Block block, BlockStateMeta meta) {
        Material blockType = Material.valueOf(
                meta.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING)
        );
        block.setType(blockType);
        Container container = (Container) block.getState();
        container.setBlockData(meta.getBlockState().getBlockData());

        String chestIdStr = meta.getPersistentDataContainer().get(chestIdKey, PersistentDataType.STRING);
        if (chestIdStr == null) return;
        UUID chestId = UUID.fromString(chestIdStr);

        Inventory chestInventory = container.getInventory();
        loadChestContents(chestId, chestInventory);
        deleteChestContents(chestId);

        container.update(true, false);
        TextHandler.get().sendMessage(player, "carry_on.placed_block");
        consumeItemInHand(player);
    }

    private boolean tryPlaceDoubleChest(Player player, Block block, BlockStateMeta meta, String chestPart) {
        Material blockType = Material.valueOf(
                meta.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING)
        );
        String otherPart = chestPart.equals("LEFT") ? "RIGHT" : "LEFT";
        ItemStack otherChestItem = null;
        int otherChestSlot = -1;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem != null && invItem.getType() == blockType && invItem.hasItemMeta()) {
                if (!(invItem.getItemMeta() instanceof BlockStateMeta itemMeta)) continue;
                if (itemMeta.getPersistentDataContainer().has(chestPartKey, PersistentDataType.STRING) &&
                        Objects.equals(itemMeta.getPersistentDataContainer().get(chestPartKey, PersistentDataType.STRING), otherPart)) {
                    otherChestItem = invItem;
                    otherChestSlot = i;
                    break;
                }
            }
        }

        if (otherChestItem == null) {
            TextHandler.get().sendMessage(player, "carry_on.need_both_chest_parts");
            return false;
        }

        Block otherBlock = null;
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block adjacent = block.getRelative(face);
            if (adjacent.getType().isAir()) {
                otherBlock = adjacent;
                break;
            }
        }

        if (otherBlock == null) {
            TextHandler.get().sendMessage(player, "carry_on.no_space_for_double_chest");
            return false;
        }

        block.setType(blockType);
        otherBlock.setType(blockType);
        Container firstContainer = (Container) block.getState();
        Container secondContainer = (Container) otherBlock.getState();
        Chest firstChestData = (Chest) block.getBlockData();
        Chest secondChestData = (Chest) otherBlock.getBlockData();
        firstChestData.setType(chestPart.equals("LEFT") ? Chest.Type.LEFT : Chest.Type.RIGHT);
        secondChestData.setType(chestPart.equals("LEFT") ? Chest.Type.RIGHT : Chest.Type.LEFT);
        block.setBlockData(firstChestData);
        otherBlock.setBlockData(secondChestData);

        String chestIdStr = meta.getPersistentDataContainer().get(chestIdKey, PersistentDataType.STRING);
        if (chestIdStr == null) return false;
        UUID chestId = UUID.fromString(chestIdStr);

        ItemStack[] combinedContents = new ItemStack[54];
        loadChestContents(chestId, combinedContents);

        Inventory firstInventory = firstContainer.getInventory();
        Inventory secondInventory = secondContainer.getInventory();

        firstInventory.setContents(Arrays.copyOfRange(combinedContents, 0, 27));
        secondInventory.setContents(Arrays.copyOfRange(combinedContents, 27, 54));

        deleteChestContents(chestId);

        firstContainer.update(true, false);
        secondContainer.update(true, false);
        consumeItemInHand(player);
        player.getInventory().setItem(otherChestSlot, null);
        return true;
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
                    chestInventory.setItem(slot, item);
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.chest.load_error", e);
        }
    }

    private void loadChestContents(UUID chestId, ItemStack[] contents) {
        String sql = "SELECT slot, item_data FROM chest_contents WHERE chest_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chestId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int slot = rs.getInt("slot");
                    String itemData = rs.getString("item_data");
                    ItemStack item = deserializeItemStack(itemData);
                    if (slot < contents.length) {
                        contents[slot] = item;
                    }
                }
            }
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.chest.load_error", e);
        }
    }

    private void deleteChestContents(UUID chestId) {
        String sql = "DELETE FROM chest_contents WHERE chest_id = ?";
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chestId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            TextHandler.get().logTranslated("database.chest.delete_error", e);
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
}