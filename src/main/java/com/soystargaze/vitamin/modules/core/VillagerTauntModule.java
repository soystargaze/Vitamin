package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.Particle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VillagerTauntModule implements Listener {

    private final Set<Player> playersHoldingEmerald = new HashSet<>();
    private final JavaPlugin plugin;
    private final double MOVEMENT_SPEED = 0.8;
    private final Map<UUID, Set<Villager>> villagerTasks = new HashMap<>(); // Track villagers per player
    private final double DETECTION_RADIUS = 10.0;

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

        // Update villager movement
        updateVillagerMovement(player);
    }

    private void updateVillagerMovement(Player player) {
        UUID playerId = player.getUniqueId();
        Set<Villager> currentVillagers = villagerTasks.computeIfAbsent(playerId, k -> new HashSet<>());

        // Clear previous tasks if any villagers are no longer valid
        currentVillagers.removeIf(villager -> !villager.isValid() || villager.isDead());

        // Find nearby villagers
        Set<Villager> nearbyVillagers = new HashSet<>();
        double DETECTION_HEIGHT = 5.0;
        for (Entity entity : player.getWorld().getNearbyEntities(
                player.getLocation(), DETECTION_RADIUS, DETECTION_HEIGHT, DETECTION_RADIUS)) {
            if (entity.getType() == EntityType.VILLAGER) {
                nearbyVillagers.add((Villager) entity);
            }
        }

        // Start movement for new villagers
        for (Villager villager : nearbyVillagers) {
            if (!currentVillagers.contains(villager)) {
                currentVillagers.add(villager);
                startVillagerMovement(player, villager);
            }
        }

        // Stop movement for villagers no longer in range
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

                // Calculate direction to player
                Vector direction = player.getLocation().toVector()
                        .subtract(villager.getLocation().toVector())
                        .normalize()
                        .multiply(MOVEMENT_SPEED * 0.2); // Scale for smoother movement

                // Apply velocity (preserve Y to allow natural falling/jumping)
                villager.setVelocity(new Vector(direction.getX(), villager.getVelocity().getY(), direction.getZ()));
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run every 2 ticks for smooth movement
    }

    private void stopVillagerTasks(Player player) {
        UUID playerId = player.getUniqueId();
        Set<Villager> villagers = villagerTasks.remove(playerId);
        if (villagers != null) {
            for (Villager villager : villagers) {
                if (villager.isValid() && !villager.isDead()) {
                    villager.setVelocity(new Vector(0, villager.getVelocity().getY(), 0)); // Stop horizontal movement
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
}