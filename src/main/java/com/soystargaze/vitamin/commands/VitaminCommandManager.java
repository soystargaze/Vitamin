package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class VitaminCommandManager {

    private final Vitamin plugin;

    public VitaminCommandManager(Vitamin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        registerCommand("vitamin", new VitaminCommand(plugin));
        registerCommand("vita", new VitaminCommand(plugin));
        registerCommand("vi", new VitaminCommand(plugin));
        registerCommand("module", new ModuleCommand(plugin));
        registerCommand("pmodule", new PModuleCommand(plugin));
    }

    private void registerCommand(String commandName, CommandExecutor executor) {
        JavaPlugin javaPlugin = plugin;
        javaPlugin.getCommand(commandName).setExecutor(executor);
        if (executor instanceof TabCompleter) {
            javaPlugin.getCommand(commandName).setTabCompleter((TabCompleter) executor);
        }
    }
}