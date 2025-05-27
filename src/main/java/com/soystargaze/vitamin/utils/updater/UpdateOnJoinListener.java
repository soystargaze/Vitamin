package com.soystargaze.vitamin.utils.updater;

import com.soystargaze.vitamin.config.ConfigHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

public class UpdateOnJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!ConfigHandler.getConfig().getBoolean("update-check", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isOp()) {
            UpdateChecker.checkForUpdates(player);
        }
    }

}