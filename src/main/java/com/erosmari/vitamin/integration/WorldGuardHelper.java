package com.erosmari.vitamin.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

public class WorldGuardHelper {

    public static boolean cannotInteract(Player player, Location location) {
        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = wgClass.getMethod("getInstance").invoke(null);

            Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Object query = container.getClass().getMethod("createQuery").invoke(container);

            Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Object wgPlugin = wgPluginClass.getMethod("inst").invoke(null);
            Object wrappedPlayer = wgPluginClass.getMethod("wrapPlayer", Player.class).invoke(wgPlugin, player);

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object weLoc = bukkitAdapterClass.getMethod("adapt", Location.class).invoke(null, location);

            Object regionSet = query.getClass()
                    .getMethod("getApplicableRegions", weLoc.getClass())
                    .invoke(query, weLoc);

            Collection<?> regions = (Collection<?>) regionSet.getClass()
                    .getMethod("getRegions")
                    .invoke(regionSet);

            boolean onlyGlobal = true;
            for (Object region : regions) {
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (!"__global__".equalsIgnoreCase(id)) {
                    onlyGlobal = false;
                    break;
                }
            }

            if (!onlyGlobal) {
                Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
                Object interactFlag = flagsClass.getField("INTERACT").get(null);


                return (Boolean) query.getClass()
                        .getMethod("testState", weLoc.getClass(), wrappedPlayer.getClass(), interactFlag.getClass())
                        .invoke(query, weLoc, wrappedPlayer, interactFlag);
            } else {
                return false;
            }

        } catch (Exception e) {
            return false;
        }
    }
}