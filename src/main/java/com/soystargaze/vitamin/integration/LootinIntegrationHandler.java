package com.soystargaze.vitamin.integration;

import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class LootinIntegrationHandler {

    public LootinIntegrationHandler(JavaPlugin plugin) {
    }

    public boolean isLootinContainer(BlockState state) {
        // Integration disabled due to missing API
        return false;
    }

    public boolean isLootinContainer(Entity entity) {
        // Integration disabled due to missing API
        return false;
    }
}