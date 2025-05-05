package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.utils.text.TextHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        // 1) Solo jugadores
        if (!(sender instanceof Player player)) {
            sendToSender(sender, "commands.pmodule.player_only");
            return true;
        }

        // 2) Permiso base
        if (!player.hasPermission("vitamin.use")) {
            TextHandler.get().sendMessage(player, "commands.no_permission");
            return true;
        }

        // 3) Sin args → usage
        if (args.length == 0) {
            TextHandler.get().sendMessage(player, "commands.usage");
            return true;
        }

        // 4) Dispatch al subcomando
        String sub = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (sub) {
            case "reload"  -> reloadCommand.onCommand(sender, command, label, subArgs);
            case "module"  -> moduleCommand.onCommand(sender, command, label, subArgs);
            case "pmodule" -> pModuleCommand.onCommand(sender, command, label, subArgs);
            default -> {
                TextHandler.get().sendMessage(player, "commands.usage");
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
            if (sender.hasPermission("vitamin.reload"))  subs.add("reload");
            if (sender.hasPermission("vitamin.module"))  subs.add("module");
            if (sender.hasPermission("vitamin.pmodule")) subs.add("pmodule");
            return subs;
        }
        if (args.length > 1) {
            String sub = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return switch (sub) {
                case "reload"  -> reloadCommand.onTabComplete(sender, command, alias, subArgs);
                case "module"  -> moduleCommand.onTabComplete(sender, command, alias, subArgs);
                case "pmodule" -> pModuleCommand.onTabComplete(sender, command, alias, subArgs);
                default        -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    /**
     * Envía un mensaje al sender (Player o consola) usando TextHandler.
     * Comprueba si devuelve Component o String.
     */
    private void sendToSender(CommandSender sender, String key, Object... args) {
        Object msg = TextHandler.get().getMessage(key, args);
        if (msg instanceof Component comp) {
            sender.sendMessage(comp);
        } else {
            sender.sendMessage(msg.toString());
        }
    }
}