package com.soystargaze.vitamin.modules;

import org.bukkit.event.Listener;

public interface CancellableModule extends Listener {
    void cancelTasks();
}