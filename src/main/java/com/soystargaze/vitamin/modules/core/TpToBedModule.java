package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import com.soystargaze.vitamin.utils.text.legacy.LegacyTranslationHandler;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation")
public class TpToBedModule implements Listener {

    private final Plugin plugin;
    private final Map<Player, BukkitRunnable> teleportDelays = new HashMap<>();
    private final Map<Player, Long> lastTeleportTime = new HashMap<>();
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
        if (!player.hasPermission("vitamin.module.tp_compass") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.tp_to_bed_with_compass")) {
            return;
        }

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

        long cooldownTime = plugin.getConfig().getLong("tp_with_compass.cooldown_time", 0) * 1000;
        long lastTp = lastTeleportTime.getOrDefault(player, 0L);
        if (now - lastTp < cooldownTime) {
            long timeLeft = (cooldownTime - (now - lastTp)) / 1000;
            sendMessageWithCooldown(player, "tpcompass.cooldown", timeLeft);
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
        boolean requireStationary = plugin.getConfig().getBoolean("tp_with_compass.require_stationary", true);
        BukkitRunnable task = createTeleportTask(player, startLocation, bedSpawn, requireStationary);
        task.runTaskTimer(plugin, 0L, 1L);
        teleportDelays.put(player, task);
    }

    private BukkitRunnable createTeleportTask(Player player, Location startLocation, Location bedSpawn, boolean requireStationary) {
        return new BukkitRunnable() {
            int ticks = 0;
            final int channelingTime = plugin.getConfig().getInt("tp_with_compass.channeling_time", 3) * 20;

            @Override
            public void run() {
                Location currentLocation = player.getLocation();
                if (requireStationary && movedFromStart(currentLocation, startLocation)) {
                    sendMessageWithCooldown(player, "tpcompass.cancelled");
                    teleportDelays.remove(player);
                    cancel();
                    return;
                }

                player.getWorld().spawnParticle(Particle.SONIC_BOOM, currentLocation, 1, 1.0, 1.0, 1.0, 0);

                if (ticks % 20 == 0) {
                    int secondsLeft = (channelingTime - ticks) / 20;
                    if (secondsLeft > 0) {
                        Component messageComponent = LegacyTranslationHandler.getPlayerMessage("tpcompass.channeling", secondsLeft);
                        String message = LegacyComponentSerializer.legacyAmpersand().serialize(messageComponent);
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
                    }
                }

                ticks++;

                if (ticks >= channelingTime) {
                    player.getWorld().playSound(player.getLocation(), "entity.enderman.teleport", 1.0f, 1.0f);
                    player.teleport(bedSpawn);
                    sendMessageWithCooldown(player, "tpcompass.success");
                    lastTeleportTime.put(player, System.currentTimeMillis());
                    teleportDelays.remove(player);
                    cancel();
                }
            }
        };
    }

    private boolean movedFromStart(Location current, Location start) {
        return current.getBlockX() != start.getBlockX()
                || current.getBlockY() != start.getBlockY()
                || current.getBlockZ() != start.getBlockZ();
    }

    private void sendMessageWithCooldown(Player player, String messageKey, Object... args) {
        long now = System.currentTimeMillis();
        long lastSent = lastMessageSent.getOrDefault(player, 0L);

        if (now - lastSent >= MESSAGE_COOLDOWN_MS) {
            TextHandler.get().sendMessage(player, messageKey, args);
            lastMessageSent.put(player, now);
        }
    }
}