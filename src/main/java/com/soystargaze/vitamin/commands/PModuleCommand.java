package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import net.kyori.adventure.text.Component;
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
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            sendToSender(sender, "commands.pmodule.player_only");
            return true;
        }

        if (!player.hasPermission("vitamin.use.pmodule")) {
            TextHandler.get().sendMessage(player, "commands.pmodule.no_pmodule_permission");
            return true;
        }

        if (args.length != 2) {
            TextHandler.get().sendMessage(player, "commands.pmodule.usage");
            return true;
        }

        String moduleName = args[0];
        String configKey = "module." + moduleName;         // Para configuración y base de datos
        String permissionKey = "vitamin.module." + moduleName; // Para permisos

        if (!plugin.getConfig().getBoolean(configKey, false)) {
            TextHandler.get().sendMessage(player, "commands.pmodule.module_not_active", moduleName);
            return true;
        }

        if (!player.hasPermission(permissionKey)) {
            TextHandler.get().sendMessage(player, "commands.pmodule.no_module_permission", moduleName);
            return true;
        }

        String stateArg = args[1];
        boolean enable;
        if (stateArg.equalsIgnoreCase("enable")) {
            enable = true;
        } else if (stateArg.equalsIgnoreCase("disable")) {
            enable = false;
        } else {
            TextHandler.get().sendMessage(player, "commands.pmodule.usage");
            return true;
        }

        DatabaseHandler.setModuleEnabledForPlayer(player.getUniqueId(), configKey, enable);
        boolean newState = DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), configKey);
        TextHandler.get().sendMessage(player, "commands.pmodule.changed", moduleName, newState ? "enabled" : "disabled");

        return true;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (plugin.getConfig().contains("module")) {
                Set<String> keys = Objects.requireNonNull(plugin.getConfig().getConfigurationSection("module")).getKeys(false);
                for (String k : keys) {
                    if (plugin.getConfig().getBoolean("module." + k, true)) {
                        suggestions.add(k);
                    }
                }
            }
        }
        else if (args.length == 2) {
            suggestions.add("enable");
            suggestions.add("disable");
        }

        return suggestions;
    }

    private void sendToSender(CommandSender sender, String key, Object... args) {
        Object msg = TextHandler.get().getMessage(key, args);
        if (msg instanceof Component comp) {
            sender.sendMessage(comp);
        } else {
            sender.sendMessage(msg.toString());
        }
    }
}