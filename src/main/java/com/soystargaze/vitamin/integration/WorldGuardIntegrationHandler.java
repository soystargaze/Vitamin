package com.soystargaze.vitamin.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGuardIntegrationHandler {

    @SuppressWarnings("unused")
    public WorldGuardIntegrationHandler(JavaPlugin plugin) {
    }

    public boolean canInteract(Player player, Location location) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            ApplicableRegionSet regionSet = query.getApplicableRegions(weLoc);

            if (regionSet.getRegions().isEmpty() || regionSet.getRegions().stream().allMatch(region -> region.getId().equalsIgnoreCase("__global__"))) {
                return true;
            }

            for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : regionSet.getRegions()) {
                if (!region.getId().equalsIgnoreCase("__global__")) {
                    if (region.getMembers().contains(wgPlayer.getUniqueId())) {
                        continue;
                    }
                    if (!query.testState(weLoc, wgPlayer, Flags.INTERACT)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canBuild(Player player, Location location) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            ApplicableRegionSet regionSet = query.getApplicableRegions(weLoc);

            if (regionSet.getRegions().isEmpty() || regionSet.getRegions().stream().allMatch(region -> region.getId().equalsIgnoreCase("__global__"))) {
                return true;
            }

            for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : regionSet.getRegions()) {
                if (!region.getId().equalsIgnoreCase("__global__")) {
                    if (region.getMembers().contains(wgPlayer.getUniqueId())) {
                        continue;
                    }
                    if (!query.testBuild(weLoc, wgPlayer)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}