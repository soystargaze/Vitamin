package com.erosmari.vitamin.commands;

import com.erosmari.vitamin.Vitamin;
import com.erosmari.vitamin.config.ConfigHandler;
import com.erosmari.vitamin.modules.ModuleManager;
import com.erosmari.vitamin.modules.CustomRecipesModule;
import com.erosmari.vitamin.utils.LoggingUtils;
import com.erosmari.vitamin.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class ReloadCommand {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;

    public ReloadCommand(Vitamin plugin) {
        this.plugin = plugin;
        this.moduleManager = plugin.getModuleManager();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(Vitamin plugin) {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("vitamin.reload"))
                .executes(context -> {
                    new ReloadCommand(plugin).execute(context.getSource());
                    return 1;
                });
    }

    public void execute(CommandSourceStack source) {
        try {
            reloadConfig();
            if (!plugin.getConfig().getBoolean("module.custom_recipes", true)) {
                Listener mod = moduleManager.getModule("custom_recipes");
                if (mod instanceof CustomRecipesModule) {
                    ((CustomRecipesModule) mod).unregisterRecipes();
                }
            }
            int loadedTranslations = reloadTranslations();
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("commands.reload.success", loadedTranslations));
        } catch (Exception e) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("commands.reload.error"));
            LoggingUtils.logTranslated("commands.reload.error", e.getMessage());
        }
    }

    private void reloadConfig() {
        plugin.reloadConfig();
        ConfigHandler.reload();

        moduleManager.reloadModules();
    }

    private int reloadTranslations() {
        TranslationHandler.clearTranslations();
        TranslationHandler.loadTranslations(plugin, plugin.getConfig().getString("language", "en_us"));
        return TranslationHandler.getLoadedTranslationsCount();
    }
}