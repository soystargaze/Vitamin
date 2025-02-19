package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.utils.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("CallToPrintStackTrace")
public class LeafDecayModule implements Listener {

    private final JavaPlugin plugin;
    private final int maxRadius;
    private final int decayTime;
    private static final Set<Material> LOGS = EnumSet.noneOf(Material.class);
    private static final Set<Material> LEAVES = EnumSet.noneOf(Material.class);

    public static boolean ENABLED = true;

    private static LeafDecayModule instance;

    static {
        for (Material material : Material.values()) {
            if (material.name().endsWith("_LOG")) {
                LOGS.add(material);
            } else if (material.name().endsWith("_LEAVES")) {
                LEAVES.add(material);
            }
        }
    }

    public LeafDecayModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.maxRadius = plugin.getConfig().getInt("leaf_decay.max_radius", 4);
        this.decayTime = plugin.getConfig().getInt("leaf_decay.decay_time", 4) * 20;
        instance = this;
    }

    public static LeafDecayModule getInstance() {
        return instance;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        World world = block.getWorld();

        if (LOGS.contains(block.getType())) {
            AsyncExecutor.getExecutor().execute(() -> {
                try {
                    removeConnectedLeaves(world, block);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void removeConnectedLeaves(World world, Block start) {
        Set<Block> visited = new HashSet<>();
        Set<Block> queue = new HashSet<>();
        queue.add(start);
        AtomicInteger blocksProcessed = new AtomicInteger(0);
        int distance = 0;
        Set<Block> leavesToRemove = new HashSet<>();

        while (!queue.isEmpty() && distance <= maxRadius) {
            Set<Block> nextQueue = new HashSet<>();

            for (Block block : queue) {
                if (blocksProcessed.get() > 1000) {
                    return;
                }

                processNeighbors(block, visited, nextQueue, world, blocksProcessed, leavesToRemove);
            }

            queue = nextQueue;
            distance++;
        }

        scheduleLeafDecay(leavesToRemove);
    }

    private void processNeighbors(Block block, Set<Block> visited, Set<Block> nextQueue, World world, AtomicInteger blocksProcessed, Set<Block> leavesToRemove) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    Block neighbor = world.getBlockAt(
                            block.getX() + dx,
                            block.getY() + dy,
                            block.getZ() + dz
                    );

                    if (!visited.contains(neighbor) && LEAVES.contains(neighbor.getType())) {
                        visited.add(neighbor);
                        nextQueue.add(neighbor);
                        blocksProcessed.incrementAndGet();
                        leavesToRemove.add(neighbor);
                    }
                }
            }
        }
    }

    private void scheduleLeafDecay(Set<Block> leaves) {
        if (leaves.isEmpty()) return;

        int totalLeaves = leaves.size();
        int interval = Math.max(1, decayTime / totalLeaves);
        AtomicInteger index = new AtomicInteger(0);

        for (Block leaf : leaves) {
            int delay = index.getAndIncrement() * interval;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (leaf.getType().name().endsWith("_LEAVES")) {
                    leaf.breakNaturally();
                }
            }, delay);
        }
    }
}