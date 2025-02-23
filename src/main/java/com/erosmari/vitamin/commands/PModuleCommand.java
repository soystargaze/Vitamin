package com.erosmari.vitamin.commands;

import com.erosmari.vitamin.Vitamin;
import com.erosmari.vitamin.database.DatabaseHandler;
import com.erosmari.vitamin.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.bukkit.entity.Player;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class PModuleCommand {

    private final Vitamin plugin;

    public PModuleCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(Vitamin plugin) {
        return Commands.literal("pmodule")
                .requires(source -> source.getSender().hasPermission("vitamin.pmodule"))
                .then(Commands.argument("module", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Set<String> keys = plugin.getConfig().getKeys(false);
                            for (String key : keys) {
                                if (key.startsWith("module.") && plugin.getConfig().getBoolean(key, true)) {
                                    builder.suggest(key);
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
                                    new PModuleCommand(plugin).execute(context.getSource(), moduleName, stateArg);
                                    return 1;
                                })
                        )
                );
    }

    public void execute(CommandSourceStack source, String moduleName, String stateArg) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("commands.pmodule.player_only"));
            return;
        }

        String key = moduleName.startsWith("module.") ? moduleName : "module." + moduleName;

        if (!plugin.getConfig().getBoolean(key, false)) {
            player.sendMessage(TranslationHandler.getPlayerMessage("commands.pmodule.module_not_active", key));
            return;
        }

        if (!player.hasPermission(key)) {
            player.sendMessage(TranslationHandler.getPlayerMessage("commands.pmodule.no_module_permission", key));
            return;
        }

        if (!player.hasPermission("vitamin.pmodule")) {
            player.sendMessage(TranslationHandler.getPlayerMessage("commands.pmodule.no_pmodule_permission"));
            return;
        }

        boolean enable;
        if (stateArg.equalsIgnoreCase("enable")) {
            enable = true;
        } else if (stateArg.equalsIgnoreCase("disable")) {
            enable = false;
        } else {
            player.sendMessage(TranslationHandler.getPlayerMessage("commands.pmodule.usage"));
            return;
        }

        DatabaseHandler.setModuleEnabledForPlayer(player.getUniqueId(), key, enable);
        player.sendMessage(TranslationHandler.getPlayerMessage("commands.pmodule.changed", key, (enable ? "enabled" : "disabled")));
    }
}