package com.erosmari.vividplus.commands;

import com.erosmari.vividplus.VividPlus;
import com.erosmari.vividplus.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("UnstableApiUsage")
public class VividPlusCommandManager {

    private final VividPlus plugin;

    public VividPlusCommandManager(VividPlus plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        LifecycleEventManager<@org.jetbrains.annotations.NotNull Plugin> manager = plugin.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            registerCommand(commands, "vividplus");
            registerCommand(commands, "vivid");
            registerCommand(commands, "vi");
        });
    }

    private void registerCommand(Commands commands, String commandName) {
        commands.register(
                Commands.literal(commandName)
                        .requires(source -> source.getSender().hasPermission("vividplus.use"))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("commands.usage"));
                            return 1;
                        })
                        .then(ReloadCommand.register(plugin))
                        .build()
        );
    }
}