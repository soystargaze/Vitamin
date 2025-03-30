package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.Vitamin;
import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class ElytraArmorModule implements Listener {

    private final double defaultArmorValue;
    private final JavaPlugin plugin;

    public ElytraArmorModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defaultArmorValue = plugin.getConfig().getDouble("elytra_armor.value", 11.0);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.elytra_armor") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elytra_armor")) {
            return;
        }
        updateArmorBonus(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.elytra_armor") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elytra_armor")) {
            return;
        }
        Attribute armorAttribute = Vitamin.getInstance().getVersionAdapter().getArmorAttribute();
        AttributeInstance instance = player.getAttribute(armorAttribute);
        if (instance != null) {
            instance.setBaseValue(calculateTotalArmorFromEquipment(player));
        }
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.elytra_armor") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elytra_armor")) {
            return;
        }
        updateArmorBonus(player);
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.elytra_armor") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elytra_armor")) {
            return;
        }
        updateArmorBonus(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (!player.hasPermission("vitamin.module.elytra_armor") ||
                    !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.elytra_armor")) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> updateArmorBonus(player));
        }
    }

    private void updateArmorBonus(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        Attribute armorAttribute = Vitamin.getInstance().getVersionAdapter().getArmorAttribute();
        AttributeInstance instance = player.getAttribute(armorAttribute);
        if (instance == null) return;

        double baseArmorValue = calculateTotalArmorFromEquipment(player);
        double elytraBonus = (chestplate != null && chestplate.getType() == Material.ELYTRA) ? defaultArmorValue : 0;

        instance.setBaseValue(baseArmorValue + elytraBonus);
    }

    private double calculateTotalArmorFromEquipment(Player player) {
        double totalArmorValue = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                totalArmorValue += getArmorValueFromItem(item);
            }
        }
        return totalArmorValue;
    }

    private double getArmorValueFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasAttributeModifiers()) return 0;
        return Objects.requireNonNull(meta.getAttributeModifiers(Vitamin.getInstance().getVersionAdapter().getArmorAttribute()))
                .stream().mapToDouble(AttributeModifier::getAmount).sum();
    }
}