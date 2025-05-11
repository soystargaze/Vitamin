package com.soystargaze.vitamin.modules.core;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.soystargaze.vitamin.database.DatabaseHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OxygenBottleModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public OxygenBottleModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!player.hasPermission("vitamin.module.oxygen_bottle") ||
                !DatabaseHandler.isModuleEnabledForPlayer(playerId, "module.oxygen_bottle")) {
            return;
        }

        if (event.getAction().toString().contains("RIGHT_CLICK") && player.isInWater()) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() == Material.GLASS_BOTTLE && item.getAmount() > 0) {
                long currentTime = System.currentTimeMillis();
                long COOLDOWN = 500;
                if (lastUse.containsKey(playerId) && currentTime - lastUse.get(playerId) < COOLDOWN) {
                    return;
                }
                lastUse.put(playerId, currentTime);

                int currentAir = player.getRemainingAir();
                int maxAir = player.getMaximumAir();
                int restoreAmount = plugin.getConfig().getInt("oxygen_bottle.restore_amount", 60);
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