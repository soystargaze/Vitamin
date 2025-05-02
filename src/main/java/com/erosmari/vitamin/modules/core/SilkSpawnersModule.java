package com.erosmari.vitamin.modules.core;

import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class SilkSpawnersModule implements Listener {

    private final NamespacedKey spawnerKey;

    public SilkSpawnersModule(JavaPlugin plugin) {
        this.spawnerKey = new NamespacedKey(plugin, "mob-type");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.silk_spawners") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.silk_spawners")) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        CreatureSpawner spawnerBlock = (CreatureSpawner) block.getState();
        EntityType entityType = spawnerBlock.getSpawnedType();

        event.setExpToDrop(0);
        event.setDropItems(false);

        block.setType(Material.AIR);

        dropSpawner(block, entityType);
    }

    private void dropSpawner(Block block, EntityType entityType) {
        ItemStack spawnerItem = createSpawnerItem(entityType);
        block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
    }

    private ItemStack createSpawnerItem(EntityType entityType) {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) spawner.getItemMeta();
        if (meta != null) {
            CreatureSpawner creatureSpawner = (CreatureSpawner) meta.getBlockState();
            creatureSpawner.setSpawnedType(entityType);
            meta.setBlockState(creatureSpawner);

            meta.getPersistentDataContainer().set(spawnerKey, PersistentDataType.STRING, entityType.name());
            spawner.setItemMeta(meta);
        }
        return spawner;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.silk_spawners") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.silk_spawners")) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) {
            return;
        }

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) {
            return;
        }

        String entityName = meta.getPersistentDataContainer().get(spawnerKey, PersistentDataType.STRING);
        if (entityName == null) {
            return;
        }

        try {
            EntityType entityType = EntityType.valueOf(entityName);
            CreatureSpawner spawner = (CreatureSpawner) event.getBlock().getState();
            spawner.setSpawnedType(entityType);
            spawner.update();
        } catch (IllegalArgumentException ignored) {
        }
    }
}