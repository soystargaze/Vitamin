package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CropProtectionModule implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            if (event.getClickedBlock() != null) {
                Player player = event.getPlayer();
                if (!player.hasPermission("vitamin.module.crop_protection") ||
                        !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.crop_protection")) {
                    return;
                }
                Material type = event.getClickedBlock().getType();
                if (isCrop(type) || type == Material.FARMLAND) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, PUMPKIN, MELON -> true;
            default -> false;
        };
    }
}