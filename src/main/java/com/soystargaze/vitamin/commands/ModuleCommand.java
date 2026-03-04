package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.modules.ModuleManager;
import com.soystargaze.vitamin.modules.core.CustomRecipesModule;
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

public class ModuleCommand implements CommandExecutor, TabCompleter {

    private final Vitamin plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public ModuleCommand(Vitamin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!sender.hasPermission("vitamin.use.module")) {
            sendToSender(sender, "commands.module.no_permission");
            return true;
        }

        if (args.length == 0 && sender instanceof Player player) {
            openAdminGUI(player);
            return true;
        }

        if (args.length != 2) {
            sendToSender(sender, "commands.module.usage");
            return true;
        }

        toggleModule(sender, args[0], args[1]);
        return true;
    }

    private void toggleModule(CommandSender sender, String moduleName, String stateArg) {
        try {
            ModuleManager moduleManager = plugin.getModuleManager();
            boolean enable;

            if (stateArg.equalsIgnoreCase("enable")) {
                enable = true;
            } else if (stateArg.equalsIgnoreCase("disable")) {
                enable = false;
            } else {
                sendToSender(sender, "commands.module.usage");
                return;
            }

            String key = "module." + moduleName;

            if (!plugin.getConfig().contains(key)) {
                sendToSender(sender, "commands.module.not_found", moduleName);
                return;
            }

            plugin.getConfig().set(key, enable);
            plugin.saveConfig();

            if ("module.custom_recipes".equalsIgnoreCase(key) && !enable) {
                Object mod = moduleManager.getModule("custom_recipes");
                if (mod instanceof CustomRecipesModule) {
                    ((CustomRecipesModule) mod).unregisterRecipes();
                }
            }

            moduleManager.reloadModules();
            sendToSender(sender, "commands.module.changed", moduleName, enable ? "enabled" : "disabled");

        } catch (Exception e) {
            sendToSender(sender, "commands.reload.error");
            e.printStackTrace();
        }
    }

    private void openAdminGUI(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("module");
        if (section == null) return;

        Set<String> moduleKeys = section.getKeys(false);
        int size = (int) Math.ceil(moduleKeys.size() / 9.0) * 9;
        size = Math.max(9, Math.min(54, size));

        String titleRaw = ConfigHandler.getString("gui.module_admin.title", 
                "<gradient:#FFA500:#FFFF00><bold>Vitamin+</bold></gradient> <gray>| <white>Module Admin");
        
        Gui gui = Gui.gui()
                .title(parseToComponent(titleRaw))
                .rows(size / 9)
                .disableAllInteractions()
                .create();

        String enabledMat = ConfigHandler.getString("gui.module_admin.enabled-item", "LIME_CONCRETE");
        String disabledMat = ConfigHandler.getString("gui.module_admin.disabled-item", "RED_CONCRETE");
        String nameFormat = ConfigHandler.getString("gui.module_admin.item.display-name", "<gray>Module: %name%");
        String statusEnabled = ConfigHandler.getString("gui.module_admin.item.status-enabled", "<green>ENABLED");
        String statusDisabled = ConfigHandler.getString("gui.module_admin.item.status-disabled", "<red>DISABLED");
        List<String> loreTemplate = ConfigHandler.getStringList("gui.module_admin.item.lore");

        for (String key : moduleKeys) {
            boolean enabled = section.getBoolean(key);
            Material material;
            try {
                material = Material.valueOf(enabled ? enabledMat : disabledMat);
            } catch (Exception e) {
                material = enabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                String displayName = nameFormat.replace("%name%", formatName(key));
                meta.displayName(parseToComponent(displayName));
                
                List<Component> lore = new ArrayList<>();
                for (String line : loreTemplate) {
                    String processed = line.replace("%status%", enabled ? statusEnabled : statusDisabled);
                    lore.add(parseToComponent(processed));
                }
                
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            gui.addItem(new GuiItem(item, event -> {
                toggleModule(player, key, enabled ? "disable" : "enable");
                openAdminGUI(player); 
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
                Set<String> keys = Objects
                        .requireNonNull(plugin.getConfig().getConfigurationSection("module"))
                        .getKeys(false);
                suggestions.addAll(keys);
            }
        } else if (args.length == 2) {
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
            sender.sendMessage(parseToComponent(msg.toString()));
        }
    }
}