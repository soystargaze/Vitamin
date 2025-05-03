package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class UnlockRecipesModule implements Listener {

    private final JavaPlugin plugin;

    public UnlockRecipesModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.unlock_recipes") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.unlock_recipes")) {
            return;
        }
        if (!player.hasPlayedBefore()) {
            AsyncExecutor.getExecutor().execute(() -> unlockAllRecipes(player));
        }
    }

    private void unlockAllRecipes(Player player) {
        Bukkit.getServer().recipeIterator().forEachRemaining(recipe -> {
            if (recipe instanceof Keyed keyedRecipe) {
                Bukkit.getScheduler().runTask(plugin, () -> player.discoverRecipe(keyedRecipe.getKey()));
            }
        });
    }
}