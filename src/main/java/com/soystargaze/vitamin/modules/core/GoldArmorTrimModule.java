package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GoldArmorTrimModule implements Listener {

    private final Set<UUID> provokedPlayers = new HashSet<>();

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Piglin && event.getTarget() instanceof Player player) {
            if (!player.hasPermission("vitamin.module.gold_armor_trim") ||
                    !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.gold_armor_trim")) {
                return;
            }
            if (!provokedPlayers.contains(player.getUniqueId()) && hasGoldArmorTrim(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof Piglin) {
            provokedPlayers.add(player.getUniqueId());
        }
    }

    private boolean hasGoldArmorTrim(Player player) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.hasItemMeta()) {
                ArmorMeta meta = (ArmorMeta) armor.getItemMeta();
                ArmorTrim trim = meta.getTrim();
                if (trim != null && trim.getMaterial() == TrimMaterial.GOLD) {
                    return true;
                }
            }
        }
        return false;
    }
}