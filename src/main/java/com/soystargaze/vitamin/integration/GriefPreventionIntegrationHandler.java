package com.soystargaze.vitamin.integration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.function.Supplier;

public class GriefPreventionIntegrationHandler {
    private final GriefPrevention griefPrevention;

    public GriefPreventionIntegrationHandler(JavaPlugin plugin) {
        this.griefPrevention = GriefPrevention.instance;
    }

    public boolean canInteract(Player player, Location location, Event event) {
        if (player.isOp()) {
            return true;
        }

        if (griefPrevention == null) {
            return true;
        }

        Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
        if (claim == null) {
            return true;
        }

        if (claim.getOwnerID().equals(player.getUniqueId())) {
            return true;
        }

        return claim.checkPermission(player, ClaimPermission.Access, event) == null;
    }

    public boolean hasBuildPermission(Player player, Location location, Event event) {
        if (player.isOp()) {
            return true;
        }

        if (griefPrevention == null) {
            return true;
        }

        Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
        if (claim == null) {
            return true;
        }

        if (claim.getOwnerID().equals(player.getUniqueId())) {
            return true;
        }

        Supplier<String> denyReason = claim.checkPermission(player, ClaimPermission.Build, event);
        return denyReason == null;
    }

    public boolean hasInventoryPermission(Player player, Location location, Event event) {
        if (player.isOp()) {
            return true;
        }

        if (griefPrevention == null) {
            return true;
        }

        Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
        if (claim == null) {
            return true;
        }

        if (claim.getOwnerID().equals(player.getUniqueId())) {
            return true;
        }

        Supplier<String> denyReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
        return denyReason == null;
    }

    public boolean hasContainerPermissions(Player player, Location location, Event event) {
        return hasBuildPermission(player, location, event) &&
                hasInventoryPermission(player, location, event);
    }
}