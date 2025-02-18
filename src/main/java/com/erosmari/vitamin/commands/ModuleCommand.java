package com.erosmari.vitamin.commands;

import com.erosmari.vitamin.Vitamin;
import com.erosmari.vitamin.modules.ModuleManager;
import com.erosmari.vitamin.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class ModuleCommand {

    private final Vitamin plugin;
    private final ModuleManager moduleManager;

    public ModuleCommand(Vitamin plugin) {
        this.plugin = plugin;
        this.moduleManager = plugin.getModuleManager();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(Vitamin plugin) {
        return Commands.literal("module")
                .requires(source -> source.getSender().hasPermission("vitamin.module"))
                .then(Commands.argument("module", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            if (plugin.getConfig().contains("module")) {
                                Set<String> keys = Objects.requireNonNull(plugin.getConfig().getConfigurationSection("module")).getKeys(false);
                                for (String key : keys) {
                                    builder.suggest("module." + key);
                                }
                            } else {
                                Set<String> keys = plugin.getConfig().getKeys(false);
                                for (String key : keys) {
                                    if (key.startsWith("module.")) {
                                        builder.suggest(key);
                                    }
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("state", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("enable");
                                    builder.suggest("disable");
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String moduleName = StringArgumentType.getString(context, "module");
                                    String stateArg = StringArgumentType.getString(context, "state");
                                    new ModuleCommand(plugin).execute(context.getSource(), moduleName, stateArg);
                                    return 1;
                                })
                        )
                );
    }

    public void execute(CommandSourceStack source, String moduleName, String stateArg) {
        boolean enable;
        if (stateArg.equalsIgnoreCase("enable")) {
            enable = true;
        } else if (stateArg.equalsIgnoreCase("disable")) {
            enable = false;
        } else {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("commands.module.usage"));
            return;
        }

        String key = moduleName.startsWith("module.") ? moduleName : "module." + moduleName;

        if (!plugin.getConfig().contains(key)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("commands.module.not_found", key));
            return;
        }

        plugin.getConfig().set(key, enable);
        plugin.saveConfig();

        moduleManager.reloadModules();

        source.getSender().sendMessage(TranslationHandler.getPlayerMessage("commands.module.changed", key, (enable ? "enabled" : "disabled")));
    }
}