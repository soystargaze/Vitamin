package com.erosmari.vividplus.commands;

import com.erosmari.vividplus.VividPlus;
import com.erosmari.vividplus.config.ConfigHandler;
import com.erosmari.vividplus.modules.ModuleManager;
import com.erosmari.vividplus.utils.LoggingUtils;
import com.erosmari.vividplus.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class ReloadCommand {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;

    public ReloadCommand(VividPlus plugin) {
        this.plugin = plugin;
        this.moduleManager = plugin.getModuleManager();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(VividPlus plugin) {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("vividplus.reload"))
                .executes(context -> {
                    new ReloadCommand(plugin).execute(context.getSource());
                    return 1;
                });
    }

    public void execute(CommandSourceStack source) {
        try {
            reloadConfig();
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