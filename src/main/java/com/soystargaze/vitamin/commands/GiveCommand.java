package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.utils.text.TextHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GiveCommand implements CommandExecutor, TabCompleter {

    private final Vitamin plugin;

    public GiveCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!sender.hasPermission("vitamin.usegive")) {
            sendToSender(sender, "commands.give.no_permission");
            return true;
        }

        if (plugin.getModuleManager().getModule("module.waystone") == null) {
            sendToSender(sender, "commands.give.module_disabled");
            return true;
        }

        if (args.length < 2) {
            sendToSender(sender, "commands.give.usage");
            return true;
        }

        String target = args[0];
        String itemType = args[1];

        if (!itemType.equalsIgnoreCase("waystone_core")) {
            sendToSender(sender, "commands.give.invalid_item");
            return true;
        }

        int quantity = 1;
        if (args.length >= 3) {
            try {
                quantity = Integer.parseInt(args[2]);
                if (quantity <= 0) {
                    sendToSender(sender, "commands.give.invalid_quantity");
                    return true;
                }
            } catch (NumberFormatException e) {
                sendToSender(sender, "commands.give.invalid_quantity");
                return true;
            }
        }

        ItemStack waystoneCore = plugin.getModuleManager().getWaystoneCore();
        if (waystoneCore == null) {
            sendToSender(sender, "commands.give.failed_to_create_item");
            return true;
        }

        waystoneCore.setAmount(quantity);

        if (target.equalsIgnoreCase("all")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                giveItemToPlayer(player, waystoneCore.clone());
            }
            sendToSender(sender, "commands.give.given_to_all", String.valueOf(quantity));
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player == null) {
                sendToSender(sender, "commands.give.player_not_found", target);
                return true;
            }
            giveItemToPlayer(player, waystoneCore);
            sendToSender(sender, "commands.give.given_to_player", player.getName(), String.valueOf(quantity));
        }

        return true;
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {
        if (!sender.hasPermission("vitamin.give")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList());
            return suggestions;
        } else if (args.length == 2) {
            return List.of("waystone_core");
        } else if (args.length == 3) {
            return List.of("1", "5", "10", "64");
        }

        return new ArrayList<>();
    }

    private void sendToSender(CommandSender sender, String key, Object... args) {
        Object msg = TextHandler.get().getMessage(key, args);
        if (msg instanceof Component comp) {
            sender.sendMessage(comp);
        } else {
            sender.sendMessage(msg.toString());
        }
    }
}