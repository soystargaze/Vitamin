package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.utils.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
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
        if (!event.getPlayer().hasPlayedBefore()) {
            AsyncExecutor.getExecutor().execute(() -> unlockAllRecipes(event.getPlayer()));
        }
    }

    private void unlockAllRecipes(org.bukkit.entity.Player player) {
        Bukkit.getServer().recipeIterator().forEachRemaining(recipe -> {
            if (recipe instanceof Keyed keyedRecipe) {
                Bukkit.getScheduler().runTask(plugin, () -> player.discoverRecipe(keyedRecipe.getKey()));
            }
        });
    }
}