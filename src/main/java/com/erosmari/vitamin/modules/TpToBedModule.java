package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.utils.LoggingUtils;
import com.erosmari.vitamin.utils.TranslationHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class TpToBedModule implements Listener {

    private final Plugin plugin;
    private final Map<Player, BukkitRunnable> teleportDelays = new HashMap<>();

    private final Map<Player, Long> lastMessageSent = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 2000; // 2 s

    private final Map<Player, Long> lastInteract = new HashMap<>();
    private static final long INTERACT_COOLDOWN_MS = 300; // 0.3 s

    public TpToBedModule(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long lastClick = lastInteract.getOrDefault(player, 0L);
        if (now - lastClick < INTERACT_COOLDOWN_MS) {
            return;
        }
        lastInteract.put(player, now);

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        Location bedSpawn = player.getRespawnLocation();
        if (teleportDelays.containsKey(player)) {
            sendMessageWithCooldown(player, "tpcompass.already_teleporting");
            return;
        }

        if (bedSpawn == null) {
            sendMessageWithCooldown(player, "tpcompass.no_bed");
            return;
        }

        Location startLocation = player.getLocation();
        BukkitRunnable task = createTeleportTask(player, startLocation, bedSpawn);
        task.runTaskTimer(plugin, 0L, 1L);
        teleportDelays.put(player, task);
    }

    private BukkitRunnable createTeleportTask(Player player, Location startLocation, Location bedSpawn) {
        return new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                Location currentLocation = player.getLocation();
                if (movedFromStart(currentLocation, startLocation)) {
                    sendMessageWithCooldown(player, "tpcompass.cancelled");
                    teleportDelays.remove(player);
                    cancel();
                    return;
                }

                player.getWorld().spawnParticle(Particle.SONIC_BOOM, currentLocation, 1, 1.0, 1.0, 1.0, 0);

                if (ticks % 20 == 0) {
                    int secondsLeft = plugin.getConfig().getInt("tp_with_compass.channeling_time", 3) - (ticks / 20);
                    if (secondsLeft > 0) {
                        player.sendActionBar((TranslationHandler.getPlayerMessage("tpcompass.channeling", secondsLeft)));
                    }
                }

                ticks++;

                if (ticks >= 60) {
                    player.getWorld().playSound(player.getLocation(), "entity.enderman.teleport", 1.0f, 1.0f);
                    player.teleport(bedSpawn);
                    sendMessageWithCooldown(player, "tpcompass.success");
                    teleportDelays.remove(player);
                }
            }
        };
    }

    private boolean movedFromStart(Location current, Location start) {
        return current.getBlockX() != start.getBlockX()
                || current.getBlockY() != start.getBlockY()
                || current.getBlockZ() != start.getBlockZ();
    }

    private void sendMessageWithCooldown(Player player, String messageKey) {
        long now = System.currentTimeMillis();
        long lastSent = lastMessageSent.getOrDefault(player, 0L);

        if (now - lastSent >= MESSAGE_COOLDOWN_MS) {
            LoggingUtils.sendMessage(player, messageKey);
            lastMessageSent.put(player, now);
        }
    }
}