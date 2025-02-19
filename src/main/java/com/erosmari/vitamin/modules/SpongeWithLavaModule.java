package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.utils.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SpongeWithLavaModule implements Listener {

    private static final int RADIUS = 3;
    private static final int MAX_LAVA_BLOCKS = 64;
    private static final double MAX_DISTANCE = RADIUS * RADIUS;

    private final JavaPlugin plugin;

    public SpongeWithLavaModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpongePlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPONGE) return;

        if (!event.canBuild()) return;

        AsyncExecutor.getExecutor().execute(() -> processLavaAbsorption(block));
    }

    private void processLavaAbsorption(Block sponge) {
        int[] absorbed = {0};

        for (int x = -RADIUS; x <= RADIUS && absorbed[0] < MAX_LAVA_BLOCKS; x++) {
            for (int y = -RADIUS; y <= RADIUS && absorbed[0] < MAX_LAVA_BLOCKS; y++) {
                for (int z = -RADIUS; z <= RADIUS && absorbed[0] < MAX_LAVA_BLOCKS; z++) {
                    if (x * x + y * y + z * z > MAX_DISTANCE) continue;

                    final int fx = x, fy = y, fz = z;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Block target = sponge.getRelative(fx, fy, fz);
                        if (target.getType() == Material.LAVA) {
                            target.setType(Material.AIR);
                            absorbed[0]++;

                            if (absorbed[0] == 1 && sponge.getType() == Material.SPONGE) {
                                sponge.setType(Material.WET_SPONGE);
                            }
                        }
                    });
                }
            }
        }
    }
}