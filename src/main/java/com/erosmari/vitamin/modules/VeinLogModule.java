package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.database.DatabaseHandler;
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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class VeinLogModule implements Listener {

    private static final int MAX_BLOCKS = 100;

    private static final Set<Material> LOGS = EnumSet.of(
            Material.OAK_LOG,
            Material.SPRUCE_LOG,
            Material.BIRCH_LOG,
            Material.JUNGLE_LOG,
            Material.ACACIA_LOG,
            Material.DARK_OAK_LOG,
            Material.CRIMSON_STEM,
            Material.WARPED_STEM
    );

    private final JavaPlugin plugin;

    public VeinLogModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.tree_vein_miner") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.tree_vein_miner")) {
            return;
        }

        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!isValidMining(block.getType(), tool.getType())) {
            return;
        }

        if (!tool.containsEnchantment(Enchantment.EFFICIENCY) || tool.getEnchantmentLevel(Enchantment.EFFICIENCY) != 5) {
            return;
        }

        processVeinMining(block);
    }

    private boolean isValidMining(Material blockType, Material toolType) {
        return isLog(blockType) && isAxe(toolType);
    }

    private boolean isLog(Material material) {
        return LOGS.contains(material);
    }

    private boolean isAxe(Material material) {
        return material.name().endsWith("_AXE");
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void processVeinMining(Block startBlock) {
        Set<Block> veinBlocks = new HashSet<>();
        findConnectedLogs(startBlock, startBlock.getType(), veinBlocks);

        if (veinBlocks.size() > MAX_BLOCKS) {
            return;
        }

        for (Block logBlock : veinBlocks) {
            Material dropMaterial = logBlock.getType();
            logBlock.setType(Material.AIR, true);
            logBlock.getWorld().dropItemNaturally(logBlock.getLocation(), new ItemStack(dropMaterial));

            for (Block neighbor : getAdjacentBlocks(logBlock)) {
                neighbor.getState().update(true, true);
            }
        }

        if (LeafDecayModule.ENABLED) {
            LeafDecayModule decayModule = LeafDecayModule.getInstance();
            if (decayModule != null) {
                for (Block logBlock : veinBlocks) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            decayModule.removeConnectedLeaves(logBlock.getWorld(), logBlock);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    private void findConnectedLogs(Block block, Material logType, Set<Block> visited) {
        if (visited.size() >= MAX_BLOCKS || visited.contains(block)) {
            return;
        }
        visited.add(block);

        for (Block neighbor : getAdjacentBlocks(block)) {
            if (neighbor.getType() == logType) {
                findConnectedLogs(neighbor, logType, visited);
            }
        }
    }

    private Set<Block> getAdjacentBlocks(Block block) {
        Set<Block> neighbors = new HashSet<>();
        int[][] offsets = {
                {1, 0, 0},
                {-1, 0, 0},
                {0, 1, 0},
                {0, -1, 0},
                {0, 0, 1},
                {0, 0, -1}
        };

        for (int[] offset : offsets) {
            neighbors.add(block.getRelative(offset[0], offset[1], offset[2]));
        }
        return neighbors;
    }
}