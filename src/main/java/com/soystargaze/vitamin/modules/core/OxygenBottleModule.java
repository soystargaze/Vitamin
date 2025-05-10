package com.soystargaze.vitamin.modules.core;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.soystargaze.vitamin.database.DatabaseHandler;

public class OxygenBottleModule implements Listener {

    private final JavaPlugin plugin;

    public OxygenBottleModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("vitamin.module.oxygen_bottle") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.oxygen_bottle")) {
            return;
        }
        if (event.getAction().toString().contains("RIGHT_CLICK") && player.isInWater()) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() == Material.GLASS_BOTTLE && item.getAmount() > 0) {
                int currentAir = player.getRemainingAir();
                int maxAir = player.getMaximumAir();
                int restoreAmount = plugin.getConfig().getInt("oxygen_bottle.restore_amount", 60); // 3 segundos por defecto
                int newAir = Math.min(currentAir + restoreAmount, maxAir);
                player.setRemainingAir(newAir);

                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    player.getInventory().setItemInMainHand(null);
                }
                player.getInventory().addItem(new ItemStack(Material.POTION));
            }
        }
    }
}