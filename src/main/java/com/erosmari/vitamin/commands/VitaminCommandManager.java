package com.erosmari.vitamin.commands;

import com.erosmari.vitamin.Vitamin;
import com.erosmari.vitamin.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("UnstableApiUsage")
public class VitaminCommandManager {

    private final Vitamin plugin;

    public VitaminCommandManager(Vitamin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        LifecycleEventManager<@org.jetbrains.annotations.NotNull Plugin> manager = plugin.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            registerCommand(commands, "vitamin");
            registerCommand(commands, "vita");
            registerCommand(commands, "vi");
        });
    }

    private void registerCommand(Commands commands, String commandName) {
        commands.register(
                Commands.literal(commandName)
                        .requires(source -> source.getSender().hasPermission("vitamin.use"))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("commands.usage"));
                            return 1;
                        })
                        .then(ReloadCommand.register(plugin))
                        .then(ModuleCommand.register(plugin))
                        .build()
        );
    }
}