package com.erosmari.vividplus.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class InvisibleItemFramesModule implements Listener {

    public InvisibleItemFramesModule(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame itemFrame)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            itemFrame.setVisible(!itemFrame.isVisible());

            event.setCancelled(true);
        }
    }
}