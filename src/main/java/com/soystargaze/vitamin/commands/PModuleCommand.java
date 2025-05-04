package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.LoggingUtils;
import com.soystargaze.vitamin.utils.TranslationHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PModuleCommand implements CommandExecutor, TabCompleter {

    private final Vitamin plugin;

    public PModuleCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TranslationHandler.getPlayerMessage("commands.pmodule.player_only"));
            return true;
        }

        if (!player.hasPermission("vitamin.pmodule")) {
            LoggingUtils.sendMessage(player, "commands.pmodule.no_pmodule_permission");
            return true;
        }

        if (args.length != 2) {
            LoggingUtils.sendMessage(player, "commands.pmodule.usage");
            return true;
        }

        String moduleName = args[0];
        String stateArg = args[1];
        String key = moduleName.startsWith("module.") ? moduleName : "module." + moduleName;

        if (!plugin.getConfig().getBoolean(key, false)) {
            LoggingUtils.sendMessage(player, "commands.pmodule.module_not_active", key);
            return true;
        }

        if (!player.hasPermission(key)) {
            LoggingUtils.sendMessage(player, "commands.pmodule.no_module_permission", key);
            return true;
        }

        boolean enable;
        if (stateArg.equalsIgnoreCase("enable")) {
            enable = true;
        } else if (stateArg.equalsIgnoreCase("disable")) {
            enable = false;
        } else {
            LoggingUtils.sendMessage(player, "commands.pmodule.usage");
            return true;
        }

        DatabaseHandler.setModuleEnabledForPlayer(player.getUniqueId(), key, enable);

        boolean newState = DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), key);
        LoggingUtils.sendMessage(player, "commands.pmodule.changed", key, (newState ? "enabled" : "disabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (plugin.getConfig().contains("module")) {
                Set<String> keys = Objects.requireNonNull(plugin.getConfig().getConfigurationSection("module")).getKeys(false);
                for (String key : keys) {
                    if (plugin.getConfig().getBoolean("module." + key, true)) {
                        suggestions.add("module." + key);
                    }
                }
            } else {
                for (String key : plugin.getConfig().getKeys(false)) {
                    if (key.startsWith("module.") && plugin.getConfig().getBoolean(key, true)) {
                        suggestions.add(key);
                    }
                }
            }
        } else if (args.length == 2) {
            suggestions.add("enable");
            suggestions.add("disable");
        }

        return suggestions;
    }
}