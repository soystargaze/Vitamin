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
    public static StateFlag VITAMIN_WAYSTONE;
    public static StateFlag VITAMIN_WAYSTONE_CREATE;
    public static StateFlag VITAMIN_WAYSTONE_USE;
    public static StateFlag VITAMIN_WAYSTONE_BREAK;

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
                VITAMIN_ENTITY = (StateFlag) existing;
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

        // Waystone flags
        try {
            StateFlag waystoneFlag = new StateFlag("vitamin-waystone", false);
            registry.register(waystoneFlag);
            VITAMIN_WAYSTONE = waystoneFlag;
            TextHandler.get().logTranslated("plugin.worldguard.waystone_flag_registered");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("vitamin-waystone");
            if (existing instanceof StateFlag) {
                VITAMIN_WAYSTONE = (StateFlag) existing;
                TextHandler.get().logTranslated("plugin.worldguard.waystone_flag_conflict_resolved");
            } else {
                TextHandler.get().logTranslated("plugin.worldguard.waystone_flag_conflict_error");
            }
        }

        try {
            StateFlag waystoneCreateFlag = new StateFlag("vitamin-waystone-create", false);
            registry.register(waystoneCreateFlag);
            VITAMIN_WAYSTONE_CREATE = waystoneCreateFlag;
            TextHandler.get().logTranslated("plugin.worldguard.waystone_create_flag_registered");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("vitamin-waystone-create");
            if (existing instanceof StateFlag) {
                VITAMIN_WAYSTONE_CREATE = (StateFlag) existing;
                TextHandler.get().logTranslated("plugin.worldguard.waystone_create_flag_conflict_resolved");
            } else {
                TextHandler.get().logTranslated("plugin.worldguard.waystone_create_flag_conflict_error");
            }
        }

        try {
            StateFlag waystoneUseFlag = new StateFlag("vitamin-waystone-use", false);
            registry.register(waystoneUseFlag);
            VITAMIN_WAYSTONE_USE = waystoneUseFlag;
            TextHandler.get().logTranslated("plugin.worldguard.waystone_use_flag_registered");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("vitamin-waystone-use");
            if (existing instanceof StateFlag) {
                VITAMIN_WAYSTONE_USE = (StateFlag) existing;
                TextHandler.get().logTranslated("plugin.worldguard.waystone_use_flag_conflict_resolved");
            } else {
                TextHandler.get().logTranslated("plugin.worldguard.waystone_use_flag_conflict_error");
            }
        }

        try {
            StateFlag waystoneBreakFlag = new StateFlag("vitamin-waystone-break", false);
            registry.register(waystoneBreakFlag);
            VITAMIN_WAYSTONE_BREAK = waystoneBreakFlag;
            TextHandler.get().logTranslated("plugin.worldguard.waystone_break_flag_registered");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("vitamin-waystone-break");
            if (existing instanceof StateFlag) {
                VITAMIN_WAYSTONE_BREAK = (StateFlag) existing;
                TextHandler.get().logTranslated("plugin.worldguard.waystone_break_flag_conflict_resolved");
            } else {
                TextHandler.get().logTranslated("plugin.worldguard.waystone_break_flag_conflict_error");
            }
        }
    }
}