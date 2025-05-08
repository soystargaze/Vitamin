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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class VillagerTauntModule implements Listener {

    private final Set<Player> playersHoldingEmerald = new HashSet<>();
    private final JavaPlugin plugin;
    private final double MOVEMENT_SPEED = 0.8;
    private final Map<Player, Set<Villager>> villagerTasks = new WeakHashMap<>();
    private final Map<Villager, BukkitRunnable> villagerRunnables = new HashMap<>();
    private final double DETECTION_RADIUS_SQUARED = 196.0; // 14^2
    private final double MINIMUM_DISTANCE = 1.5;
    private final Map<UUID, Long> effectCooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5000; // 5 seconds
    private final Map<UUID, Boolean> moduleEnabledCache = new HashMap<>();
    private final Map<Villager, Location> lastPositions = new HashMap<>();
    private final int STUCK_THRESHOLD = 20; // Ticks to consider stuck

    public VillagerTauntModule(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getOnlinePlayers().forEach(player ->
                moduleEnabledCache.put(player.getUniqueId(),
                        DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.villager_taunt")));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        moduleEnabledCache.put(player.getUniqueId(),
                DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.villager_taunt"));
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        checkPlayerInventory(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkPlayerInventory(player), 1L);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        checkPlayerInventory(player);
    }

    private void checkPlayerInventory(Player player) {
        if (isEligible(player)) {
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
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isEligible(player) || !playersHoldingEmerald.contains(player)) {
            stopVillagerTasks(player);
            return;
        }

        updateVillagerMovement(player);
    }

    private void updateVillagerMovement(Player player) {
        Set<Villager> currentVillagers = villagerTasks.computeIfAbsent(player, k -> new HashSet<>());
        if (currentVillagers.size() >= 5) return; // Limit to 5 villagers

        currentVillagers.removeIf(villager -> !villager.isValid() || villager.isDead());

        Set<Villager> nearbyVillagers = new HashSet<>();
        for (Entity entity : player.getWorld().getNearbyEntities(
                player.getLocation(), 14.0, 7.0, 14.0)) {
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
        BukkitRunnable runnable = new BukkitRunnable() {
            int stuckCounter = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !villager.isValid() || villager.isDead() ||
                        !playersHoldingEmerald.contains(player) ||
                        player.getWorld() != villager.getWorld() ||
                        player.getLocation().distanceSquared(villager.getLocation()) > DETECTION_RADIUS_SQUARED) {
                    villager.setVelocity(new Vector(0, villager.getVelocity().getY(), 0));
                    cancel();
                    villagerRunnables.remove(villager);
                    return;
                }

                Location currentLoc = villager.getLocation();
                if (lastPositions.containsKey(villager) && lastPositions.get(villager).equals(currentLoc)) {
                    stuckCounter++;
                    if (stuckCounter >= STUCK_THRESHOLD) {
                        villager.setVelocity(new Vector(0, 0.5, 0)); // Jump to unstuck
                        stuckCounter = 0;
                    }
                } else {
                    stuckCounter = 0;
                }
                lastPositions.put(villager, currentLoc);

                double distanceSquared = player.getLocation().distanceSquared(villager.getLocation());
                if (distanceSquared <= MINIMUM_DISTANCE * MINIMUM_DISTANCE) {
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
        };
        runnable.runTaskTimer(plugin, 0L, 5L); // Every 5 ticks
        villagerRunnables.put(villager, runnable);
    }

    private void stopVillagerTasks(Player player) {
        Set<Villager> villagers = villagerTasks.remove(player);
        if (villagers != null) {
            for (Villager villager : villagers) {
                stopVillagerMovement(villager);
            }
        }
    }

    private void stopVillagerMovement(Villager villager) {
        if (villagerRunnables.containsKey(villager)) {
            villagerRunnables.get(villager).cancel();
            villagerRunnables.remove(villager);
            villager.setVelocity(new Vector(0, villager.getVelocity().getY(), 0));
        }
    }

    private void playEmeraldEffect(Player player) {
        long currentTime = System.currentTimeMillis();
        if (!effectCooldowns.containsKey(player.getUniqueId()) ||
                currentTime - effectCooldowns.get(player.getUniqueId()) > COOLDOWN_TIME) {
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
            effectCooldowns.put(player.getUniqueId(), currentTime);
        }
    }

    private void facePlayer(Villager villager, Player player) {
        Location villagerLoc = villager.getLocation();
        Location playerLoc = player.getLocation();
        double dx = playerLoc.getX() - villagerLoc.getX();
        double dz = playerLoc.getZ() - villagerLoc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        villager.setRotation(yaw, villagerLoc.getPitch());
    }

    private boolean isEligible(Player player) {
        return !player.hasPermission("vitamin.module.villager_taunt") ||
                !moduleEnabledCache.getOrDefault(player.getUniqueId(), false);
    }
}