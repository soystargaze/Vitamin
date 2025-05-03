package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import net.advancedplugins.ae.api.AEAPI;
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
import java.util.Map.Entry;

public class EnchantsBackModule implements Listener {

    private final JavaPlugin plugin;

    public EnchantsBackModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGrindstoneUse(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof GrindstoneInventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("vitamin.module.enchants_back")
                || !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.enchants_back")) {
            return;
        }

        ItemStack inputItem = inventory.getItem(0);
        if (inputItem == null) return;

        Map<Enchantment, Integer> vanillaEnchants = inputItem.getEnchantments();
        boolean hasVanilla = !vanillaEnchants.isEmpty();

        boolean aeLoaded = plugin.getServer()
                .getPluginManager()
                .isPluginEnabled("AdvancedEnchantments");
        Map<String, Integer> aeEnchants = aeLoaded
                ? AEAPI.getEnchantmentsOnItem(inputItem)
                : Map.of();
        boolean hasAE = !aeEnchants.isEmpty();

        if (!hasVanilla && !hasAE) return;

        int totalEnchants = (hasVanilla ? vanillaEnchants.size() : 0)
                + (hasAE      ? aeEnchants.size()      : 0);
        int maxReturned = plugin.getConfig()
                .getInt("enchants_back.max_returned", totalEnchants);
        int toProcess = Math.min(totalEnchants, maxReturned);

        int bookCount = countBooks(player.getInventory());
        if (bookCount < toProcess) return;
        int freeSlots = getFreeInventorySlots(player.getInventory());
        if (freeSlots < toProcess) return;

        int processed = 0;

        for (Entry<Enchantment, Integer> entry : vanillaEnchants.entrySet()) {
            if (processed >= toProcess) break;
            if (createEnchantedBook(player, entry.getKey(), entry.getValue())) {
                removeOneBook(player);
                processed++;
            } else {
                return;
            }
        }

        if (aeLoaded) {
            for (Entry<String, Integer> aeEntry : aeEnchants.entrySet()) {
                if (processed >= toProcess) break;
                ItemStack aeBook = new ItemStack(Material.BOOK);
                aeBook = AEAPI.applyEnchant(
                        aeEntry.getKey(),
                        aeEntry.getValue(),
                        aeBook
                );
                if (player.getInventory().addItem(aeBook).isEmpty()) {
                    removeOneBook(player);
                    processed++;
                }
            }
        }

        player.updateInventory();
    }

    private boolean createEnchantedBook(Player player, Enchantment ench, int level) {
        try {
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
            if (meta != null) {
                meta.addStoredEnchant(ench, level, true);
                book.setItemMeta(meta);
                return player.getInventory().addItem(book).isEmpty();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private int countBooks(Inventory inv) {
        int c = 0;
        for (ItemStack itm : inv.getContents()) {
            if (itm != null && itm.getType() == Material.BOOK) {
                c += itm.getAmount();
            }
        }
        return c;
    }

    private int getFreeInventorySlots(Inventory inv) {
        int c = 0;
        for (ItemStack itm : inv.getContents()) {
            if (itm == null || itm.getType() == Material.AIR) {
                c++;
            }
        }
        return c;
    }

    private void removeOneBook(Player player) {
        for (ItemStack itm : player.getInventory().getContents()) {
            if (itm != null && itm.getType() == Material.BOOK) {
                if (itm.getAmount() > 1) itm.setAmount(itm.getAmount() - 1);
                else player.getInventory().remove(itm);
                break;
            }
        }
    }
}