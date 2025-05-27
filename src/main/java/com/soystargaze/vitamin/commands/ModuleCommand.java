package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.modules.ModuleManager;
import com.soystargaze.vitamin.modules.core.CustomRecipesModule;
import com.soystargaze.vitamin.utils.text.TextHandler;
import net.kyori.adventure.text.Component;
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

    public ModuleCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!sender.hasPermission("vitamin.use.module")) {
            sendToSender(sender, "commands.module.no_permission");
            return true;
        }

        if (args.length != 2) {
            sendToSender(sender, "commands.module.usage");
            return true;
        }

        try {
            ModuleManager moduleManager = plugin.getModuleManager();
            if (moduleManager == null) {
                plugin.getLogger().warning("ModuleManager is null, cannot reload modules.");
                return true;
            }

            String moduleName = args[0];
            String stateArg   = args[1];
            boolean enable;

            if (stateArg.equalsIgnoreCase("enable")) {
                enable = true;
            } else if (stateArg.equalsIgnoreCase("disable")) {
                enable = false;
            } else {
                sendToSender(sender, "commands.module.usage");
                return true;
            }

            String key = "module." + moduleName;

            if (!plugin.getConfig().contains(key)) {
                sendToSender(sender, "commands.module.not_found", moduleName);
                return true;
            }

            plugin.getConfig().set(key, enable);
            plugin.saveConfig();

            if ("module.custom_recipes".equalsIgnoreCase(key) && !enable) {
                Object mod = moduleManager.getModule("custom_recipes");
                if (mod instanceof CustomRecipesModule) {
                    ((CustomRecipesModule) mod).unregisterRecipes();
                }
            }

            moduleManager.reloadModules();
            sendToSender(sender, "commands.module.changed", moduleName, enable ? "enabled" : "disabled");

        } catch (Exception e) {
            sendToSender(sender, "commands.reload.error");
            TextHandler.get().logTranslated("commands.reload.error", e.getMessage());
            e.printStackTrace();
        }
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
                Set<String> keys = Objects
                        .requireNonNull(plugin.getConfig().getConfigurationSection("module"))
                        .getKeys(false);
                suggestions.addAll(keys);
            }
        } else if (args.length == 2) {
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