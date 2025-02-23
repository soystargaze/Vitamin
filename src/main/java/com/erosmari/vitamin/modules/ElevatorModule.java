package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.database.DatabaseHandler;
import com.erosmari.vitamin.utils.TranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Particle;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;

public class ElevatorModule implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey keyElevator;
    private final Map<Material, Material> woolToShulkerMap = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> notifiedPlayers = new java.util.HashSet<>();

    private static final long COOLDOWN_MILLIS = 500;

    public ElevatorModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.keyElevator = new NamespacedKey(plugin, "elevator");
        setupWoolToShulkerMap();
        registerRecipe();
    }

    private void setupWoolToShulkerMap() {
        woolToShulkerMap.put(Material.WHITE_WOOL, Material.WHITE_SHULKER_BOX);
        woolToShulkerMap.put(Material.ORANGE_WOOL, Material.ORANGE_SHULKER_BOX);
        woolToShulkerMap.put(Material.MAGENTA_WOOL, Material.MAGENTA_SHULKER_BOX);
        woolToShulkerMap.put(Material.LIGHT_BLUE_WOOL, Material.LIGHT_BLUE_SHULKER_BOX);
        woolToShulkerMap.put(Material.YELLOW_WOOL, Material.YELLOW_SHULKER_BOX);
        woolToShulkerMap.put(Material.LIME_WOOL, Material.LIME_SHULKER_BOX);
        woolToShulkerMap.put(Material.PINK_WOOL, Material.PINK_SHULKER_BOX);
        woolToShulkerMap.put(Material.GRAY_WOOL, Material.GRAY_SHULKER_BOX);
        woolToShulkerMap.put(Material.LIGHT_GRAY_WOOL, Material.LIGHT_GRAY_SHULKER_BOX);
        woolToShulkerMap.put(Material.CYAN_WOOL, Material.CYAN_SHULKER_BOX);
        woolToShulkerMap.put(Material.PURPLE_WOOL, Material.PURPLE_SHULKER_BOX);
        woolToShulkerMap.put(Material.BLUE_WOOL, Material.BLUE_SHULKER_BOX);
        woolToShulkerMap.put(Material.BROWN_WOOL, Material.BROWN_SHULKER_BOX);
        woolToShulkerMap.put(Material.GREEN_WOOL, Material.GREEN_SHULKER_BOX);
        woolToShulkerMap.put(Material.RED_WOOL, Material.RED_SHULKER_BOX);
        woolToShulkerMap.put(Material.BLACK_WOOL, Material.BLACK_SHULKER_BOX);
    }

    private void registerRecipe() {
        woolToShulkerMap.forEach((woolColor, shulkerColor) -> {
            String colorName = woolColor.name().toLowerCase().replace("_wool", "");
            NamespacedKey recipeKey = new NamespacedKey(plugin, "elevator_" + colorName);

            Bukkit.removeRecipe(recipeKey);

            ItemStack elevator = createElevatorItem(shulkerColor);

            ShapedRecipe recipe = new ShapedRecipe(recipeKey, elevator);
            recipe.shape("LLL", "LEL", "LLL");
            recipe.setIngredient('L', woolColor);
            recipe.setIngredient('E', Material.ENDER_PEARL);

            Bukkit.addRecipe(recipe);
        });
    }

    private ItemStack createElevatorItem(Material shulkerColor) {
        ItemStack elevator = new ItemStack(shulkerColor);
        BlockStateMeta meta = (BlockStateMeta) elevator.getItemMeta();
        if (meta != null) {
            meta.displayName(TranslationHandler.getComponent("elevator.item_name"));
            meta.getPersistentDataContainer().set(keyElevator, PersistentDataType.BYTE, (byte) 1);

            ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
            shulkerBox.getPersistentDataContainer().set(keyElevator, PersistentDataType.BYTE, (byte) 1);
            meta.setBlockState(shulkerBox);

            elevator.setItemMeta(meta);
        }
        return elevator;
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.elevator") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elevator")) {
            return;
        }
        teleportElevator(player, 1);
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.elevator") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elevator")) {
            return;
        }
        if (event.isSneaking()) {
            teleportElevator(player, -1);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.elevator") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elevator")) {
            return;
        }
        ItemStack item = event.getItemInHand();
        Block block = event.getBlock();

        if (woolToShulkerMap.containsValue(block.getType())) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            if (meta.getPersistentDataContainer().has(keyElevator, PersistentDataType.BYTE)) {
                if (block.getState() instanceof ShulkerBox shulkerBox) {
                    shulkerBox.getPersistentDataContainer().set(keyElevator, PersistentDataType.BYTE, (byte) 1);
                    shulkerBox.update();
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.elevator") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elevator")) {
            return;
        }
        Block block = event.getBlock();

        if (woolToShulkerMap.containsValue(block.getType())) {
            if (block.getState() instanceof ShulkerBox shulkerBox) {
                if (shulkerBox.getPersistentDataContainer().has(keyElevator, PersistentDataType.BYTE)) {
                    ItemStack drop = new ItemStack(block.getType());
                    BlockStateMeta meta = (BlockStateMeta) drop.getItemMeta();
                    if (meta != null) {
                        meta.displayName(TranslationHandler.getComponent("elevator.item_name"));
                        meta.getPersistentDataContainer().set(keyElevator, PersistentDataType.BYTE, (byte) 1);
                        drop.setItemMeta(meta);

                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!player.hasPermission("vitamin.module.elevator") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elevator")) {
            return;
        }
        if (event.getInventory().getHolder() instanceof ShulkerBox shulkerBox) {
            if (shulkerBox.getPersistentDataContainer().has(keyElevator, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                UUID uuid = player.getUniqueId();
                if (notifiedPlayers.add(uuid)) {
                    player.sendMessage(TranslationHandler.getPlayerMessage("elevator.cannot_open"));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> notifiedPlayers.remove(uuid), 20L);
                }
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isElevator)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isElevator)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        protectElevatorsFromExplosion(event.blockList());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        protectElevatorsFromExplosion(event.blockList());
    }

    private void protectElevatorsFromExplosion(List<Block> blocks) {
        blocks.removeIf(this::isElevator);
    }

    private boolean isElevator(Block block) {
        if (!woolToShulkerMap.containsValue(block.getType())) return false;

        if (block.getState() instanceof ShulkerBox shulkerBox) {
            return shulkerBox.getPersistentDataContainer().has(keyElevator, PersistentDataType.BYTE);
        }
        return false;
    }

    private boolean isOnCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) return false;

        long lastUse = cooldowns.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();

        return (currentTime - lastUse) < COOLDOWN_MILLIS;
    }

    private void teleportElevator(Player player, int direction) {
        if (isOnCooldown(player)) {
            return;
        }

        Block currentBlock = player.getLocation().getBlock().getRelative(0, -1, 0);
        if (!isElevator(currentBlock)) return;

        int maxY = player.getWorld().getMaxHeight();
        int minY = player.getWorld().getMinHeight();
        int startY = currentBlock.getY();
        Block targetElevator = null;

        for (int y = startY + direction; y >= minY && y <= maxY; y += direction) {
            Block targetBlock = player.getWorld().getBlockAt(currentBlock.getX(), y, currentBlock.getZ());
            if (isElevator(targetBlock)) {
                targetElevator = targetBlock;
                break;
            }
        }

        if (targetElevator != null) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            Location sourceLocation = player.getLocation();
            Location targetLocation = targetElevator.getLocation().add(0.5, 1, 0.5);

            player.getWorld().spawnParticle(Particle.SONIC_BOOM, sourceLocation, 5, 0.5, 0.5, 0.5, 0.1);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(targetLocation);
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, targetLocation, 5, 0.5, 0.5, 0.5, 0.1);
            }, 5L);
        }
    }
}