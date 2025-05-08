package com.soystargaze.vitamin.modules.paper;

import com.destroystokyo.paper.entity.Pathfinder;
import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class PaperVillagerTauntModule implements Listener {

    private final Set<Player> playersHoldingEmerald = new HashSet<>();
    private final JavaPlugin plugin;
    private final double MOVEMENT_SPEED = 0.8;
    private final Map<Villager, Entity> villagerTargets = new HashMap<>();
    private final double PICKUP_DISTANCE = 1.5;
    private final double DETECTION_RADIUS = 10.0;
    private final double DETECTION_HEIGHT = 5.0;

    public PaperVillagerTauntModule(JavaPlugin plugin) {
        this.plugin = plugin;
        startVillagerPathfindingUpdate();
        startVillagerPickupCheck();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        checkPlayerInventory(player);
        updateVillagerAssignments(player.getWorld());
    }

    private void checkPlayerInventory(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (mainHand.getType() == Material.EMERALD || offHand.getType() == Material.EMERALD) {
            if (!playersHoldingEmerald.contains(player)) {
                playersHoldingEmerald.add(player);
                playEmeraldEffect(player);
            }
        } else {
            playersHoldingEmerald.remove(player);
        }
    }

    private void startVillagerPathfindingUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Villager, Entity> entry : villagerTargets.entrySet()) {
                    Villager villager = entry.getKey();
                    Entity target = entry.getValue();
                    if (target == null || !target.isValid()) {
                        villagerTargets.remove(villager);
                        continue;
                    }
                    Pathfinder pathfinder = villager.getPathfinder();
                    if (target instanceof Player) {
                        pathfinder.moveTo((Player) target, MOVEMENT_SPEED);
                    } else if (target instanceof Item) {
                        pathfinder.moveTo(target.getLocation(), MOVEMENT_SPEED);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startVillagerPickupCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<Villager, Entity>> iterator = villagerTargets.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Villager, Entity> entry = iterator.next();
                    Villager villager = entry.getKey();
                    Entity target = entry.getValue();
                    if (target instanceof Item item) {
                        if (villager.getLocation().distanceSquared(item.getLocation()) < PICKUP_DISTANCE * PICKUP_DISTANCE) {
                            pickUpEmerald(villager, item);
                            iterator.remove();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void updateVillagerAssignments(World world) {
        for (Villager entity : world.getEntitiesByClass(Villager.class)) {
            assignVillagerToNearestTarget(entity);
        }
    }

    private void assignVillagerToNearestTarget(Villager villager) {
        Entity nearestTarget = null;
        double minDistanceSquared = Double.MAX_VALUE;

        for (Player player : playersHoldingEmerald) {
            if (player.getWorld() == villager.getWorld() &&
                    player.hasPermission("vitamin.module.villager_follow_emeralds") &&
                    DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.villager_follow_emeralds")) {
                double distanceSquared = player.getLocation().distanceSquared(villager.getLocation());
                if (distanceSquared < minDistanceSquared && distanceSquared < DETECTION_RADIUS * DETECTION_RADIUS) {
                    minDistanceSquared = distanceSquared;
                    nearestTarget = player;
                }
            }
        }

        for (Entity entity : villager.getWorld().getNearbyEntities(
                villager.getLocation(), DETECTION_RADIUS, DETECTION_HEIGHT, DETECTION_RADIUS)) {
            if (entity instanceof Item item) {
                if (item.getItemStack().getType() == Material.EMERALD) {
                    boolean hasEligiblePlayerNearby = false;
                    for (Player player : playersHoldingEmerald) {
                        if (player.getWorld() == item.getWorld() &&
                                player.getLocation().distanceSquared(item.getLocation()) < DETECTION_RADIUS * DETECTION_RADIUS &&
                                player.hasPermission("vitamin.module.villager_follow_emeralds") &&
                                DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.villager_follow_emeralds")) {
                            hasEligiblePlayerNearby = true;
                            break;
                        }
                    }
                    if (hasEligiblePlayerNearby) {
                        double distanceSquared = item.getLocation().distanceSquared(villager.getLocation());
                        if (distanceSquared < minDistanceSquared) {
                            minDistanceSquared = distanceSquared;
                            nearestTarget = item;
                        }
                    }
                }
            }
        }

        if (nearestTarget != null) {
            villagerTargets.put(villager, nearestTarget);
        } else {
            villagerTargets.remove(villager);
        }
    }

    private void pickUpEmerald(Villager villager, Item item) {
        ItemStack itemStack = item.getItemStack();
        villager.getInventory().addItem(itemStack);
        item.remove();
        villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    private void playEmeraldEffect(Player player) {
        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5, 0
        );
        player.playSound(
                player.getLocation(),
                Sound.ENTITY_VILLAGER_TRADE,
                0.5f, 1.0f
        );
    }
}