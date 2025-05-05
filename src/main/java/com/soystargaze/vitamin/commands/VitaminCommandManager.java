package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class VitaminCommandManager {

    private final Vitamin plugin;

    public VitaminCommandManager(Vitamin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        register("vitamin", commandExecutor());
        register("vita",    commandExecutor());
        register("vi",      commandExecutor());
    }

    private CommandExecutor commandExecutor() {
        return new VitaminCommand(plugin);
    }

    private void register(String name, CommandExecutor executor) {
        var cmd = plugin.getCommand(name);
        if (cmd == null) {
            throw new IllegalStateException(
                    "The command '" + name + "' is not defined in plugin.yml."
            );
        }
        cmd.setExecutor(executor);
        if (executor instanceof TabCompleter) {
            cmd.setTabCompleter((TabCompleter) executor);
        }
    }
}