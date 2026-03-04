package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class RepairModule implements Listener {

    private final JavaPlugin plugin;

    public RepairModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isRepairableTool(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        Material type = item.getType();
        String name = type.name();
        return name.endsWith("_SWORD") || name.endsWith("_PICKAXE")
                || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE") || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS") || type == Material.ELYTRA || type == Material.TRIDENT;
    }

    private Material getBaseRepairMaterial(Material toolType) {
        String name = toolType.name();
        if (name.startsWith("DIAMOND_")) return Material.DIAMOND;
        if (name.startsWith("NETHERITE_")) return Material.NETHERITE_INGOT;
        if (name.startsWith("IRON_")) return Material.IRON_INGOT;
        if (name.startsWith("GOLDEN_")) return Material.GOLD_INGOT;
        if (name.startsWith("STONE_") || name.equals("STONE_SWORD")) return Material.COBBLESTONE;
        if (name.startsWith("WOODEN_")) return Material.OAK_PLANKS;
        if (toolType == Material.ELYTRA) return Material.PHANTOM_MEMBRANE;
        if (toolType == Material.TRIDENT) return Material.PRISMARINE_SHARD;
        return null;
    }

    private boolean isValidRepairMaterial(Material toolType, Material repairMaterial) {
        Material base = getBaseRepairMaterial(toolType);
        if (base == null) return false;
        if (repairMaterial == base) return true;

        if (base == Material.IRON_INGOT && repairMaterial == Material.IRON_NUGGET) return true;
        if (base == Material.GOLD_INGOT && repairMaterial == Material.GOLD_NUGGET) return true;

        return false;
    }

    private double getRepairValue(Material material) {
        return switch (material) {
            case NETHERITE_INGOT -> plugin.getConfig().getDouble("repair.netherite_value", 0.8);
            case DIAMOND -> plugin.getConfig().getDouble("repair.diamond_value", 0.4);
            case IRON_INGOT, GOLD_INGOT -> plugin.getConfig().getDouble("repair.ingot_value", 0.27);
            case IRON_NUGGET, GOLD_NUGGET -> plugin.getConfig().getDouble("repair.nugget_value", 0.03);
            case PHANTOM_MEMBRANE -> plugin.getConfig().getDouble("repair.phantom_membrane_value", 0.25);
            case PRISMARINE_SHARD -> plugin.getConfig().getDouble("repair.prismarine_shard_value", 0.25);
            case COBBLESTONE -> plugin.getConfig().getDouble("repair.cobblestone_value", 0.20);
            case OAK_PLANKS -> plugin.getConfig().getDouble("repair.planks_value", 0.20);
            default -> 0.0;
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        if (!player.hasPermission("vitamin.module.repair") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.repair")) {
            return;
        }

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        
        ItemStack tool = null;
        ItemStack material = null;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (isRepairableTool(item)) {
                if (tool != null) return;
                tool = item;
            } else {
                if (material != null && material.getType() != item.getType()) return;
                material = item;
            }
        }

        if (tool == null || material == null) return;
        if (!isValidRepairMaterial(tool.getType(), material.getType())) return;

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.getDamage() == 0) return;

        int maxDurability = tool.getType().getMaxDurability();
        int currentDamage = damageable.getDamage();
        
        double repairPercentPerItem = getRepairValue(material.getType());
        int repairPerItem = (int) (maxDurability * repairPercentPerItem);
        if (repairPerItem <= 0) repairPerItem = 1;

        // Calculate how many items are needed to reach 100% durability
        int itemsNeeded = (int) Math.ceil((double) currentDamage / repairPerItem);
        int itemsToUse = Math.min(itemsNeeded, material.getAmount());

        ItemStack result = tool.clone();
        Damageable resultMeta = (Damageable) result.getItemMeta();
        resultMeta.setDamage(Math.max(0, currentDamage - (repairPerItem * itemsToUse)));
        result.setItemMeta(resultMeta);

        inv.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getInventory() instanceof CraftingInventory inv)) return;
        
        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("vitamin.module.repair") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.repair")) {
            return;
        }

        ItemStack result = inv.getResult();
        if (result == null || result.getType() == Material.AIR) return;

        ItemStack[] matrix = inv.getMatrix();
        ItemStack tool = null;
        ItemStack material = null;
        int toolIndex = -1;
        int materialIndex = -1;

        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item == null || item.getType() == Material.AIR) continue;

            if (isRepairableTool(item)) {
                tool = item;
                toolIndex = i;
            } else {
                material = item;
                materialIndex = i;
            }
        }
        
        if (tool == null || material == null) return;
        
        event.setCancelled(true);

        Damageable toolMeta = (Damageable) tool.getItemMeta();
        int currentDamage = toolMeta.getDamage();
        int maxDurability = tool.getType().getMaxDurability();
        
        double repairPercentPerItem = getRepairValue(material.getType());
        int repairPerItem = (int) (maxDurability * repairPercentPerItem);
        if (repairPerItem <= 0) repairPerItem = 1;

        int itemsNeeded = (int) Math.ceil((double) currentDamage / repairPerItem);
        int itemsToUse = Math.min(itemsNeeded, material.getAmount());

        // Update matrix: consume tool and used materials
        inv.setItem(toolIndex + 1, null); 
        
        if (material.getAmount() > itemsToUse) {
            ItemStack leftovers = material.clone();
            leftovers.setAmount(material.getAmount() - itemsToUse);
            inv.setItem(materialIndex + 1, leftovers);
        } else {
            inv.setItem(materialIndex + 1, null);
        }

        // Give the repaired item to the player (cursor)
        event.setCurrentItem(null); // Clear result slot
        if (event.getClick().isShiftClick()) {
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), result);
            } else {
                player.getInventory().addItem(result);
            }
        } else {
            event.getWhoClicked().setItemOnCursor(result);
        }
        
        inv.setResult(null);
        player.updateInventory();
    }
}