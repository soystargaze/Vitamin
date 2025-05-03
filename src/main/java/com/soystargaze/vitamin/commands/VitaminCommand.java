package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.utils.TranslationHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VitaminCommand implements CommandExecutor, TabCompleter {

    private final ModuleCommand moduleCommand;
    private final PModuleCommand pModuleCommand;
    private final ReloadCommand reloadCommand;

    public VitaminCommand(Vitamin plugin) {
        this.moduleCommand = new ModuleCommand(plugin);
        this.pModuleCommand = new PModuleCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("vitamin.use")) {
            sendTranslatedMessage(sender, "commands.no_permission");
            return true;
        }

        if (args.length == 0) {
            sendTranslatedMessage(sender, "commands.usage");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (subCommand) {
            case "reload" -> reloadCommand.onCommand(sender, command, label, subArgs);
            case "module" -> moduleCommand.onCommand(sender, command, label, subArgs);
            case "pmodule" -> pModuleCommand.onCommand(sender, command, label, subArgs);
            default -> {
                sendTranslatedMessage(sender, "commands.usage");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("vitamin.reload")) {
                subCommands.add("reload");
            }
            if (sender.hasPermission("vitamin.module")) {
                subCommands.add("module");
            }
            if (sender.hasPermission("vitamin.pmodule")) {
                subCommands.add("pmodule");
            }
            return subCommands;
        } else if (args.length > 1) {
            String subCommand = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return switch (subCommand) {
                case "reload" -> reloadCommand.onTabComplete(sender, command, alias, subArgs);
                case "module" -> moduleCommand.onTabComplete(sender, command, alias, subArgs);
                case "pmodule" -> pModuleCommand.onTabComplete(sender, command, alias, subArgs);
                default -> new ArrayList<>();
            };
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    private void sendTranslatedMessage(CommandSender sender, String key, Object... args) {
        Component messageComponent = TranslationHandler.getPlayerMessage(key, args);
        String message = LegacyComponentSerializer.legacyAmpersand().serialize(messageComponent);
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
        sender.sendMessage(formattedMessage);
    }
}