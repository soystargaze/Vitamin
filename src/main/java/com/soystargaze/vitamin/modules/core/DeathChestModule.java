package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.TextHandler;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeathChestModule implements Listener {

    private final JavaPlugin plugin;
    private final int verticalSearch;
    private final int horizontalSearch;
    private final NamespacedKey keyDeathChest;
    
    private static final Set<Material> UNSAFE_BLOCKS = Set.of(
            Material.LAVA, Material.FIRE, Material.CACTUS, Material.MAGMA_BLOCK
    );

    public DeathChestModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.verticalSearch   = plugin.getConfig().getInt("death_chest.vertical_search", 30);
        this.horizontalSearch = plugin.getConfig().getInt("death_chest.horizontal_search", 50);
        this.keyDeathChest = new NamespacedKey(plugin, "is_death_chest");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!player.hasPermission("vitamin.module.death_chest")
                || !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.death_chest")
        ) return;

        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty()) return;

        Location deathLocation = player.getLocation();
        DatabaseHandler.saveDeathLocation(player.getUniqueId(), deathLocation);

        Location chestLocation = findSafeLocation(deathLocation);
        if (chestLocation == null) {
            TextHandler.get().logTranslated("death_chest.location_not_found", player.getName());
            return;
        }

        Block block1 = chestLocation.getBlock();
        boolean useDoubleChest = drops.size() > 27;
        Block block2 = null;
        if (useDoubleChest) {
            Location adj = chestLocation.clone().add(1, 0, 0);
            if (isSafeLocation(adj)) {
                block2 = adj.getBlock();
            } else {
                useDoubleChest = false;
            }
        }

        createDeathChestBlock(block1);
        if (useDoubleChest && block2 != null) {
            createDeathChestBlock(block2);
            
            BlockData d1 = block1.getBlockData();
            BlockData d2 = block2.getBlockData();
            if (d1 instanceof Chest cd1 && d2 instanceof Chest cd2) {
                cd1.setType(Chest.Type.LEFT);
                cd2.setType(Chest.Type.RIGHT);
                block1.setBlockData(cd1);
                block2.setBlockData(cd2);
            }
        }

        BlockState state = block1.getState();
        if (state instanceof org.bukkit.block.Chest chestState) {
            String customNameFormat = plugin.getConfig().getString("death_chest.custom_name", "<gold>%player%'s Death Chest");
            String name = customNameFormat.replace("%player%", player.getName());
            chestState.customName(MiniMessage.miniMessage().deserialize(name));
            chestState.update();

            Inventory inv = chestState.getInventory();
            for (ItemStack item : drops) {
                Map<Integer, ItemStack> leftoverMap = inv.addItem(item);
                for (ItemStack leftover : leftoverMap.values()) {
                    block1.getWorld().dropItemNaturally(block1.getLocation(), leftover);
                }
            }
            event.getDrops().clear();
        }

        // Spawn head
        Location skullLoc = chestLocation.clone().add(0, 1, 0);
        if (skullLoc.getBlock().getType() == Material.AIR) {
            skullLoc.getBlock().setType(Material.PLAYER_HEAD);
            Skull skull = (Skull) skullLoc.getBlock().getState();
            skull.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
            skull.update();
        }

        // Effects
        if (plugin.getConfig().getBoolean("death_chest.spawn_effects", true)) {
            chestLocation.getWorld().spawnParticle(Particle.SOUL, chestLocation.clone().add(0.5, 0.5, 0.5), 20, 0.5, 0.5, 0.5, 0.05);
            chestLocation.getWorld().playSound(chestLocation, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.5f);
        }

        TextHandler.get().sendAndLog(player, "death_chest.created", 
                chestLocation.getBlockX(), chestLocation.getBlockY(), chestLocation.getBlockZ());
    }

    private void createDeathChestBlock(Block block) {
        block.setType(Material.CHEST);
        if (block.getState() instanceof org.bukkit.block.Chest chest) {
            // Mark as death chest for explosion protection and auto-removal
            chest.getPersistentDataContainer().set(keyDeathChest, PersistentDataType.BYTE, (byte) 1);
            chest.update();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!plugin.getConfig().getBoolean("death_chest.remove_when_empty", true)) return;
        
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof org.bukkit.block.Chest chest) {
            if (chest.getPersistentDataContainer().has(keyDeathChest, PersistentDataType.BYTE)) {
                // Check if empty
                boolean isEmpty = true;
                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        isEmpty = false;
                        break;
                    }
                }

                if (isEmpty) {
                    Block block = chest.getBlock();
                    block.setType(Material.AIR);
                    block.getWorld().spawnParticle(Particle.SMOKE, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.02);
                    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 0.8f);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("death_chest.protect_from_explosions", true)) return;
        event.blockList().removeIf(this::isDeathChest);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("death_chest.protect_from_explosions", true)) return;
        event.blockList().removeIf(this::isDeathChest);
    }

    private boolean isDeathChest(Block block) {
        if (block.getType() != Material.CHEST) return false;
        if (block.getState() instanceof org.bukkit.block.Chest chest) {
            return chest.getPersistentDataContainer().has(keyDeathChest, PersistentDataType.BYTE);
        }
        return false;
    }

    private Location findSafeLocation(Location start) {
        World world = start.getWorld();
        int x = start.getBlockX();
        int y = start.getBlockY();
        int z = start.getBlockZ();
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 1;

        y = Math.max(minY, Math.min(maxY, y));

        for (int i = 0; i < verticalSearch; i++) {
            int searchY = y - i;
            if (searchY > minY) {
                Location loc = new Location(world, x, searchY, z);
                if (isSafeLocation(loc)) return loc;
            }
        }

        for (int i = 1; i <= verticalSearch; i++) {
            int searchY = y + i;
            if (searchY < maxY) {
                Location loc = new Location(world, x, searchY, z);
                if (isSafeLocation(loc)) return loc;
            }
        }

        for (int radius = 1; radius <= horizontalSearch; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        Location loc = new Location(world, x + dx, y, z + dz);
                        Location groundLoc = findGroundLocation(loc, minY);
                        if (groundLoc != null && isSafeLocation(groundLoc)) {
                            return groundLoc;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Location findGroundLocation(Location loc, int minY) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        while (y > minY && world.getBlockAt(x, y - 1, z).getType() == Material.AIR) {
            y--;
        }
        return y > minY ? new Location(world, x, y, z) : null;
    }

    private boolean isSafeLocation(Location loc) {
        Block block = loc.getBlock();
        Block below = loc.clone().add(0, -1, 0).getBlock();
        return block.getType() == Material.AIR
                && below.getType().isSolid()
                && !UNSAFE_BLOCKS.contains(below.getType());
    }
}