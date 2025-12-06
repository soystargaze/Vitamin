package com.soystargaze.vitamin.modules;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Function;

public record ModuleDef(
        String configPath,
        Function<JavaPlugin, Listener> factory
) {
    public Listener create(JavaPlugin plugin) {
        return factory.apply(plugin);
    }
}