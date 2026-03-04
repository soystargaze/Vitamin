package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PModuleCommand implements CommandExecutor, TabCompleter {

    private final Vitamin plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public PModuleCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            sendToSender(sender, "commands.pmodule.player_only");
            return true;
        }

        if (!player.hasPermission("vitamin.use.pmodule")) {
            TextHandler.get().sendMessage(player, "commands.pmodule.no_pmodule_permission");
            return true;
        }

        if (args.length == 0) {
            openPlayerGUI(player);
            return true;
        }

        if (args.length != 2) {
            TextHandler.get().sendMessage(player, "commands.pmodule.usage");
            return true;
        }

        togglePersonalModule(player, args[0], args[1]);
        return true;
    }

    private void togglePersonalModule(Player player, String moduleName, String stateArg) {
        String configKey = "module." + moduleName;
        String permissionKey = "vitamin.module." + moduleName;

        if (!plugin.getConfig().getBoolean(configKey, false)) {
            TextHandler.get().sendMessage(player, "commands.pmodule.module_not_active", moduleName);
            return;
        }

        if (!player.hasPermission(permissionKey)) {
            TextHandler.get().sendMessage(player, "commands.pmodule.no_module_permission", moduleName);
            return;
        }

        boolean enable;
        if (stateArg.equalsIgnoreCase("enable")) {
            enable = true;
        } else if (stateArg.equalsIgnoreCase("disable")) {
            enable = false;
        } else {
            TextHandler.get().sendMessage(player, "commands.pmodule.usage");
            return;
        }

        DatabaseHandler.setModuleEnabledForPlayer(player.getUniqueId(), configKey, enable);
        boolean newState = DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), configKey);
        TextHandler.get().sendMessage(player, "commands.pmodule.changed", moduleName, newState ? "enabled" : "disabled");
    }

    private void openPlayerGUI(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("module");
        if (section == null) return;

        List<String> activeModules = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            if (section.getBoolean(key) && player.hasPermission("vitamin.module." + key)) {
                activeModules.add(key);
            }
        }

        if (activeModules.isEmpty()) {
            TextHandler.get().sendMessage(player, "commands.pmodule.no_available_modules");
            return;
        }

        int size = (int) Math.ceil(activeModules.size() / 9.0) * 9;
        size = Math.max(9, Math.min(54, size));

        String titleRaw = ConfigHandler.getString("gui.module_player.title", 
                "<gradient:#FFA500:#FFFF00><bold>Vitamin+</bold></gradient> <gray>| <white>Your Settings");

        Gui gui = Gui.gui()
                .title(parseToComponent(titleRaw))
                .rows(size / 9)
                .disableAllInteractions()
                .create();

        String enabledMat = ConfigHandler.getString("gui.module_player.enabled-item", "LIME_STAINED_GLASS_PANE");
        String disabledMat = ConfigHandler.getString("gui.module_player.disabled-item", "RED_STAINED_GLASS_PANE");
        String nameFormat = ConfigHandler.getString("gui.module_player.item.display-name", "<gray>Module: %name%");
        String statusOn = ConfigHandler.getString("gui.module_player.item.status-on", "<green>ON");
        String statusOff = ConfigHandler.getString("gui.module_player.item.status-off", "<red>OFF");
        List<String> loreTemplate = ConfigHandler.getStringList("gui.module_player.item.lore");

        for (String key : activeModules) {
            String configKey = "module." + key;
            boolean isPlayerEnabled = DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), configKey);
            
            Material material;
            try {
                material = Material.valueOf(isPlayerEnabled ? enabledMat : disabledMat);
            } catch (Exception e) {
                material = isPlayerEnabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.displayName(parseToComponent(nameFormat.replace("%name%", formatName(key))));
                
                List<Component> lore = new ArrayList<>();
                for (String line : loreTemplate) {
                    lore.add(parseToComponent(line.replace("%status%", isPlayerEnabled ? statusOn : statusOff)));
                }
                
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            gui.addItem(new GuiItem(item, event -> {
                togglePersonalModule(player, key, isPlayerEnabled ? "disable" : "enable");
                openPlayerGUI(player); 
            }));
        }

        gui.open(player);
    }

    private Component parseToComponent(String text) {
        if (text == null) return Component.empty();
        if (text.contains("&") || text.contains("§")) {
            return legacySerializer.deserialize(text);
        }
        return MiniMessage.miniMessage().deserialize(text);
    }

    private String formatName(String key) {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (plugin.getConfig().contains("module")) {
                Set<String> keys = Objects.requireNonNull(plugin.getConfig().getConfigurationSection("module")).getKeys(false);
                for (String k : keys) {
                    if (plugin.getConfig().getBoolean("module." + k, true)) {
                        suggestions.add(k);
                    }
                }
            }
        }
        else if (args.length == 2) {
            suggestions.add("enable");
            suggestions.add("disable");
        }

        return suggestions;
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