package com.erosmari.vitamin.modules.core;

import com.erosmari.vitamin.database.DatabaseHandler;
import com.erosmari.vitamin.utils.LoggingUtils;
import com.erosmari.vitamin.utils.TranslationHandler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerXptoBooksModule implements Listener {

    private final NamespacedKey xpKey;

    public PlayerXptoBooksModule(JavaPlugin plugin) {
        this.xpKey = new NamespacedKey(plugin, "xp_amount");
    }

    @EventHandler
    public void onBookConversion(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.xp_books") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.player_xp_to_books")) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BOOK) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(xpKey, PersistentDataType.INTEGER)) {
            return;
        }

        int totalXp = getPlayerTotalXp(player);
        if (totalXp <= 0) {
            return;
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        ItemStack xpBook = createXpBook(totalXp);
        setPlayerXp(player, 0);

        if (!player.getInventory().addItem(xpBook).isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), xpBook);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);
        LoggingUtils.sendAndLog(player, "xpbook.created", totalXp);
    }

    @EventHandler
    public void onXpBookUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.xp_books") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.player_xp_to_books")) {
            return;
        }
        if (player.isSneaking()) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BOOK) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(xpKey, PersistentDataType.INTEGER)) {
            return;
        }
        Integer storedXpObj = meta.getPersistentDataContainer().get(xpKey, PersistentDataType.INTEGER);
        if (storedXpObj == null) {
            return;
        }
        int storedXp = storedXpObj;

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        int currentXp = getPlayerTotalXp(player);
        int newTotalXp = currentXp + storedXp;
        setPlayerXp(player, newTotalXp);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);
        LoggingUtils.sendAndLog(player, "xpbook.used", storedXp);
    }

    private ItemStack createXpBook(int xpAmount) {
        ItemStack xpBook = new ItemStack(Material.BOOK);
        ItemMeta meta = xpBook.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(xpKey, PersistentDataType.INTEGER, xpAmount);
            meta.displayName(TranslationHandler.getComponent("xpbook.item_name", xpAmount));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            xpBook.setItemMeta(meta);
        }
        return xpBook;
    }

    private int getPlayerTotalXp(Player player) {
        int level = player.getLevel();
        float progress = player.getExp();
        return (int) (getXpAtLevel(level) + progress * getXpToNextLevel(level));
    }

    private void setPlayerXp(Player player, int totalXp) {
        player.setLevel(0);
        player.setExp(0f);
        int level = 0;
        while (getXpAtLevel(level + 1) <= totalXp) {
            level++;
        }
        int xpIntoLevel = totalXp - getXpAtLevel(level);
        float progress = (getXpToNextLevel(level) > 0) ? (float) xpIntoLevel / getXpToNextLevel(level) : 0f;
        player.setLevel(level);
        player.setExp(progress);
    }

    private int getXpAtLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    private int getXpToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }
}