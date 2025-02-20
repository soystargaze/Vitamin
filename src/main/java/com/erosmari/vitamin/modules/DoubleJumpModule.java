package com.erosmari.vitamin.modules;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class DoubleJumpModule implements Listener {

    private final Set<Player> canDoubleJump = new HashSet<>();
    private final double JUMP_BOOST;
    private boolean moduleEnabled;

    public DoubleJumpModule(JavaPlugin plugin) {
        this.JUMP_BOOST = plugin.getConfig().getDouble("double_jump.jump_boost", 0.42);
        this.moduleEnabled = plugin.getConfig().getBoolean("module.double_jump", true);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!moduleEnabled) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                        player.setAllowFlight(false);
                        if (player.isFlying()) {
                            player.setFlying(false);
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    public void setModuleEnabled(boolean enabled) {
        this.moduleEnabled = enabled;
        if (!moduleEnabled) {
            disableModule();
        }
    }

    public void disableModule() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                if (player.isFlying()) {
                    player.setFlying(false);
                }
            }
        }
        canDoubleJump.clear();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!moduleEnabled) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                if (player.isFlying()) {
                    player.setFlying(false);
                }
            }
            return;
        }

        if (isOnSolidGround(player)) {
            canDoubleJump.add(player);
            player.setAllowFlight(true);
        }
    }

    @EventHandler
    public void onPlayerDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!moduleEnabled) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                if (player.isFlying()) {
                    player.setFlying(false);
                }
            }
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!canDoubleJump.contains(player)) return;

        event.setCancelled(true);
        canDoubleJump.remove(player);
        player.setAllowFlight(false);

        float currentFallDistance = player.getFallDistance();
        Vector jumpVelocity = player.getVelocity();
        jumpVelocity.setY(JUMP_BOOST);
        player.setVelocity(jumpVelocity);
        player.setFallDistance(currentFallDistance);

        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
    }

    private boolean isOnSolidGround(Player player) {
        Material blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        return blockBelow.isSolid();
    }
}