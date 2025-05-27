package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.Vitamin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

public class VitaminCommandManager {

    private final Vitamin plugin;
    private VitaminCommand vitaminCommand;

    public VitaminCommandManager(Vitamin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        vitaminCommand = new VitaminCommand(plugin);
        RestoreCommand restoreCommand = vitaminCommand.getRestoreCommand();

        register("vitamin", vitaminCommand);
        register("vita", vitaminCommand);
        register("vi", vitaminCommand);

        RestoreInventoryListener listener = new RestoreInventoryListener(plugin, restoreCommand);

        plugin.getModuleManager().addSystemListener(listener);
    }

    public void reregisterListeners() {
        if (vitaminCommand != null) {
            RestoreCommand restoreCommand = vitaminCommand.getRestoreCommand();
            if (restoreCommand != null) {
                RestoreInventoryListener restoreListener = new RestoreInventoryListener(plugin, restoreCommand);
                plugin.getModuleManager().addSystemListener(restoreListener);
            }
        }
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