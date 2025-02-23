package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.Particle;

import java.util.HashSet;
import java.util.Set;

public class VillagerTauntModule implements Listener {

    private final Set<Player> playersHoldingEmerald = new HashSet<>();
    private final JavaPlugin plugin;
    private final double MOVEMENT_SPEED = 0.8;

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
            playersHoldingEmerald.remove(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.villager_taunt") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.villager_taunt")) {
            playersHoldingEmerald.remove(player);
            return;
        }
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem != null && newItem.getType() == Material.EMERALD) {
            playersHoldingEmerald.add(player);
            playEmeraldEffect(player);
        } else {
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
        if (!playersHoldingEmerald.contains(player)) return;

        double DETECTION_HEIGHT = 5.0;
        double DETECTION_RADIUS = 10.0;
        player.getWorld().getNearbyEntities(
                player.getLocation(),
                DETECTION_RADIUS,
                DETECTION_HEIGHT,
                DETECTION_RADIUS
        ).forEach(entity -> {
            if (entity.getType() == EntityType.VILLAGER) {
                Villager villager = (Villager) entity;
                villager.getPathfinder().moveTo(player, MOVEMENT_SPEED);
            }
        });
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