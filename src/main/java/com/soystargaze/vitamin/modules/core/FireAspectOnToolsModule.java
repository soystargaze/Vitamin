package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FireAspectOnToolsModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<Material, Material> smeltMap;
    private final Set<Location> processedBlocks = ConcurrentHashMap.newKeySet();

    public FireAspectOnToolsModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.smeltMap = initializeSmeltMap();
    }

    private Map<Material, Material> initializeSmeltMap() {
        Map<Material, Material> map = new EnumMap<>(Material.class);
        map.put(Material.IRON_ORE, Material.IRON_INGOT);
        map.put(Material.RAW_IRON, Material.IRON_INGOT);
        map.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        map.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        map.put(Material.NETHER_GOLD_ORE, Material.GOLD_INGOT);
        map.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        map.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        map.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        map.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        map.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        map.put(Material.SAND, Material.GLASS);
        map.put(Material.RED_SAND, Material.GLASS);
        map.put(Material.STONE, Material.STONE);
        map.put(Material.COBBLESTONE, Material.STONE);
        map.put(Material.CLAY_BALL, Material.BRICK);
        map.put(Material.KELP, Material.DRIED_KELP);
        map.put(Material.WET_SPONGE, Material.SPONGE);
        map.put(Material.CLAY, Material.TERRACOTTA);
        map.put(Material.POTATO, Material.BAKED_POTATO);
        return map;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack left = event.getInventory().getItem(0);
        ItemStack right = event.getInventory().getItem(1);

        if (left == null || right == null) return;

        boolean leftHasFire = left.containsEnchantment(Enchantment.FIRE_ASPECT);
        boolean leftHasFortune = left.containsEnchantment(Enchantment.FORTUNE);
        boolean leftHasSilk = left.containsEnchantment(Enchantment.SILK_TOUCH);

        // Case A: Right item is an Enchanted Book
        if (right.getType() == Material.ENCHANTED_BOOK && right.getItemMeta() instanceof EnchantmentStorageMeta esm) {
            
            // Prohibit Fire Aspect if tool has Fortune or Silk Touch
            if (esm.hasStoredEnchant(Enchantment.FIRE_ASPECT) && (leftHasFortune || leftHasSilk)) {
                event.setResult(null);
                return;
            }

            // Prohibit Fortune or Silk Touch if tool has Fire Aspect
            if ((esm.hasStoredEnchant(Enchantment.FORTUNE) || esm.hasStoredEnchant(Enchantment.SILK_TOUCH)) && leftHasFire) {
                event.setResult(null);
                return;
            }

            // Custom logic to allow applying Fire Aspect book to valid tools
            if (esm.hasStoredEnchant(Enchantment.FIRE_ASPECT) && isValidTool(left.getType())) {
                int level = esm.getStoredEnchantLevel(Enchantment.FIRE_ASPECT);
                ItemStack result = left.clone();
                result.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, level);
                event.setResult(result);
                
                AnvilView view = event.getView();
                int cost = (level == 2) ? 10 : 5;
                Bukkit.getScheduler().runTask(plugin, () -> view.setRepairCost(cost));
            }
        } 
        // Case B: Combining two tools
        else if (isValidTool(left.getType()) && left.getType() == right.getType()) {
            boolean rightHasFire = right.containsEnchantment(Enchantment.FIRE_ASPECT);
            boolean rightHasFortune = right.containsEnchantment(Enchantment.FORTUNE);
            boolean rightHasSilk = right.containsEnchantment(Enchantment.SILK_TOUCH);

            // If the combination would result in Fire Aspect + (Fortune or Silk Touch), block it
            if ((leftHasFire || rightHasFire) && (leftHasFortune || rightHasFortune || leftHasSilk || rightHasSilk)) {
                event.setResult(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.fire_aspect_tools") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.fire_aspect_tools")) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        Material blockType = event.getBlock().getType();
        Location blockLocation = event.getBlock().getLocation();

        int fireAspectLevel = tool.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
        if (fireAspectLevel <= 0 || !smeltMap.containsKey(blockType)) {
            return;
        }

        if (processedBlocks.contains(blockLocation)) {
            return;
        }
        processedBlocks.add(blockLocation);
        Bukkit.getScheduler().runTaskLater(plugin, () -> processedBlocks.remove(blockLocation), 1L);

        if (fireAspectLevel == 1) {
            if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                processBlockDrop(event, tool);
            }
        } else {
            processBlockDrop(event, tool);
        }
    }

    private void processBlockDrop(BlockBreakEvent event, ItemStack tool) {
        Material blockType = event.getBlock().getType();
        Material resultType = smeltMap.get(blockType);
        if (resultType == null) return;

        Collection<ItemStack> naturalDrops = event.getBlock().getDrops(tool);
        if (naturalDrops.isEmpty()) return;

        event.setDropItems(false);

        for (ItemStack drop : naturalDrops) {
            Material smelted = smeltMap.get(drop.getType());
            if (smelted == null) {
                smelted = resultType;
            }
            
            ItemStack finalDrop = new ItemStack(smelted, drop.getAmount());
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), finalDrop);
        }
    }

    private boolean isValidTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || 
               name.endsWith("_SWORD") || name.endsWith("_HOE");
    }
}