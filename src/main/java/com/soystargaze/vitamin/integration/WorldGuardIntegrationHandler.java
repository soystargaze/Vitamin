package com.soystargaze.vitamin.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGuardIntegrationHandler {

    @SuppressWarnings("unused")
    public WorldGuardIntegrationHandler(JavaPlugin plugin) {
    }

    public boolean canInteract(Player player, Location location) {
        return canInteractWithFlag(player, location, WorldGuardFlags.VITAMIN_ENTITY);
    }

    public boolean canBuild(Player player, Location location) {
        return canBuildWithFlag(player, location, WorldGuardFlags.VITAMIN_CONTAINER);
    }

    public boolean canInteractEntity(Player player, Location location) {
        return canInteractWithFlag(player, location, WorldGuardFlags.VITAMIN_ENTITY);
    }

    public boolean canInteractContainer(Player player, Location location) {
        return canBuildWithFlag(player, location, WorldGuardFlags.VITAMIN_CONTAINER);
    }

    // Waystone methods
    public boolean canCreateWaystone(Player player, Location location) {
        return canBuildWithFlag(player, location, WorldGuardFlags.VITAMIN_WAYSTONE_CREATE, WorldGuardFlags.VITAMIN_WAYSTONE);
    }

    public boolean canUseWaystone(Player player, Location location) {
        return canInteractWithFlag(player, location, WorldGuardFlags.VITAMIN_WAYSTONE_USE, WorldGuardFlags.VITAMIN_WAYSTONE);
    }

    public boolean canBreakWaystone(Player player, Location location) {
        return canBuildWithFlag(player, location, WorldGuardFlags.VITAMIN_WAYSTONE_BREAK, WorldGuardFlags.VITAMIN_WAYSTONE);
    }

    private boolean canInteractWithFlag(Player player, Location location, StateFlag primaryFlag, StateFlag fallbackFlag) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            ApplicableRegionSet regionSet = query.getApplicableRegions(weLoc);

            if (regionSet.getRegions().isEmpty() ||
                    regionSet.getRegions().stream().allMatch(region -> region.getId().equalsIgnoreCase("__global__"))) {

                if (primaryFlag != null) {
                    StateFlag.State primaryState = regionSet.queryState(wgPlayer, primaryFlag);
                    if (primaryState == StateFlag.State.ALLOW) {
                        return true;
                    }
                }

                if (fallbackFlag != null) {
                    StateFlag.State fallbackState = regionSet.queryState(wgPlayer, fallbackFlag);
                    return fallbackState == StateFlag.State.ALLOW;
                }

                return false;
            }

            boolean hasGeneralPermission = query.testState(weLoc, wgPlayer, Flags.INTERACT);

            if (!hasGeneralPermission) {
                return false;
            }

            if (primaryFlag != null) {
                StateFlag.State primaryState = regionSet.queryState(wgPlayer, primaryFlag);
                if (primaryState == StateFlag.State.ALLOW) {
                    return true;
                } else if (primaryState == StateFlag.State.DENY) {
                    return false;
                }
            }

            if (fallbackFlag != null) {
                StateFlag.State fallbackState = regionSet.queryState(wgPlayer, fallbackFlag);
                if (fallbackState == StateFlag.State.ALLOW) {
                    return true;
                } else if (fallbackState == StateFlag.State.DENY) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean canInteractWithFlag(Player player, Location location, StateFlag customFlag) {
        return canInteractWithFlag(player, location, customFlag, null);
    }

    private boolean canBuildWithFlag(Player player, Location location, StateFlag primaryFlag, StateFlag fallbackFlag) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            ApplicableRegionSet regionSet = query.getApplicableRegions(weLoc);

            if (regionSet.getRegions().isEmpty() ||
                    regionSet.getRegions().stream().allMatch(region -> region.getId().equalsIgnoreCase("__global__"))) {

                if (primaryFlag != null) {
                    StateFlag.State primaryState = regionSet.queryState(wgPlayer, primaryFlag);
                    if (primaryState == StateFlag.State.ALLOW) {
                        return true;
                    }
                }

                if (fallbackFlag != null) {
                    StateFlag.State fallbackState = regionSet.queryState(wgPlayer, fallbackFlag);
                    return fallbackState == StateFlag.State.ALLOW;
                }

                return false;
            }

            boolean hasGeneralPermission = query.testBuild(weLoc, wgPlayer);

            if (!hasGeneralPermission) {
                return false;
            }

            if (primaryFlag != null) {
                StateFlag.State primaryState = regionSet.queryState(wgPlayer, primaryFlag);
                if (primaryState == StateFlag.State.ALLOW) {
                    return true;
                } else if (primaryState == StateFlag.State.DENY) {
                    return false;
                }
            }

            if (fallbackFlag != null) {
                StateFlag.State fallbackState = regionSet.queryState(wgPlayer, fallbackFlag);
                if (fallbackState == StateFlag.State.ALLOW) {
                    return true;
                } else if (fallbackState == StateFlag.State.DENY) {
                    return false;
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean canBuildWithFlag(Player player, Location location, StateFlag customFlag) {
        return canBuildWithFlag(player, location, customFlag, null);
    }
}