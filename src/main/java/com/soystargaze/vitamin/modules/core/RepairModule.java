package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RepairModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastRepairTime = new HashMap<>();
    private static final long REPAIR_COOLDOWN_MS = 500; // Prevenir clics rápidos

    public RepairModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isRepairableTool(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type.name().endsWith("_SWORD") || type.name().endsWith("_PICKAXE")
                || type.name().endsWith("_AXE") || type.name().endsWith("_SHOVEL")
                || type.name().endsWith("_HOE");
    }

    private Material getToolRepairMaterial(ItemStack tool) {
        Material type = tool.getType();
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

    private boolean isValidRepairMaterial(ItemStack tool, Material material) {
        Material baseMaterial = getToolRepairMaterial(tool);
        if (material == baseMaterial) {
            return true;
        }

        if (baseMaterial == Material.IRON_INGOT && material == Material.IRON_NUGGET) {
            return true;
        } else return baseMaterial == Material.GOLD_INGOT && material == Material.GOLD_NUGGET;
    }

    private double getRepairValuePerMaterial(Material material) {
        if (material == Material.DIAMOND) {
            return plugin.getConfig().getDouble("repair.diamond_value", 0.4);
        } else if (material == Material.NETHERITE_INGOT) {
            return plugin.getConfig().getDouble("repair.netherite_value", 0.8);
        } else if (material == Material.IRON_INGOT || material == Material.GOLD_INGOT) {
            return plugin.getConfig().getDouble("repair.ingot_value", 0.27);
        } else if (material == Material.IRON_NUGGET || material == Material.GOLD_NUGGET) {
            return plugin.getConfig().getDouble("repair.nugget_value", 0.03);
        }
        return 0.0;
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
        Map<Material, Integer> repairMaterials = new HashMap<>();

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (isRepairableTool(item)) {
                if (tool != null) {
                    inv.setResult(null);
                    return;
                }
                tool = item.clone();
            } else if (tool != null && isValidRepairMaterial(tool, item.getType())) {
                repairMaterials.put(item.getType(), repairMaterials.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        if (tool == null || repairMaterials.isEmpty()) {
            inv.setResult(null);
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int maxDurability = tool.getType().getMaxDurability();
        int currentDamage = damageable.getDamage();

        if (currentDamage <= 0) {
            inv.setResult(null);
            return;
        }

        int totalRepairAmount = 0;
        for (Map.Entry<Material, Integer> entry : repairMaterials.entrySet()) {
            double repairPerMaterial = maxDurability * getRepairValuePerMaterial(entry.getKey());
            totalRepairAmount += (int)(entry.getValue() * repairPerMaterial);
        }

        if (totalRepairAmount <= 0) {
            inv.setResult(null);
            return;
        }

        int newDamage = Math.max(0, currentDamage - totalRepairAmount);
        damageable.setDamage(newDamage);
        tool.setItemMeta(meta);
        inv.setResult(tool);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getSlotType() != InventoryType.SlotType.RESULT ||
                !(event.getView().getTopInventory() instanceof CraftingInventory craftingInv)) {
            return;
        }

        ItemStack result = craftingInv.getResult();

        if (result == null || !player.hasPermission("vitamin.module.repair") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.repair")) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (lastRepairTime.containsKey(playerUUID)) {
            long lastTime = lastRepairTime.get(playerUUID);
            if (currentTime - lastTime < REPAIR_COOLDOWN_MS) {
                return;
            }
        }
        lastRepairTime.put(playerUUID, currentTime);

        ItemStack[] matrix = craftingInv.getMatrix();
        ItemStack toolToRepair = null;
        Map<Material, Integer> repairMaterials = new HashMap<>();

        for (ItemStack item : matrix) {
            if (item == null) continue;

            if (isRepairableTool(item)) {
                if (toolToRepair != null) return;
                toolToRepair = item;
            } else if (toolToRepair != null && isValidRepairMaterial(toolToRepair, item.getType())) {
                repairMaterials.put(item.getType(), repairMaterials.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        if (toolToRepair == null || repairMaterials.isEmpty()) return;

        event.setCancelled(true);

        ItemMeta meta = toolToRepair.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int maxDurability = toolToRepair.getType().getMaxDurability();
        int currentDamage = damageable.getDamage();

        int totalRepairAmount = 0;
        for (Map.Entry<Material, Integer> entry : repairMaterials.entrySet()) {
            double repairPerMaterial = maxDurability * getRepairValuePerMaterial(entry.getKey());
            totalRepairAmount += (int)(entry.getValue() * repairPerMaterial);
        }

        int actualRepairAmount = Math.min(totalRepairAmount, currentDamage);
        double repairRatio = actualRepairAmount > 0 ? (double) actualRepairAmount / totalRepairAmount : 0;

        ItemStack repairedTool = toolToRepair.clone();
        ItemMeta repairedMeta = repairedTool.getItemMeta();
        if (repairedMeta instanceof Damageable repairableMeta) {
            repairableMeta.setDamage(Math.max(0, currentDamage - actualRepairAmount));
            repairedTool.setItemMeta(repairedMeta);
        }

        for (int i = 0; i < matrix.length; i++) {
            craftingInv.setItem(i + 1, null);
        }

        player.getInventory().addItem(repairedTool);

        if (repairRatio < 1.0) {
            for (Map.Entry<Material, Integer> entry : repairMaterials.entrySet()) {
                int remainingAmount = (int) Math.floor(entry.getValue() * (1 - repairRatio));
                if (remainingAmount > 0) {
                    ItemStack leftoverMaterials = new ItemStack(entry.getKey(), remainingAmount);
                    player.getInventory().addItem(leftoverMaterials);
                }
            }
        }

        craftingInv.setResult(null);

        player.updateInventory();
    }
}