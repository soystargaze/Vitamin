package com.erosmari.vitamin.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LandsHelper {

    public static boolean cannotInteract(Player player, Location location, JavaPlugin plugin) {
        try {
            Class<?> landsIntegrationClass = Class.forName("me.angeschossen.lands.api.LandsIntegration");
            Object landsApi = landsIntegrationClass.getMethod("of", org.bukkit.plugin.Plugin.class).invoke(null, plugin);

            Object landWorld = landsIntegrationClass.getMethod("getWorld", location.getWorld().getClass()).invoke(landsApi, location.getWorld());
            if (landWorld != null) {
                Object landPlayer = landsIntegrationClass.getMethod("getLandPlayer", java.util.UUID.class).invoke(landsApi, player.getUniqueId());
                if (landPlayer == null) {
                    return true;
                }
                Class<?> landsFlagsClass = Class.forName("me.angeschossen.lands.api.flags.type.Flags");
                Object interactGeneralFlag = landsFlagsClass.getField("INTERACT_GENERAL").get(null);
                return (Boolean) landWorld.getClass()
                        .getMethod("hasRoleFlag", landPlayer.getClass(), Location.class, interactGeneralFlag.getClass(), Object.class, boolean.class)
                        .invoke(landWorld, landPlayer, location, interactGeneralFlag, null, false);
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}