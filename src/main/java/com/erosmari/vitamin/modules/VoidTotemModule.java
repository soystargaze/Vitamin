package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.utils.AsyncExecutor;
import com.erosmari.vitamin.utils.LoggingUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoidTotemModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public VoidTotemModule(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerFallIntoVoid(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getY() > -59) return;

        if (isOnCooldown(player)) {
            LoggingUtils.sendMessage(player, "void_totem.cooldown");
            return;
        }

        boolean totemFromInventoryEnabled = plugin.getConfig().getBoolean("modules.totem_from_inventory", true);

        if (totemFromInventoryEnabled ? !hasTotemInInventory(player) : !hasTotemInHand(player)) {
            LoggingUtils.sendMessage(player, "void_totem.no_totem");
            return;
        }

        if (activateVoidTotem(player, totemFromInventoryEnabled)) {
            setCooldown(player);
        }
    }

    private boolean activateVoidTotem(Player player, boolean useInventoryTotem) {
        if (removeTotem(player, useInventoryTotem)) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 20, 1));

            AsyncExecutor.getExecutor().execute(() -> teleportToSafeGround(player));

            LoggingUtils.sendMessage(player, "void_totem.activated");
            return true;
        }
        return false;
    }

    private boolean hasTotemInInventory(Player player) {
        return player.getInventory().contains(Material.TOTEM_OF_UNDYING);
    }

    private boolean hasTotemInHand(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return (mainHand.getType() == Material.TOTEM_OF_UNDYING || offHand.getType() == Material.TOTEM_OF_UNDYING);
    }

    private boolean removeTotem(Player player, boolean useInventoryTotem) {
        if (useInventoryTotem) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                    item.setAmount(item.getAmount() - 1);
                    return true;
                }
            }
        } else {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (mainHand.getType() == Material.TOTEM_OF_UNDYING) {
                mainHand.setAmount(mainHand.getAmount() - 1);
                return true;
            } else if (offHand.getType() == Material.TOTEM_OF_UNDYING) {
                offHand.setAmount(offHand.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    private void teleportToSafeGround(Player player) {
        World world = player.getWorld();
        Location location = player.getLocation();
        Location safeLocation = null;

        for (int y = world.getMaxHeight(); y > 0; y--) {
            Location testLocation = new Location(world, location.getX(), y, location.getZ());
            if (isSafeLocation(testLocation)) {
                safeLocation = testLocation.add(0, 1, 0);
                break;
            }
        }

        if (safeLocation == null) {
            safeLocation = world.getSpawnLocation();
            LoggingUtils.sendMessage(player, "void_totem.teleport_spawn");
        } else {
            LoggingUtils.sendMessage(player, "void_totem.teleport_safe");
        }

        Location finalSafeLocation = safeLocation;
        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(finalSafeLocation));
    }

    private boolean isSafeLocation(Location location) {
        return !location.getBlock().isPassable() &&
                location.add(0, 1, 0).getBlock().isPassable() &&
                location.add(0, 1, 0).getBlock().isPassable() &&
                !location.getBlock().isLiquid();
    }

    private boolean isOnCooldown(Player player) {
        Long lastUsage = cooldowns.get(player.getUniqueId());
        if (lastUsage == null) return false;
        long COOLDOWN_TIME = 30 * 1000;
        return System.currentTimeMillis() - lastUsage < COOLDOWN_TIME;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}