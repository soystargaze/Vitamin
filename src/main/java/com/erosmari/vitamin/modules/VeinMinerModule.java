package com.erosmari.vitamin.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class VeinMinerModule implements Listener {

    private static final int MAX_BLOCKS = 64;

    private static final Map<Material, Material> SMELT_MAP = new EnumMap<>(Material.class);
    private static final Set<Material> ORES = EnumSet.noneOf(Material.class);

    static {
        SMELT_MAP.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);

        ORES.add(Material.COAL_ORE);
        ORES.add(Material.DEEPSLATE_COAL_ORE);
        ORES.add(Material.IRON_ORE);
        ORES.add(Material.DEEPSLATE_IRON_ORE);
        ORES.add(Material.GOLD_ORE);
        ORES.add(Material.DEEPSLATE_GOLD_ORE);
        ORES.add(Material.COPPER_ORE);
        ORES.add(Material.DEEPSLATE_COPPER_ORE);
        ORES.add(Material.LAPIS_ORE);
        ORES.add(Material.DEEPSLATE_LAPIS_ORE);
        ORES.add(Material.REDSTONE_ORE);
        ORES.add(Material.DEEPSLATE_REDSTONE_ORE);
        ORES.add(Material.EMERALD_ORE);
        ORES.add(Material.DEEPSLATE_EMERALD_ORE);
        ORES.add(Material.DIAMOND_ORE);
        ORES.add(Material.DEEPSLATE_DIAMOND_ORE);
        ORES.add(Material.NETHER_QUARTZ_ORE);
        ORES.add(Material.NETHER_GOLD_ORE);
    }

    public VeinMinerModule(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();

        if (!isValidMining(block.getType(), tool.getType())) {
            return;
        }

        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        boolean hasFireAspect = tool.containsEnchantment(Enchantment.FIRE_ASPECT);
        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);

        if (!tool.containsEnchantment(Enchantment.EFFICIENCY) || tool.getEnchantmentLevel(Enchantment.EFFICIENCY) != 5) {
            return;
        }

        processVeinMining(block, hasSilkTouch, hasFireAspect, fortuneLevel);
    }

    private void processVeinMining(Block block, boolean hasSilkTouch, boolean hasFireAspect, int fortuneLevel) {
        Set<Block> veinBlocks = new HashSet<>();
        findConnectedOres(block, block.getType(), veinBlocks);

        if (veinBlocks.size() > MAX_BLOCKS) {
            return;
        }

        for (Block ore : veinBlocks) {
            handleBlockDrop(ore, hasSilkTouch, hasFireAspect, fortuneLevel);
        }
    }

    private void handleBlockDrop(Block ore, boolean hasSilkTouch, boolean hasFireAspect, int fortuneLevel) {
        Material dropMaterial = ore.getType();

        if (!hasSilkTouch && hasFireAspect) {
            dropMaterial = SMELT_MAP.getOrDefault(dropMaterial, dropMaterial);
        }

        int dropAmount = calculateDropAmount(hasSilkTouch, fortuneLevel);

        ore.setType(Material.AIR);
        ore.getWorld().dropItemNaturally(ore.getLocation(), new ItemStack(dropMaterial, dropAmount));
    }

    private void findConnectedOres(Block block, Material oreType, Set<Block> visited) {
        if (visited.size() >= MAX_BLOCKS || visited.contains(block)) {
            return;
        }

        visited.add(block);

        for (Block neighbor : getAdjacentBlocks(block)) {
            if (neighbor.getType() == oreType) {
                findConnectedOres(neighbor, oreType, visited);
            }
        }
    }

    private Set<Block> getAdjacentBlocks(Block block) {
        Set<Block> neighbors = new HashSet<>();
        int[][] offsets = { {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1} };

        for (int[] offset : offsets) {
            neighbors.add(block.getRelative(offset[0], offset[1], offset[2]));
        }
        return neighbors;
    }

    private boolean isValidMining(Material blockType, Material toolType) {
        return isOre(blockType) && isPickaxe(toolType);
    }

    private boolean isOre(Material material) {
        return ORES.contains(material);
    }

    private boolean isPickaxe(Material material) {
        return material.name().endsWith("_PICKAXE");
    }

    private int calculateDropAmount(boolean hasSilkTouch, int fortuneLevel) {
        if (hasSilkTouch) {
            return 1;
        }
        return fortuneLevel == 0 ? 1 : 1 + ThreadLocalRandom.current().nextInt(fortuneLevel + 1);
    }
}