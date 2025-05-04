package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.modules.ModuleManager;
import com.soystargaze.vitamin.modules.core.CustomRecipesModule;
import com.soystargaze.vitamin.utils.LoggingUtils;
import com.soystargaze.vitamin.utils.TranslationHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final Vitamin plugin;

    public ReloadCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TranslationHandler.getPlayerMessage("commands.pmodule.player_only"));
            return true;
        }

        if (!sender.hasPermission("vitamin.reload")) {
            LoggingUtils.sendMessage(player, "commands.reload.no_permission");
            return true;
        }

        try {
            plugin.reloadConfig();
            ConfigHandler.reload();

            ModuleManager moduleManager = plugin.getModuleManager();

            boolean customEnabled = plugin.getConfig().getBoolean("module.custom_recipes", true);
            if (!customEnabled) {
                Listener mod = moduleManager.getModule("custom_recipes");
                if (mod instanceof CustomRecipesModule) {
                    ((CustomRecipesModule) mod).unregisterRecipes();
                }
            }

            moduleManager.reloadModules();

            int loadedTranslations = reloadTranslations();

            LoggingUtils.sendMessage(player, "commands.reload.success", loadedTranslations);
        } catch (Exception e) {
            LoggingUtils.sendMessage(player, "commands.reload.error");
            LoggingUtils.logTranslated("commands.reload.error", e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        return new ArrayList<>();
    }

    private int reloadTranslations() {
        TranslationHandler.clearTranslations();
        TranslationHandler.loadTranslations(plugin, plugin.getConfig().getString("language", "en_us"));
        return TranslationHandler.getLoadedTranslationsCount();
    }
}