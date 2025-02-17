package com.erosmari.vividplus.commands;

import com.erosmari.vividplus.VividPlus;
import com.erosmari.vividplus.modules.ModuleManager;
import com.erosmari.vividplus.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

@SuppressWarnings("UnstableApiUsage")
public class ModuleCommand {

    private final VividPlus plugin;
    private final ModuleManager moduleManager;

    public ModuleCommand(VividPlus plugin) {
        this.plugin = plugin;
        this.moduleManager = plugin.getModuleManager();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(VividPlus plugin) {
        return Commands.literal("module")
                .requires(source -> source.getSender().hasPermission("vividplus.module"))
                .then(Commands.argument("module", StringArgumentType.word())
                        .then(Commands.argument("state", StringArgumentType.word())
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