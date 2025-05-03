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
import org.bukkit.inventory.Inventory;
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

@SuppressWarnings("ALL")
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilView anvilView = event.getView();
        Inventory inventory = event.getInventory();
        Player player = (Player) anvilView.getPlayer();
        if (player == null) return;
        if (!player.hasPermission("vitamin.module.fire_aspect_tools") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.fire_aspect_tools")) {
            return;
        }

        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);

        if (left == null) {
            return;
        }

        if (right != null && right.getType() == Material.ENCHANTED_BOOK) {
            if (right.hasItemMeta() && right.getItemMeta() instanceof EnchantmentStorageMeta esm) {
                if (esm.hasStoredEnchant(Enchantment.FIRE_ASPECT)) {
                    int level = esm.getStoredEnchantLevel(Enchantment.FIRE_ASPECT);
                    ItemStack result = left.clone();
                    result.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, level);
                    event.setResult(result);

                    int repairCost = (level == 2) ? 14 : 7;
                    anvilView.setRepairCost(repairCost);
                }
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

        if (!isValidBreak(blockType, tool)) {
            return;
        }

        if (processedBlocks.contains(blockLocation)) {
            return;
        }
        processedBlocks.add(blockLocation);
        Bukkit.getScheduler().runTaskLater(plugin, () -> processedBlocks.remove(blockLocation), 1L);

        event.setDropItems(false);

        int fireAspectLevel = tool.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
        if (fireAspectLevel == 1) {
            if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                processBlockDrop(event, tool);
            } else {
                dropNaturalItems(event, tool);
            }
        } else if (fireAspectLevel >= 2) {
            processBlockDrop(event, tool);
        }
    }

    private void dropNaturalItems(BlockBreakEvent event, ItemStack tool) {
        Collection<ItemStack> naturalDrops = event.getBlock().getDrops(tool);
        for (ItemStack drop : naturalDrops) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
        }
    }

    private void processBlockDrop(BlockBreakEvent event, ItemStack tool) {
        int amount = calculateDropAmount(tool);
        Material resultType = smeltMap.get(event.getBlock().getType());
        if (resultType == null) {
            dropNaturalItems(event, tool);
            return;
        }
        ItemStack smeltedItem = new ItemStack(resultType, amount);
        event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation(),
                smeltedItem
        );
    }

    private int calculateDropAmount(ItemStack tool) {
        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        int dropAmount = 1;
        if (fortuneLevel > 0) {
            dropAmount += fortuneLevel;
        }
        return dropAmount;
    }

    private boolean isValidBreak(Material blockType, ItemStack tool) {
        return smeltMap.containsKey(blockType)
                && isValidTool(tool.getType())
                && !tool.containsEnchantment(Enchantment.SILK_TOUCH)
                && tool.getEnchantmentLevel(Enchantment.FIRE_ASPECT) > 0;
    }

    private boolean isValidTool(Material material) {
        return switch (material) {
            case WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE,
                 WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE,
                 WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL,
                 WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD,
                 WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE, DIAMOND_HOE, NETHERITE_HOE -> true;
            default -> false;
        };
    }
}