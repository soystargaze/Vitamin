package com.soystargaze.vitamin.modules.paper;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import com.soystargaze.vitamin.utils.text.modern.ModernTranslationHandler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
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

import java.util.List;

public class PaperPlayerXptoBooksModule implements Listener {

    private final NamespacedKey xpKey;
    private final int maxXpPerBook;
    private final double efficiency;

    public PaperPlayerXptoBooksModule(JavaPlugin plugin) {
        this.xpKey = new NamespacedKey(plugin, "xp_amount");
        this.maxXpPerBook = plugin.getConfig().getInt("xpbooks.max_per_book", 10000);
        this.efficiency = plugin.getConfig().getDouble("xpbooks.efficiency", 0.95);
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

        int xpToStore = Math.min(totalXp, maxXpPerBook);
        int remainingXp = totalXp - xpToStore;
        setPlayerXp(player, remainingXp);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        ItemStack xpBook = createXpBook(xpToStore);
        if (!player.getInventory().addItem(xpBook).isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), xpBook);
        }

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1F, 1F);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0.5F);

        TextHandler.get().sendMessage(player, "xpbook.created", xpToStore);
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
        int storedXp = (int) (storedXpObj * efficiency);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        int currentXp = getPlayerTotalXp(player);
        int newTotalXp = currentXp + storedXp;
        setPlayerXp(player, newTotalXp);

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1.5F);

        TextHandler.get().sendMessage(player, "xpbook.used", storedXp);
    }

    @EventHandler
    public void onXpBookMerge(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.xp_books") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.player_xp_to_books")) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (mainHand.getType() != Material.BOOK || offHand.getType() != Material.BOOK) {
            return;
        }
        ItemMeta mainMeta = mainHand.getItemMeta();
        ItemMeta offMeta = offHand.getItemMeta();
        if (mainMeta == null || offMeta == null ||
                !mainMeta.getPersistentDataContainer().has(xpKey, PersistentDataType.INTEGER) ||
                !offMeta.getPersistentDataContainer().has(xpKey, PersistentDataType.INTEGER)) {
            return;
        }
        Integer mainXp = mainMeta.getPersistentDataContainer().get(xpKey, PersistentDataType.INTEGER);
        Integer offXp = offMeta.getPersistentDataContainer().get(xpKey, PersistentDataType.INTEGER);
        if (mainXp == null || offXp == null) {
            return;
        }
        int totalMerged = Math.min(mainXp + offXp, maxXpPerBook);
        int lostXp = (mainXp + offXp) - totalMerged;

        if (offHand.getAmount() > 1) {
            offHand.setAmount(offHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInOffHand(null);
        }

        ItemStack mergedBook = createXpBook(totalMerged);
        player.getInventory().setItemInMainHand(mergedBook);

        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 40, 0.4, 0.4, 0.4, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5F, 1F);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);

        TextHandler.get().sendMessage(player, "xpbook.merged", totalMerged, lostXp);
    }

    private ItemStack createXpBook(int xpAmount) {
        ItemStack xpBook = new ItemStack(Material.BOOK);
        ItemMeta meta = xpBook.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(xpKey, PersistentDataType.INTEGER, xpAmount);
            meta.displayName(ModernTranslationHandler.getComponent("xpbook.item_name", xpAmount));
            meta.lore(List.of(ModernTranslationHandler.getComponent("xpbook.lore", xpAmount)));
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