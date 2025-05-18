package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class VeinMinerModule implements Listener {

    private static final int MAX_BLOCKS = 64;

    private static final Set<Material> ORES = EnumSet.noneOf(Material.class);
    private static final Map<Material, Material> RAW_TO_INGOT = new EnumMap<>(Material.class);
    private static final Set<Material> XP_ORES = EnumSet.noneOf(Material.class);
    private static final Random RANDOM = new Random();

    static {
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

        RAW_TO_INGOT.put(Material.RAW_IRON, Material.IRON_INGOT);
        RAW_TO_INGOT.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        RAW_TO_INGOT.put(Material.RAW_COPPER, Material.COPPER_INGOT);

        XP_ORES.add(Material.COAL_ORE);
        XP_ORES.add(Material.DEEPSLATE_COAL_ORE);
        XP_ORES.add(Material.COPPER_ORE);
        XP_ORES.add(Material.DEEPSLATE_COPPER_ORE);
        XP_ORES.add(Material.DIAMOND_ORE);
        XP_ORES.add(Material.DEEPSLATE_DIAMOND_ORE);
        XP_ORES.add(Material.EMERALD_ORE);
        XP_ORES.add(Material.DEEPSLATE_EMERALD_ORE);
        XP_ORES.add(Material.LAPIS_ORE);
        XP_ORES.add(Material.DEEPSLATE_LAPIS_ORE);
        XP_ORES.add(Material.REDSTONE_ORE);
        XP_ORES.add(Material.DEEPSLATE_REDSTONE_ORE);
        XP_ORES.add(Material.NETHER_QUARTZ_ORE);
        XP_ORES.add(Material.NETHER_GOLD_ORE);
    }

    public VeinMinerModule(JavaPlugin ignored) {
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.vein_miner") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.vein_miner")) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();

        if (!isValidMining(block.getType(), tool.getType())) {
            return;
        }

        if (!tool.containsEnchantment(Enchantment.EFFICIENCY) ||
                tool.getEnchantmentLevel(Enchantment.EFFICIENCY) != 5) {
            return;
        }

        if (!player.isSneaking()) {
            event.setCancelled(true);
            processVeinMining(block, tool);
        }
    }

    private void processVeinMining(Block block, ItemStack tool) {
        Set<Block> veinBlocks = new HashSet<>();
        findConnectedOres(block, block.getType(), veinBlocks);
        if (veinBlocks.size() > MAX_BLOCKS) {
            return;
        }
        for (Block ore : veinBlocks) {
            handleBlockDrop(ore, tool);
        }
    }

    private void handleBlockDrop(Block ore, ItemStack tool) {
        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        boolean hasFireAspect = tool.containsEnchantment(Enchantment.FIRE_ASPECT);

        Material originalType = ore.getType();

        Collection<ItemStack> drops;
        if (hasSilkTouch) {
            drops = Collections.singletonList(new ItemStack(originalType));
        } else {
            drops = ore.getDrops(tool);
            if (hasFireAspect) {
                List<ItemStack> modifiedDrops = new ArrayList<>();
                for (ItemStack drop : drops) {
                    Material dropType = drop.getType();
                    if (RAW_TO_INGOT.containsKey(dropType)) {
                        Material ingot = RAW_TO_INGOT.get(dropType);
                        modifiedDrops.add(new ItemStack(ingot, drop.getAmount()));
                    } else {
                        modifiedDrops.add(drop);
                    }
                }
                drops = modifiedDrops;
            }
        }

        if (!hasSilkTouch && XP_ORES.contains(originalType)) {
            int[] range = getExperienceRange(originalType);
            int exp = range[0] + RANDOM.nextInt(range[1] - range[0] + 1);

            if (exp > 0) {
                ExperienceOrb orb = (ExperienceOrb) ore.getWorld().spawnEntity(ore.getLocation(), EntityType.EXPERIENCE_ORB);
                orb.setExperience(exp);
            }
        }

        ore.setType(Material.AIR);
        for (ItemStack drop : drops) {
            ore.getWorld().dropItemNaturally(ore.getLocation(), drop);
        }
    }

    private int[] getExperienceRange(Material oreType) {
        return switch (oreType) {
            case COAL_ORE, DEEPSLATE_COAL_ORE, COPPER_ORE, DEEPSLATE_COPPER_ORE -> new int[]{0, 2};
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> new int[]{3, 7};
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE, NETHER_QUARTZ_ORE -> new int[]{2, 5};
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> new int[]{1, 5};
            case NETHER_GOLD_ORE -> new int[]{0, 1};
            default -> new int[]{0, 0};
        };
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
}