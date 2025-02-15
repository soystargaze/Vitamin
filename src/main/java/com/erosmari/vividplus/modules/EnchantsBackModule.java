package com.erosmari.vividplus.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class EnchantsBackModule implements Listener {

    public EnchantsBackModule(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onGrindstoneUse(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof GrindstoneInventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack inputItem = inventory.getItem(0);

        if (inputItem == null || inputItem.getEnchantments().isEmpty()) return;

        int enchantmentCount = inputItem.getEnchantments().size();
        int bookCount = countBooks(player.getInventory());

        if (bookCount < enchantmentCount) {
            return;
        }

        int freeSlots = getFreeInventorySlots(player.getInventory());
        if (freeSlots < enchantmentCount) {
            return;
        }

        try {
            for (Map.Entry<Enchantment, Integer> entry : inputItem.getEnchantments().entrySet()) {
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();

                if (createEnchantedBook(player, enchantment, level)) {
                    removeOneBook(player);
                } else {
                    return;
                }
            }

            player.updateInventory();

        } catch (Exception ignored) {
        }
    }

    private boolean createEnchantedBook(Player player, Enchantment enchantment, int level) {
        try {
            ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) enchantedBook.getItemMeta();

            if (meta != null) {
                meta.addStoredEnchant(enchantment, level, true);
                enchantedBook.setItemMeta(meta);
                return player.getInventory().addItem(enchantedBook).isEmpty();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private int countBooks(Inventory inventory) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.BOOK) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private int getFreeInventorySlots(Inventory inventory) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                count++;
            }
        }
        return count;
    }

    private void removeOneBook(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BOOK) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
                break;
            }
        }
    }
}