package com.erosmari.vitamin.modules.core;

import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WeatherEffectsModule implements Listener {

    private final JavaPlugin plugin;

    private static final Material[] CROPS = {
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS
    };

    public WeatherEffectsModule(JavaPlugin plugin) {
        this.plugin = plugin;
        scheduleCropGrowthTask();
    }

    private void scheduleCropGrowthTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    if (world.hasStorm()) {
                        for (Player player : world.getPlayers()) {
                            if (!player.hasPermission("vitamin.module.weather_effects") ||
                                    !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.weather_effects")) {
                                continue;
                            }
                            Location center = player.getLocation();
                            int radius = 10;
                            for (int x = -radius; x <= radius; x++) {
                                for (int y = -5; y <= 5; y++) {
                                    for (int z = -radius; z <= radius; z++) {
                                        Location loc = center.clone().add(x, y, z);
                                        Block block = loc.getBlock();
                                        for (Material crop : CROPS) {
                                            if (block.getType() == crop && block.getBlockData() instanceof Ageable ageable) {
                                                int age = ageable.getAge();
                                                int maxAge = ageable.getMaximumAge();
                                                if (age < maxAge) {
                                                    ageable.setAge(age + 1);
                                                    block.setBlockData(ageable);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 25000L);
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        World world = event.getEntity().getWorld();
        if (!world.hasStorm()) {
            Location loc = event.getEntity().getLocation();
            world.spawnEntity(loc, event.getEntityType());
        }
    }
}