package com.soystargaze.vitamin.integration;

import com.github.sachin.lootin.utils.ChestUtils;
import com.github.sachin.lootin.utils.ContainerType;
import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class LootinIntegrationHandler {

    public LootinIntegrationHandler(JavaPlugin plugin) {
    }

    public boolean isLootinContainer(BlockState state) {
        try {
            if (state instanceof Chest) {
                if (ChestUtils.isLootinContainer(null, state, ContainerType.CHEST)
                        || ChestUtils.isLootinContainer(null, state, ContainerType.DOUBLE_CHEST)) {
                    return true;
                }
            }
            else if (state instanceof Barrel) {
                if (ChestUtils.isLootinContainer(null, state, ContainerType.BARREL)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            TextHandler.get().logTranslated("carry_on.error_lootin_container", t.getMessage());
        }
        return false;
    }

    public boolean isLootinContainer(Entity entity) {
        try {
            return ChestUtils.isLootinContainer(entity, null, ContainerType.MINECART);
        } catch (Throwable t) {
            TextHandler.get().logTranslated("carry_on.error_lootin_container", t.getMessage());
        }
        return false;
    }
}