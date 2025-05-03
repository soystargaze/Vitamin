package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.modules.ModuleManager;
import com.soystargaze.vitamin.modules.core.CustomRecipesModule;
import com.soystargaze.vitamin.utils.TranslationHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ModuleCommand implements CommandExecutor, TabCompleter {

    private final Vitamin plugin;
    private final ModuleManager moduleManager;

    public ModuleCommand(Vitamin plugin) {
        this.plugin = plugin;
        this.moduleManager = plugin.getModuleManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vitamin.module")) {
            sendTranslatedMessage(sender, "commands.module.no_permission");
            return true;
        }

        if (args.length != 2) {
            sendTranslatedMessage(sender, "commands.module.usage");
            return true;
        }

        String moduleName = args[0];
        String stateArg = args[1];
        boolean enable;

        if (stateArg.equalsIgnoreCase("enable")) {
            enable = true;
        } else if (stateArg.equalsIgnoreCase("disable")) {
            enable = false;
        } else {
            sendTranslatedMessage(sender, "commands.module.usage");
            return true;
        }

        String key = moduleName.startsWith("module.") ? moduleName : "module." + moduleName;

        if (!plugin.getConfig().contains(key)) {
            sendTranslatedMessage(sender, "commands.module.not_found", key);
            return true;
        }

        plugin.getConfig().set(key, enable);
        plugin.saveConfig();

        if (key.equalsIgnoreCase("module.custom_recipes") && !enable) {
            Object moduleInstance = moduleManager.getModule("custom_recipes");
            if (moduleInstance instanceof CustomRecipesModule) {
                ((CustomRecipesModule) moduleInstance).unregisterRecipes();
            }
        }

        moduleManager.reloadModules();

        sendTranslatedMessage(sender, "commands.module.changed", key, (enable ? "enabled" : "disabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (plugin.getConfig().contains("module")) {
                Set<String> keys = Objects.requireNonNull(plugin.getConfig().getConfigurationSection("module")).getKeys(false);
                for (String key : keys) {
                    suggestions.add("module." + key);
                }
            } else {
                Set<String> keys = plugin.getConfig().getKeys(false);
                for (String key : keys) {
                    if (key.startsWith("module.")) {
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

    private void sendTranslatedMessage(CommandSender sender, String key, Object... args) {
        Component messageComponent = TranslationHandler.getPlayerMessage(key, args);
        String message = LegacyComponentSerializer.legacyAmpersand().serialize(messageComponent);
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
        sender.sendMessage(formattedMessage);
    }
}