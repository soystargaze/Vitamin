package com.erosmari.vitamin.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class TotemFromInventoryModule implements Listener {

    public TotemFromInventoryModule(final JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth <= 0) {
            if (hasTotemAnywhere(player)) {
                removeOneTotem(player);

                event.setCancelled(true);

                player.setHealth(1.0);

                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40 * 20, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 5 * 20, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40 * 20, 0));

                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
            }
        }
    }

    private boolean hasTotemAnywhere(Player player) {
        PlayerInventory inventory = player.getInventory();

        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.getType() == Material.TOTEM_OF_UNDYING) {
                return true;
            }
        }

        for (ItemStack armor : inventory.getArmorContents()) {
            if (armor != null && armor.getType() == Material.TOTEM_OF_UNDYING) {
                return true;
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        return offHand.getType() == Material.TOTEM_OF_UNDYING;
    }

    private void removeOneTotem(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null && stack.getType() == Material.TOTEM_OF_UNDYING) {
                decrementStack(inventory, i, stack);
                return;
            }
        }

        ItemStack[] armorContents = inventory.getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            ItemStack armor = armorContents[i];
            if (armor != null && armor.getType() == Material.TOTEM_OF_UNDYING) {
                decrementStack(armorContents, i, armor);
                inventory.setArmorContents(armorContents);
                return;
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (!offHand.getType().isAir() && offHand.getType() == Material.TOTEM_OF_UNDYING) {
            if (offHand.getAmount() > 1) {
                offHand.setAmount(offHand.getAmount() - 1);
            } else {
                inventory.setItemInOffHand(null);
            }
        }
    }

    private void decrementStack(ItemStack[] stacks, int index, ItemStack stack) {
        int amount = stack.getAmount();
        if (amount > 1) {
            stack.setAmount(amount - 1);
        } else {
            stacks[index] = null;
        }
    }

    private void decrementStack(PlayerInventory inventory, int index, ItemStack stack) {
        int amount = stack.getAmount();
        if (amount > 1) {
            stack.setAmount(amount - 1);
        } else {
            inventory.setItem(index, null);
        }
    }
}