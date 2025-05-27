package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.modules.ModuleManager;
import com.soystargaze.vitamin.modules.core.CustomRecipesModule;
import com.soystargaze.vitamin.utils.text.TextHandler;
import com.soystargaze.vitamin.utils.text.modern.ModernTranslationHandler;
import com.soystargaze.vitamin.utils.text.legacy.LegacyTranslationHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final Vitamin plugin;

    public ReloadCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!sender.hasPermission("vitamin.use.reload")) {
            sendToSender(sender, "commands.reload.no_permission");
            return true;
        }

        try {
            plugin.reloadConfig();
            ConfigHandler.reload();

            ModuleManager moduleManager = plugin.getModuleManager();
            boolean customEnabled = plugin.getConfig().getBoolean("module.custom_recipes", true);
            if (!customEnabled) {
                Object mod = moduleManager.getModule("custom_recipes");
                if (mod instanceof CustomRecipesModule) {
                    ((CustomRecipesModule) mod).unregisterRecipes();
                }
            }

            moduleManager.reloadModules();

            plugin.reregisterCommandListeners();

            int loadedTranslations = reloadTranslations();
            sendToSender(sender, "commands.reload.success", loadedTranslations);

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
        return new ArrayList<>();
    }

    private int reloadTranslations() {
        String language = plugin.getConfig().getString("language", "en_us");
        if (Bukkit.getServer().getName().equalsIgnoreCase("Paper")) {
            ModernTranslationHandler.clearTranslations();
            ModernTranslationHandler.loadTranslations(plugin, language);
            return ModernTranslationHandler.getLoadedTranslationsCount();
        } else {
            LegacyTranslationHandler.clearTranslations();
            LegacyTranslationHandler.loadTranslations(plugin, language);
            return LegacyTranslationHandler.getLoadedTranslationsCount();
        }
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