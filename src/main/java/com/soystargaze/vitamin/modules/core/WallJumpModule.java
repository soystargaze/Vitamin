package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WallJumpModule implements Listener {
    private final Plugin plugin;
    private final Map<UUID, WallClimbState> playerStates;

    private final double slide_speed;
    private final double wall_jump_height;
    private final double wall_jump_distance;
    private final double wall_release_height;
    private final String slideParticle;
    private final String slideSound;
    private static final int STICK_DELAY_TICKS = 3;

    public WallJumpModule(Plugin plugin) {
        this.plugin = plugin;
        this.playerStates = new HashMap<>();
        this.slide_speed = plugin.getConfig().getDouble("wall_jump.slide_speed", 0.05);
        this.wall_jump_height = plugin.getConfig().getDouble("wall_jump.wall_jump_height", 0.7);
        this.wall_jump_distance = plugin.getConfig().getDouble("wall_jump.wall_jump_distance", 0.42);
        this.wall_release_height = plugin.getConfig().getDouble("wall_jump.wall_release_height", 0.5);
        this.slideParticle = plugin.getConfig().getString("wall_jump.slide_particle", "BLOCK");
        this.slideSound = plugin.getConfig().getString("wall_jump.slide_sound", "BLOCK_SAND_STEP");
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
        if (!player.hasPermission("vitamin.module.wall_jump") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.wall_jump")) {
            return;
        }

        UUID playerId = player.getUniqueId();

        if (event.isSneaking()) {
            handleSneakStart(player, playerId);
        } else {
            handleSneakEnd(player, playerId);
        }
    }

    private boolean isPlayerOnGround(Player player) {
        Location loc = player.getLocation();
        Block blockBelow = loc.subtract(0, 0.1, 0).getBlock();
        return blockBelow.getType().isSolid();
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
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.wall_jump") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.wall_jump")) {
            return;
        }

        UUID playerId = player.getUniqueId();
        WallClimbState state = playerStates.get(playerId);

        // Check if player is sticking to a wall and attempting to jump
        if (state != null && state.isStickingToWall && state.canWallJump) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to == null) return;

            // Detect jump by checking upward movement (Y velocity)
            double yDiff = to.getY() - from.getY();
            if (yDiff > 0.1 && !isPlayerOnGround(player)) { // Threshold for jump detection
                performWallJump(player, state.wallFace);
                cancelTasks(state);
                playerStates.remove(playerId);
            }
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
        velocity.setY(-slide_speed);
        player.setVelocity(velocity);

        BlockFace wallFace = playerStates.get(player.getUniqueId()).wallFace;
        Block wallBlock = player.getLocation().getBlock().getRelative(wallFace);

        Particle particleType;
        try {
            particleType = Particle.valueOf(slideParticle.toUpperCase());
        } catch (IllegalArgumentException e) {
            particleType = Particle.BLOCK;
        }

        if (wallBlock.getType().isSolid()) {
            if (particleType == Particle.BLOCK) {
                player.getWorld().spawnParticle(particleType, player.getLocation(), 8,
                        0.2, 0.2, 0.2, 0.1, wallBlock.getBlockData());
            } else {
                player.getWorld().spawnParticle(particleType, player.getLocation(), 8,
                        0.2, 0.2, 0.2, 0.1);
            }
        }

        Vitamin.getInstance().getVersionAdapter().playSlideSound(
                player.getLocation(),
                slideSound,
                0.2f,
                1.2f
        );
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
        float currentFallDistance = player.getFallDistance();

        Vector jumpVector = new Vector(0, this.wall_jump_height, 0);
        addHorizontalVelocity(jumpVector, wallFace, this.wall_jump_distance);

        switch (wallFace) {
            case NORTH -> jumpVector.setZ(-0.1);
            case SOUTH -> jumpVector.setZ(0.1);
            case EAST  -> jumpVector.setX(0.1);
            case WEST  -> jumpVector.setX(-0.1);
        }

        player.setVelocity(jumpVector);
        player.setFallDistance(currentFallDistance);
    }

    private void performWallRelease(Player player) {
        float currentFallDistance = player.getFallDistance();

        Vector jumpVector = new Vector(0, wall_release_height, 0);
        player.setVelocity(jumpVector);
        player.setFallDistance(currentFallDistance);
    }

    private void addHorizontalVelocity(Vector vector, BlockFace face, double wallJumpDistance) {
        switch (face) {
            case NORTH -> vector.setZ(wallJumpDistance);
            case SOUTH -> vector.setZ(-wallJumpDistance);
            case EAST -> vector.setX(-wallJumpDistance);
            case WEST -> vector.setX(wallJumpDistance);
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
        if (state.slideTask != null && !state.slideTask.isCancelled()) {
            state.slideTask.cancel();
            state.slideTask = null;
        }
        if (state.stickDelayTask != null && !state.stickDelayTask.isCancelled()) {
            state.stickDelayTask.cancel();
            state.stickDelayTask = null;
        }
        state.isStickingToWall = false;
    }
}