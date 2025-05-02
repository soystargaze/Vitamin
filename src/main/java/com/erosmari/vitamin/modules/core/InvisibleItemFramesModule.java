package com.erosmari.vitamin.modules.core;

import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class InvisibleItemFramesModule implements Listener {

    public InvisibleItemFramesModule() {
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame itemFrame)) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("vitamin.module.invisible_item_frames") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.invisible_item_frames")) {
            return;
        }

        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            itemFrame.setVisible(!itemFrame.isVisible());
            event.setCancelled(true);
        }
    }
}