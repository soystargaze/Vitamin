package com.soystargaze.vitamin.modules;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Function;

public record ModuleDef(
        String configPath,
        Function<JavaPlugin, Listener> coreFactory,
        Function<JavaPlugin, Listener> paperFactory
) {
    public Listener create(JavaPlugin plugin, boolean isPaper) {
        if (isPaper && paperFactory != null) {
            return paperFactory.apply(plugin);
        }
        return coreFactory.apply(plugin);
    }
}