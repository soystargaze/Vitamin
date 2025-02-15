package com.erosmari.vividplus.modules;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WallJumpModule implements Listener {
    private final Plugin plugin;
    private final Map<UUID, WallClimbState> playerStates;
    
    private static final double SLIDE_SPEED = 0.05;
    private static final double WALL_JUMP_HEIGHT = 0.7;
    private static final double WALL_JUMP_DISTANCE = 0.4;
    private static final double WALL_RELEASE_HEIGHT = 0.5;
    private static final int STICK_DELAY_TICKS = 3;

    public WallJumpModule(Plugin plugin) {
        this.plugin = plugin;
        this.playerStates = new HashMap<>();
    }

    private static class WallClimbState {
        boolean isStickingToWall;
        BlockFace wallFace;
        BukkitRunnable slideTask;
        BukkitRunnable stickDelayTask;
        boolean canWallJump;
        double initialY;
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.isSneaking()) {
            handleSneakStart(player, playerId);
        } else {
            handleSneakEnd(player, playerId);
        }
    }

    private void handleSneakStart(Player player, UUID playerId) {
        if (!isPlayerOnGround(player) && isNextToWall(player)) {
            WallClimbState state = playerStates.computeIfAbsent(playerId, k -> new WallClimbState());
            BlockFace wallFace = getWallFace(player);
            
            if (wallFace != null) {
                initializeWallClimb(player, state, wallFace);
            }
        }
    }

    private boolean isPlayerOnGround(Player player) {
        Location loc = player.getLocation();
        Block blockBelow = loc.subtract(0, 0.1, 0).getBlock();
        return blockBelow.getType().isSolid();
    }

    private void handleSneakEnd(Player player, UUID playerId) {
        WallClimbState state = playerStates.get(playerId);
        if (state != null && state.isStickingToWall && state.canWallJump) {
            performWallRelease(player);
            cancelTasks(state);
            playerStates.remove(playerId);
        }
    }

    private void initializeWallClimb(Player player, WallClimbState state, BlockFace wallFace) {
        state.wallFace = wallFace;
        state.initialY = player.getLocation().getY();
        state.canWallJump = true;
        
        cancelTasks(state);
        player.setVelocity(new Vector(0, 0, 0));
        
        state.stickDelayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isSneaking() && isNextToWall(player)) {
                    state.isStickingToWall = true;
                    startSlideTask(player, state);
                }
            }
        };
        state.stickDelayTask.runTaskLater(plugin, STICK_DELAY_TICKS);
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        WallClimbState state = playerStates.get(playerId);

        if (state != null && state.isStickingToWall && state.canWallJump) {
            performWallJump(player, state.wallFace);
            cancelTasks(state);
            playerStates.remove(playerId);
            event.setCancelled(true);
        }
    }

    private void startSlideTask(Player player, WallClimbState state) {
        state.slideTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking() || !isNextToWall(player) || isPlayerOnGround(player)) {
                    cancelTasks(state);
                    playerStates.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (state.isStickingToWall) {
                    keepPlayerAgainstWall(player, state.wallFace);
                    applySlideVelocity(player);
                }
            }
        };
        state.slideTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void applySlideVelocity(Player player) {
        Vector velocity = player.getVelocity();
        velocity.setY(-SLIDE_SPEED);
        player.setVelocity(velocity);
    }

    private void keepPlayerAgainstWall(Player player, BlockFace face) {
        Vector velocity = player.getVelocity();
        
        switch (face) {
            case NORTH, SOUTH -> velocity.setZ(0);
            case EAST, WEST -> velocity.setX(0);
        }
        
        player.setVelocity(velocity);
    }

    private void performWallJump(Player player, BlockFace wallFace) {
        Vector jumpVector = new Vector(0, WALL_JUMP_HEIGHT, 0);
        addHorizontalVelocity(jumpVector, wallFace);
        player.setVelocity(jumpVector);
    }

    private void performWallRelease(Player player) {
        Vector jumpVector = new Vector(0, WALL_RELEASE_HEIGHT, 0);
        player.setVelocity(jumpVector);
    }

    private void addHorizontalVelocity(Vector vector, BlockFace face) {
        switch (face) {
            case NORTH -> vector.setZ(WallJumpModule.WALL_JUMP_DISTANCE);
            case SOUTH -> vector.setZ(-WallJumpModule.WALL_JUMP_DISTANCE);
            case EAST -> vector.setX(-WallJumpModule.WALL_JUMP_DISTANCE);
            case WEST -> vector.setX(WallJumpModule.WALL_JUMP_DISTANCE);
        }
    }

    private boolean isNextToWall(Player player) {
        return getWallFace(player) != null;
    }

    private BlockFace getWallFace(Player player) {
        Location loc = player.getLocation();
        
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block block = loc.getBlock().getRelative(face);
            Block blockAbove = block.getRelative(BlockFace.UP);
            
            if (isValidWallBlock(block) && isValidWallBlock(blockAbove)) {
                return face;
            }
        }
        return null;
    }

    private boolean isValidWallBlock(Block block) {
        return block.getType().isSolid() && !block.getType().equals(Material.BARRIER);
    }

    private void cancelTasks(WallClimbState state) {
        if (state.slideTask != null) {
            state.slideTask.cancel();
            state.slideTask = null;
        }
        if (state.stickDelayTask != null) {
            state.stickDelayTask.cancel();
            state.stickDelayTask = null;
        }
        state.isStickingToWall = false;
    }
}