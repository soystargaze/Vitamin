package com.soystargaze.vitamin.integration;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGuardFlags {

    public static StateFlag VITAMIN_ENTITY;
    public static StateFlag VITAMIN_CONTAINER;

    public static void registerFlags(JavaPlugin plugin) {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            StateFlag entityFlag = new StateFlag("vitamin-carryon-entity", false);
            registry.register(entityFlag);
            VITAMIN_ENTITY = entityFlag;
            TextHandler.get().logTranslated("plugin.worldguard.entity_flag_registered");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("vitamin-carryon-entity");
            if (existing instanceof StateFlag) {
                VITAMIN_CONTAINER = (StateFlag) existing;
                TextHandler.get().logTranslated("plugin.worldguard.entity_flag_conflict_resolved");
            } else {
                TextHandler.get().logTranslated("plugin.worldguard.entity_flag_conflict_error");
            }
        }

        try {
            StateFlag containerFlag = new StateFlag("vitamin-carryon-container", false);
            registry.register(containerFlag);
            VITAMIN_CONTAINER = containerFlag;
            TextHandler.get().logTranslated("plugin.worldguard.container_flag_registered");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("vitamin-carryon-container");
            if (existing instanceof StateFlag) {
                VITAMIN_CONTAINER = (StateFlag) existing;
                TextHandler.get().logTranslated("plugin.worldguard.container_flag_conflict_resolved");
            } else {
                TextHandler.get().logTranslated("plugin.worldguard.container_flag_conflict_error");
            }
        }
    }
}