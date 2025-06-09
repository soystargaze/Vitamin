package com.soystargaze.vitamin.integration;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.LandWorld;
import me.angeschossen.lands.api.player.LandPlayer;
import me.angeschossen.lands.api.flags.type.Flags;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LandsIntegrationHandler {

    private final LandsIntegration landsApi;

    public LandsIntegrationHandler(JavaPlugin plugin) {
        this.landsApi = LandsIntegration.of(plugin);
    }

    public boolean canInteract(Player player, Location location) {
        try {
            LandWorld landWorld = landsApi.getWorld(location.getWorld());
            if (landWorld != null) {
                LandPlayer landPlayer = landsApi.getLandPlayer(player.getUniqueId());
                if (landPlayer == null) {
                    return false;
                }
                return landWorld.hasRoleFlag(
                        landPlayer,
                        location,
                        Flags.INTERACT_GENERAL,
                        null,
                        false
                );
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean canBreak(Player player, Location location, Material material) {
        try {
            LandWorld landWorld = landsApi.getWorld(location.getWorld());
            if (landWorld != null) {
                LandPlayer landPlayer = landsApi.getLandPlayer(player.getUniqueId());
                if (landPlayer == null) {
                    return false;
                }
                return landWorld.hasRoleFlag(
                        landPlayer,
                        location,
                        me.angeschossen.lands.api.flags.type.Flags.BLOCK_BREAK,
                        material,
                        false
                );
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}