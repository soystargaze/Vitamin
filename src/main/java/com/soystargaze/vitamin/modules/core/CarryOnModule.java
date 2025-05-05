package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.integration.LandsIntegrationHandler;
import com.soystargaze.vitamin.integration.WorldGuardIntegrationHandler;

import com.soystargaze.vitamin.database.DatabaseHandler;

import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Chest;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CarryOnModule implements Listener {

    private final JavaPlugin plugin;
    private final double maxCarryWeight;
    private final NamespacedKey storedBlockKey;
    private final NamespacedKey chestPartKey;
    private final Map<String, ItemStack[]> storedChestContents = new HashMap<>();
    private WorldGuardIntegrationHandler wgIntegration;
    private LandsIntegrationHandler landsIntegration;

    public CarryOnModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.maxCarryWeight = plugin.getConfig().getDouble("carry_on.max_weight", 100.0);
        this.storedBlockKey = new NamespacedKey(plugin, "stored_block");
        this.chestPartKey = new NamespacedKey(plugin, "chest_part");

        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            wgIntegration = new WorldGuardIntegrationHandler(plugin);
        }
        if (plugin.getServer().getPluginManager().getPlugin("Lands") != null) {
            landsIntegration = new LandsIntegrationHandler(plugin);
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
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.carry_on")) {
            return;
        }

        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame) return;

        if (!player.isSneaking() ||
                !player.getInventory().getItemInMainHand().getType().isAir() ||
                !player.getInventory().getItemInOffHand().getType().isAir()) {
            return;
        }

        if (!player.getPassengers().isEmpty()) {
            return;
        }

        if (entity instanceof Player) {
            TextHandler.get().sendMessage(player, "carry_on.cannot_pickup_players");
            event.setCancelled(true);
            return;
        }

        if (!(entity instanceof LivingEntity livingEntity)) {
            TextHandler.get().sendMessage(player, "carry_on.cannot_pickup_entity");
            event.setCancelled(true);
            return;
        }

        if (!entity.getScoreboardTags().isEmpty() && !entity.getScoreboardTags().contains("being_carried")) {
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

        // Integration with WorldGuard
        if (wgIntegration != null) {
            if (!wgIntegration.canInteract(player, entity.getLocation())) {
                TextHandler.get().sendMessage(player, "carry_on.no_permissions");
                event.setCancelled(true);
                return;
            }
        }

        // Integration with Lands
        if (landsIntegration != null) {
            if (!landsIntegration.canInteract(player, entity.getLocation())) {
                TextHandler.get().sendMessage(player, "carry_on.no_permissions");
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

        // Integration with WorldGuard
        if (wgIntegration != null) {
            if (!wgIntegration.canBuild(player, block.getLocation())) {
                TextHandler.get().sendMessage(player, "carry_on.no_permissions");
                event.setCancelled(true);
                return;
            }
        }

        // Integration with Lands
        if (landsIntegration != null) {
            if (!landsIntegration.canBreak(player, block.getLocation(), block.getType())) {
                TextHandler.get().sendMessage(player, "carry_on.no_permissions");
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

        if (!player.getPassengers().isEmpty() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            releaseEntity(player);
            event.setCancelled(true);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.hasItemMeta()) return;
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!meta.getPersistentDataContainer().has(storedBlockKey, PersistentDataType.STRING)) return;
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !targetBlock.getType().isAir()) return;
        Material blockType = Material.valueOf(
                meta.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING)
        );
        String chestPart = meta.getPersistentDataContainer().get(chestPartKey, PersistentDataType.STRING);
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
        ItemStack blockItem = new ItemStack(block.getType());
        if (!(blockItem.getItemMeta() instanceof BlockStateMeta meta)) return;
        Container container = (Container) block.getState();
        meta.setBlockState(container);
        meta.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, block.getType().name());
        meta.getPersistentDataContainer().set(chestPartKey, PersistentDataType.STRING, "SINGLE");
        String chestId = block.getLocation().toString();
        storedChestContents.put(chestId, container.getInventory().getContents());
        container.getInventory().clear();
        blockItem.setItemMeta(meta);
        block.setType(Material.AIR);
        player.getInventory().addItem(blockItem);
        TextHandler.get().sendMessage(player, "carry_on.picked_up_block",
                block.getType().name().toLowerCase().replace("_", " "));
    }

    private void pickupDoubleChest(Player player, Block block) {
        Block otherHalf = getConnectedChestBlock(block);
        if (otherHalf == null) return;
        ItemStack firstHalf = new ItemStack(block.getType());
        if (!(firstHalf.getItemMeta() instanceof BlockStateMeta metaFirst)) return;
        Container firstContainer = (Container) block.getState();
        metaFirst.setBlockState(firstContainer);
        metaFirst.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, block.getType().name());
        metaFirst.getPersistentDataContainer().set(chestPartKey, PersistentDataType.STRING, "LEFT");
        ItemStack secondHalf = new ItemStack(block.getType());
        if (!(secondHalf.getItemMeta() instanceof BlockStateMeta metaSecond)) return;
        Container secondContainer = (Container) otherHalf.getState();
        metaSecond.setBlockState(secondContainer);
        metaSecond.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, block.getType().name());
        metaSecond.getPersistentDataContainer().set(chestPartKey, PersistentDataType.STRING, "RIGHT");
        String chestId = block.getLocation().toString();
        storedChestContents.put(chestId + "_left", firstContainer.getInventory().getContents());
        storedChestContents.put(chestId + "_right", secondContainer.getInventory().getContents());
        firstContainer.getInventory().clear();
        secondContainer.getInventory().clear();
        firstHalf.setItemMeta(metaFirst);
        secondHalf.setItemMeta(metaSecond);
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
        String chestId = block.getLocation().toString();
        ItemStack[] contents = storedChestContents.getOrDefault(chestId, new ItemStack[0]);
        container.getInventory().setContents(contents);
        container.update(true, false);
        storedChestContents.remove(chestId);
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
        String chestId = block.getLocation().toString();
        ItemStack[] firstContents = storedChestContents.getOrDefault(chestId + "_" + chestPart.toLowerCase(), new ItemStack[0]);
        ItemStack[] secondContents = storedChestContents.getOrDefault(chestId + "_" + otherPart.toLowerCase(), new ItemStack[0]);
        firstContainer.getInventory().setContents(firstContents);
        secondContainer.getInventory().setContents(secondContents);
        firstContainer.update(true, false);
        secondContainer.update(true, false);
        storedChestContents.remove(chestId + "_" + chestPart.toLowerCase());
        storedChestContents.remove(chestId + "_" + otherPart.toLowerCase());
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
}