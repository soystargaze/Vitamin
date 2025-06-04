package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.modules.core.WaystoneModule;
import com.soystargaze.vitamin.modules.paper.PaperWaystoneModule;
import com.soystargaze.vitamin.utils.text.TextHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClearCommand implements CommandExecutor, TabCompleter {

    private final Vitamin plugin;
    private static final Map<String, Long> pendingClearances = new HashMap<>();

    public ClearCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("vitamin.use.clear")) {
            sendToSender(sender, "commands.no_permission");
            return true;
        }

        if (args.length == 0) {
            sendToSender(sender, "commands.clear.usage");
            return true;
        }

        if (args[0].equalsIgnoreCase("waystones")) {
            if (args.length == 1) {
                String senderId = getSenderId(sender);
                pendingClearances.put(senderId, System.currentTimeMillis());
                sendToSender(sender, "commands.clear.confirm");
                return true;
            } else if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
                String senderId = getSenderId(sender);
                Long requestTime = pendingClearances.get(senderId);
                if (requestTime != null && System.currentTimeMillis() - requestTime < 30000) {
                    pendingClearances.remove(senderId);
                    clearAllWaystones(sender);
                    sendToSender(sender, "commands.clear.success");
                } else {
                    sendToSender(sender, "commands.clear.timeout");
                    pendingClearances.remove(senderId);
                }
                return true;
            } else {
                sendToSender(sender, "commands.clear.usage");
                return true;
            }
        } else {
            sendToSender(sender, "commands.clear.usage");
            return true;
        }
    }

    private void clearAllWaystones(CommandSender sender) {
        Object module = plugin.getModuleManager().getModule("waystone");
        if (module != null) {
            if (module instanceof PaperWaystoneModule paperModule) {
                paperModule.clearWaystones(); // Paper-specific module
            } else if (module instanceof WaystoneModule spigotModule) {
                spigotModule.clearWaystones(); // Spigot-specific module
            } else {
                sendToSender(sender, "commands.clear.error");
            }
        } else {
            sendToSender(sender, "commands.clear.error");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return Collections.singletonList("waystones");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("waystones")) {
            return Collections.singletonList("confirm");
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

    private String getSenderId(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId().toString();
        } else {
            return "CONSOLE";
        }
    }
}