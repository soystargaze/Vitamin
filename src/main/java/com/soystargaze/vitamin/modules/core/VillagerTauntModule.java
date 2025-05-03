package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VillagerTauntModule implements Listener {

    private final Set<Player> playersHoldingEmerald = new HashSet<>();
    private final JavaPlugin plugin;
    private final double MOVEMENT_SPEED = 0.8;
    private final Map<UUID, Set<Villager>> villagerTasks = new HashMap<>();
    private final double DETECTION_RADIUS = 14.0;
    private final double DETECTION_HEIGHT = 7.0;
    private final double MINIMUM_DISTANCE = 1.5;

    public VillagerTauntModule(JavaPlugin plugin) {
        this.plugin = plugin;
        startInventoryCheck();
    }

    private void startInventoryCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, () ->
                Bukkit.getOnlinePlayers().forEach(this::checkPlayerInventory), 20L, 20L);
    }

    private void checkPlayerInventory(Player player) {
        if (!player.hasPermission("vitamin.module.villager_taunt") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.villager_taunt")) {
            stopVillagerTasks(player);
            playersHoldingEmerald.remove(player);
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (mainHand.getType() == Material.EMERALD || offHand.getType() == Material.EMERALD) {
            if (!playersHoldingEmerald.contains(player)) {
                playersHoldingEmerald.add(player);
                playEmeraldEffect(player);
            }
        } else {
            stopVillagerTasks(player);
            playersHoldingEmerald.remove(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.villager_taunt") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.villager_taunt")) {
            stopVillagerTasks(player);
            playersHoldingEmerald.remove(player);
            return;
        }
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem != null && newItem.getType() == Material.EMERALD) {
            playersHoldingEmerald.add(player);
            playEmeraldEffect(player);
        } else {
            stopVillagerTasks(player);
            playersHoldingEmerald.remove(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.villager_taunt") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.villager_taunt")) {
            return;
        }
        if (!playersHoldingEmerald.contains(player)) {
            stopVillagerTasks(player);
            return;
        }

        updateVillagerMovement(player);
    }

    private void updateVillagerMovement(Player player) {
        UUID playerId = player.getUniqueId();
        Set<Villager> currentVillagers = villagerTasks.computeIfAbsent(playerId, k -> new HashSet<>());

        currentVillagers.removeIf(villager -> !villager.isValid() || villager.isDead());

        Set<Villager> nearbyVillagers = new HashSet<>();
        for (Entity entity : player.getWorld().getNearbyEntities(
                player.getLocation(), DETECTION_RADIUS, DETECTION_HEIGHT, DETECTION_RADIUS)) {
            if (entity.getType() == EntityType.VILLAGER) {
                nearbyVillagers.add((Villager) entity);
            }
        }

        for (Villager villager : nearbyVillagers) {
            if (!currentVillagers.contains(villager)) {
                currentVillagers.add(villager);
                startVillagerMovement(player, villager);
            }
        }

        currentVillagers.removeIf(villager -> !nearbyVillagers.contains(villager));
    }

    private void startVillagerMovement(Player player, Villager villager) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !villager.isValid() || villager.isDead() ||
                        !playersHoldingEmerald.contains(player) ||
                        player.getWorld() != villager.getWorld() ||
                        player.getLocation().distanceSquared(villager.getLocation()) > DETECTION_RADIUS * DETECTION_RADIUS) {
                    villager.setVelocity(new Vector(0, villager.getVelocity().getY(), 0)); // Stop horizontal movement
                    cancel();
                    return;
                }

                double distance = player.getLocation().distance(villager.getLocation());
                if (distance <= MINIMUM_DISTANCE) {
                    villager.setVelocity(new Vector(0, villager.getVelocity().getY(), 0));
                    facePlayer(villager, player);
                    return;
                }

                Vector direction = player.getLocation().toVector()
                        .subtract(villager.getLocation().toVector())
                        .normalize()
                        .multiply(MOVEMENT_SPEED * 0.2);

                villager.setVelocity(new Vector(direction.getX(), villager.getVelocity().getY(), direction.getZ()));
                facePlayer(villager, player);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void stopVillagerTasks(Player player) {
        UUID playerId = player.getUniqueId();
        Set<Villager> villagers = villagerTasks.remove(playerId);
        if (villagers != null) {
            for (Villager villager : villagers) {
                if (villager.isValid() && !villager.isDead()) {
                    villager.setVelocity(new Vector(0, villager.getVelocity().getY(), 0));
                }
            }
        }
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

    private void facePlayer(Villager villager, Player player) {
        Location villagerLoc = villager.getLocation();
        Location playerLoc = player.getLocation();
        double dx = playerLoc.getX() - villagerLoc.getX();
        double dz = playerLoc.getZ() - villagerLoc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        villagerLoc.setYaw(yaw);
        villager.teleport(villagerLoc);
    }
}