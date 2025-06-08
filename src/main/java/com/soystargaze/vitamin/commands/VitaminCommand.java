package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.utils.text.TextHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VitaminCommand implements CommandExecutor, TabCompleter {

    private final ModuleCommand moduleCommand;
    private final PModuleCommand pModuleCommand;
    private final ReloadCommand reloadCommand;
    private final RestoreCommand restoreCommand;
    private final ClearCommand clearCommand;
    private final GiveCommand giveCommand;

    public VitaminCommand(Vitamin plugin) {
        this.moduleCommand = new ModuleCommand(plugin);
        this.pModuleCommand = new PModuleCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.restoreCommand = new RestoreCommand(plugin);
        this.clearCommand = new ClearCommand(plugin);
        this.giveCommand = new GiveCommand(plugin);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!sender.hasPermission("vitamin.use")) {
            sendToSender(sender, "commands.no_permission");
            return true;
        }

        if (args.length == 0) {
            sendToSender(sender, "commands.usage");
            return true;
        }

        String sub = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (sub) {
            case "reload"  -> reloadCommand.onCommand(sender, command, label, subArgs);
            case "module"  -> moduleCommand.onCommand(sender, command, label, subArgs);
            case "pmodule" -> pModuleCommand.onCommand(sender, command, label, subArgs);
            case "restore" -> restoreCommand.onCommand(sender, command, label, subArgs);
            case "clear"   -> clearCommand.onCommand(sender, command, label, subArgs);
            case "give"    -> giveCommand.onCommand(sender, command, label, subArgs);
            default -> {
                sendToSender(sender, "commands.usage");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("vitamin.use.reload"))  subs.add("reload");
            if (sender.hasPermission("vitamin.use.module"))  subs.add("module");
            if (sender.hasPermission("vitamin.use.pmodule")) subs.add("pmodule");
            if (sender.hasPermission("vitamin.use.restore")) subs.add("restore");
            if (sender.hasPermission("vitamin.use.clear"))   subs.add("clear");
            if (sender.hasPermission("vitamin.use.give"))    subs.add("give");
            return subs;
        }
        if (args.length > 1) {
            String sub = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return switch (sub) {
                case "reload"  -> reloadCommand.onTabComplete(sender, command, alias, subArgs);
                case "module"  -> moduleCommand.onTabComplete(sender, command, alias, subArgs);
                case "pmodule" -> pModuleCommand.onTabComplete(sender, command, alias, subArgs);
                case "restore" -> restoreCommand.onTabComplete(sender, command, alias, subArgs);
                case "clear"   -> clearCommand.onTabComplete(sender, command, alias, subArgs);
                case "give"    -> giveCommand.onTabComplete(sender, command, alias, subArgs);
                default        -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    private void sendToSender(CommandSender sender, String key, Object... args) {
        Object msg = TextHandler.get().getMessage(key, args);
        if (msg instanceof Component comp) {
            sender.sendMessage(comp);
        } else {
            sender.sendMessage(msg.toString());
        }
    }

    public RestoreCommand getRestoreCommand() {
        return restoreCommand;
    }
}