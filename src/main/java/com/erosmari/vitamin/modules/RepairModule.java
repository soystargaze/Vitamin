package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class RepairModule implements Listener {

    private final double DIAMOND_REPAIR;
    private final double INGOT_REPAIR;
    private final double NUGGET_REPAIR;
    private final double NETHERITE_REPAIR;

    public RepairModule(JavaPlugin plugin) {
        DIAMOND_REPAIR = plugin.getConfig().getDouble("repair.diamond_value", 0.4);
        NETHERITE_REPAIR = plugin.getConfig().getDouble("repair.netherite_value", 0.8);
        INGOT_REPAIR = plugin.getConfig().getDouble("repair.ingot_value", 0.27);
        NUGGET_REPAIR = plugin.getConfig().getDouble("repair.nugget_value", 0.03);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        HumanEntity human = event.getView().getPlayer();
        if (!(human instanceof Player player)) return;

        if (!player.hasPermission("vitamin.module.repair") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.repair")) {
            return;
        }

        CraftingInventory inv = event.getInventory();
        if (inv.getMatrix().length != 4) return;

        ItemStack[] matrix = inv.getMatrix();

        ItemStack tool = null;
        double totalRepairPercent = 0.0;
        Material toolRepairMaterial = null;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (isRepairableTool(item)) {
                if (tool != null) {
                    tool = null;
                    break;
                }
                tool = item;
                toolRepairMaterial = getToolRepairMaterial(item);
            } else {
                if (toolRepairMaterial != null && isValidRepairMaterial(item, toolRepairMaterial)) {
                    totalRepairPercent += getRepairValue(item);
                }
            }
        }

        if (tool == null || totalRepairPercent <= 0) return;

        Material type = tool.getType();
        int maxDurability = type.getMaxDurability();
        if (maxDurability <= 0) return;

        ItemStack repairedTool = tool.clone();
        ItemMeta meta = repairedTool.getItemMeta();

        if (meta instanceof Damageable damageable) {
            int currentDamage = damageable.getDamage();
            int repairAmount = (int) Math.ceil(maxDurability * totalRepairPercent);
            int newDamage = currentDamage - repairAmount;
            if (newDamage < 0) {
                newDamage = 0;
            }
            damageable.setDamage(newDamage);
            repairedTool.setItemMeta(meta);
            inv.setResult(repairedTool);
        }
    }

    private boolean isRepairableTool(ItemStack item) {
        Material type = item.getType();
        return type.name().endsWith("_SWORD") || type.name().endsWith("_PICKAXE")
                || type.name().endsWith("_AXE") || type.name().endsWith("_SHOVEL")
                || type.name().endsWith("_HOE");
    }

    private Material getToolRepairMaterial(ItemStack item) {
        Material type = item.getType();
        if (type.name().startsWith("DIAMOND_")) {
            return Material.DIAMOND;
        } else if (type.name().startsWith("NETHERITE_")) {
            return Material.NETHERITE_INGOT;
        } else if (type.name().startsWith("IRON_")) {
            return Material.IRON_INGOT;
        } else if (type.name().startsWith("GOLD_")) {
            return Material.GOLD_INGOT;
        }
        return null;
    }

    private boolean isValidRepairMaterial(ItemStack item, Material toolRepairMaterial) {
        Material type = item.getType();
        if (toolRepairMaterial == Material.DIAMOND) {
            return type == Material.DIAMOND;
        } else if (toolRepairMaterial == Material.IRON_INGOT) {
            return type == Material.IRON_INGOT || type == Material.IRON_NUGGET;
        } else if (toolRepairMaterial == Material.GOLD_INGOT) {
            return type == Material.GOLD_INGOT || type == Material.GOLD_NUGGET;
        } else if (toolRepairMaterial == Material.NETHERITE_INGOT) {
            return type == Material.NETHERITE_INGOT;
        }
        return false;
    }

    private double getRepairValue(ItemStack item) {
        Material type = item.getType();
        if (type == Material.DIAMOND) {
            return DIAMOND_REPAIR;
        } else if (type == Material.IRON_INGOT || type == Material.GOLD_INGOT) {
            return INGOT_REPAIR;
        } else if (type == Material.IRON_NUGGET || type == Material.GOLD_NUGGET) {
            return NUGGET_REPAIR;
        } else if (type == Material.NETHERITE_INGOT) {
            return NETHERITE_REPAIR;
        }
        return 0;
    }
}