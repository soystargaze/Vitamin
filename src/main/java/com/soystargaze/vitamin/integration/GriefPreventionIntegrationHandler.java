package com.soystargaze.vitamin.integration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

public class GriefPreventionIntegrationHandler {

    private final GriefPrevention griefPrevention;

    @SuppressWarnings("unused")
    public GriefPreventionIntegrationHandler(JavaPlugin plugin) {
        this.griefPrevention = GriefPrevention.instance;
    }

    public boolean canInteract(Player player, Location location, Event event) {
        Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
        if (claim == null) {
            return true;
        }
        return claim.checkPermission(player, ClaimPermission.Access, event) == null;
    }

    public boolean canBuild(Player player, Location location, Event event) {
        Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
        if (claim == null) {
            return true;
        }
        return claim.checkPermission(player, ClaimPermission.Build, event) == null;
    }
}