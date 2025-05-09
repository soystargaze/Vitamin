package com.soystargaze.vitamin.utils.updater;

import com.soystargaze.vitamin.Vitamin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class UpdateOnFullLoad implements Listener {

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.STARTUP) {
            Bukkit.getScheduler().runTaskAsynchronously(
                    Vitamin.getInstance(),
                    UpdateChecker::checkForUpdates
            );
        }
    }
}