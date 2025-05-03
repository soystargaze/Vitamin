package com.soystargaze.vitamin.modules.core;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import com.soystargaze.vitamin.database.DatabaseHandler;

public class AutoToolModule implements Listener {

    public AutoToolModule() {
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("vitamin.module.auto_tool") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.auto_tool")) {
            return;
        }

        Material blockType = event.getBlock().getType();
        ToolType toolType = getToolTypeForBlock(blockType);
        if (toolType != null) {
            int slot = findBestTool(player, toolType);
            if (slot != -1 && player.getInventory().getHeldItemSlot() != slot) {
                player.getInventory().setHeldItemSlot(slot);
                spawnToolChangeParticles(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("vitamin.module.auto_tool") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.auto_tool")) {
            return;
        }

        int slot = findBestToolForSword(player);
        if (slot != -1 && player.getInventory().getHeldItemSlot() != slot) {
            player.getInventory().setHeldItemSlot(slot);
            spawnToolChangeParticles(player);
        }
    }

    private enum ToolType {
        PICKAXE,
        AXE,
        SHOVEL
    }

    private ToolType getToolTypeForBlock(Material block) {
        return switch (block) {
            case STONE, COBBLESTONE, ANDESITE, DIORITE, GRANITE, DEEPSLATE, TUFF, CALCITE, DRIPSTONE_BLOCK,
                 IRON_BLOCK, GOLD_BLOCK, DIAMOND_BLOCK, NETHERITE_BLOCK, EMERALD_BLOCK, NETHERRACK, END_STONE,
                 COAL_ORE, DEEPSLATE_COAL_ORE, IRON_ORE, DEEPSLATE_IRON_ORE, GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, LAPIS_ORE, DEEPSLATE_LAPIS_ORE, DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE, NETHER_QUARTZ_ORE, NETHER_GOLD_ORE, ANCIENT_DEBRIS, MAGMA_BLOCK,
                 OBSIDIAN, CRYING_OBSIDIAN, END_STONE_BRICKS, SMOOTH_STONE, STONE_BRICKS, MOSSY_STONE_BRICKS,
                 CRACKED_STONE_BRICKS, CHISELED_STONE_BRICKS, POLISHED_ANDESITE, POLISHED_DIORITE, POLISHED_GRANITE,
                 POLISHED_DEEPSLATE, COPPER_ORE, DEEPSLATE_COPPER_ORE, COPPER_BLOCK, EXPOSED_COPPER, WEATHERED_COPPER,
                 OXIDIZED_COPPER, BASALT, SMOOTH_BASALT, BLACKSTONE, POLISHED_BLACKSTONE, NETHER_BRICKS, CRACKED_NETHER_BRICKS, CHISELED_NETHER_BRICKS, RESPAWN_ANCHOR, QUARTZ_BLOCK ->
                    ToolType.PICKAXE;
            case GRAVEL, SAND, RED_SAND, DIRT, PODZOL, COARSE_DIRT, GRASS_BLOCK -> ToolType.SHOVEL;
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                 STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_BIRCH_LOG, STRIPPED_JUNGLE_LOG, STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG,
                 CRIMSON_STEM, WARPED_STEM, STRIPPED_CRIMSON_STEM, STRIPPED_WARPED_STEM,
                 OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD,
                 STRIPPED_OAK_WOOD, STRIPPED_SPRUCE_WOOD, STRIPPED_BIRCH_WOOD, STRIPPED_JUNGLE_WOOD, STRIPPED_ACACIA_WOOD, STRIPPED_DARK_OAK_WOOD,
                 CRIMSON_HYPHAE, WARPED_HYPHAE, STRIPPED_CRIMSON_HYPHAE, STRIPPED_WARPED_HYPHAE,
                 OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, DARK_OAK_PLANKS, CRIMSON_PLANKS, WARPED_PLANKS,
                 OAK_STAIRS, SPRUCE_STAIRS, BIRCH_STAIRS, JUNGLE_STAIRS, ACACIA_STAIRS, DARK_OAK_STAIRS, CRIMSON_STAIRS, WARPED_STAIRS,
                 OAK_SLAB, SPRUCE_SLAB, BIRCH_SLAB, JUNGLE_SLAB, ACACIA_SLAB, DARK_OAK_SLAB, CRIMSON_SLAB, WARPED_SLAB,
                 OAK_FENCE, SPRUCE_FENCE, BIRCH_FENCE, JUNGLE_FENCE, ACACIA_FENCE, DARK_OAK_FENCE, CRIMSON_FENCE, WARPED_FENCE,
                 OAK_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE, JUNGLE_FENCE_GATE, ACACIA_FENCE_GATE, DARK_OAK_FENCE_GATE, CRIMSON_FENCE_GATE, WARPED_FENCE_GATE,
                 OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR, DARK_OAK_DOOR, CRIMSON_DOOR, WARPED_DOOR,
                 OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR, ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR, CRIMSON_TRAPDOOR, WARPED_TRAPDOOR,
                 OAK_BUTTON, SPRUCE_BUTTON, BIRCH_BUTTON, JUNGLE_BUTTON, ACACIA_BUTTON, DARK_OAK_BUTTON, CRIMSON_BUTTON, WARPED_BUTTON,
                 OAK_PRESSURE_PLATE, SPRUCE_PRESSURE_PLATE, BIRCH_PRESSURE_PLATE, JUNGLE_PRESSURE_PLATE, ACACIA_PRESSURE_PLATE, DARK_OAK_PRESSURE_PLATE, CRIMSON_PRESSURE_PLATE, WARPED_PRESSURE_PLATE,
                 OAK_SIGN, SPRUCE_SIGN, BIRCH_SIGN, JUNGLE_SIGN, ACACIA_SIGN, DARK_OAK_SIGN, CRIMSON_SIGN, WARPED_SIGN,
                 OAK_WALL_SIGN, SPRUCE_WALL_SIGN, BIRCH_WALL_SIGN, JUNGLE_WALL_SIGN, ACACIA_WALL_SIGN, DARK_OAK_WALL_SIGN, CRIMSON_WALL_SIGN, WARPED_WALL_SIGN,
                 OAK_HANGING_SIGN, SPRUCE_HANGING_SIGN, BIRCH_HANGING_SIGN, JUNGLE_HANGING_SIGN, ACACIA_HANGING_SIGN, DARK_OAK_HANGING_SIGN, CRIMSON_HANGING_SIGN, WARPED_HANGING_SIGN,
                 OAK_BOAT, SPRUCE_BOAT, BIRCH_BOAT, JUNGLE_BOAT, ACACIA_BOAT, DARK_OAK_BOAT, WARPED_WART_BLOCK, WARPED_ROOTS,
                 OAK_CHEST_BOAT, SPRUCE_CHEST_BOAT, BIRCH_CHEST_BOAT, JUNGLE_CHEST_BOAT, ACACIA_CHEST_BOAT, DARK_OAK_CHEST_BOAT, CRIMSON_FUNGUS, WARPED_FUNGUS, BAMBOO_BLOCK, BAMBOO_DOOR, BAMBOO_SLAB, BAMBOO_STAIRS ->
                    ToolType.AXE;
            default -> null;
        };
    }

    private int findBestTool(Player player, ToolType type) {
        Material[] tools;
        switch (type) {
            case PICKAXE:
                tools = new Material[]{
                        Material.NETHERITE_PICKAXE,
                        Material.DIAMOND_PICKAXE,
                        Material.IRON_PICKAXE,
                        Material.STONE_PICKAXE,
                        Material.WOODEN_PICKAXE,
                        Material.GOLDEN_PICKAXE
                };
                break;
            case AXE:
                tools = new Material[]{
                        Material.NETHERITE_AXE,
                        Material.DIAMOND_AXE,
                        Material.IRON_AXE,
                        Material.STONE_AXE,
                        Material.WOODEN_AXE,
                        Material.GOLDEN_AXE
                };
                break;
            case SHOVEL:
                tools = new Material[]{
                        Material.NETHERITE_SHOVEL,
                        Material.DIAMOND_SHOVEL,
                        Material.IRON_SHOVEL,
                        Material.STONE_SHOVEL,
                        Material.WOODEN_SHOVEL,
                        Material.GOLDEN_SHOVEL
                };
                break;
            default:
                return -1;
        }
        return findToolInInventory(player, tools);
    }

    private int findBestToolForSword(Player player) {
        Material[] swords = new Material[]{
                Material.NETHERITE_SWORD,
                Material.MACE,
                Material.TRIDENT,
                Material.DIAMOND_SWORD,
                Material.IRON_SWORD,
                Material.STONE_SWORD,
                Material.WOODEN_SWORD,
                Material.GOLDEN_SWORD
        };
        return findToolInInventory(player, swords);
    }

    private int findToolInInventory(Player player, Material[] desiredTools) {
        PlayerInventory inv = player.getInventory();
        int foundSlot = -1;
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack item = inv.getItem(slot);
            if (item != null) {
                for (Material tool : desiredTools) {
                    if (item.getType() == tool) {
                        foundSlot = slot;
                        break;
                    }
                }
                if (foundSlot != -1) {
                    break;
                }
            }
        }
        if (foundSlot == -1) {
            return -1;
        }
        if (foundSlot < 9) {
            return foundSlot;
        } else {
            int hotbarSlot = inv.getHeldItemSlot();
            ItemStack currentItem = inv.getItem(hotbarSlot);
            ItemStack bestTool = inv.getItem(foundSlot);
            inv.setItem(hotbarSlot, bestTool);
            inv.setItem(foundSlot, currentItem);
            return hotbarSlot;
        }
    }

    private void spawnToolChangeParticles(Player player) {
        Location loc = player.getLocation().add(0, 1.5, 0);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 0.3, 0.3, 0.3, 0.01);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.2f, 1.2f);
    }
}