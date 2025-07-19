package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.modules.CancellableModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CampfireTweaksModule implements Listener, CancellableModule {

    private final JavaPlugin plugin;
    private int regenRadius;
    private int regenLevel;
    private int regenDuration;
    private int checkInterval;
    private int saturationInterval;
    private final int taskId;
    private int tickCounter = 0;

    public CampfireTweaksModule(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::applyEffects, 0L, checkInterval);
    }

    private void loadConfig() {
        regenRadius = plugin.getConfig().getInt("campfire_tweaks.regen_radius", 5);
        regenLevel = plugin.getConfig().getInt("campfire_tweaks.regen_level", 1);
        regenDuration = plugin.getConfig().getInt("campfire_tweaks.regen_duration_ticks", 60);
        checkInterval = plugin.getConfig().getInt("campfire_tweaks.check_interval_ticks", 80);
        saturationInterval = plugin.getConfig().getInt("campfire_tweaks.saturation_interval_ticks", 160);
    }

    private void applyEffects() {
        tickCounter++;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("vitamin.module.campfire_tweaks") ||
                    !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.campfire_tweaks")) {
                continue;
            }

            if (isNearLitCampfire(player.getLocation(), regenRadius)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenDuration, regenLevel - 1, true, false));

                if (tickCounter % (saturationInterval / checkInterval) == 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, true, false));
                }
            }
        }
    }

    private boolean isNearLitCampfire(Location loc, int radius) {
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;
                    Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                    Material type = block.getType();
                    if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
                        Lightable lightable = (Lightable) block.getBlockData();
                        if (lightable.isLit()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.campfire_tweaks") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.campfire_tweaks")) {
            return;
        }
        if (event.getItem() != null) return;
        Lightable lightable = (Lightable) block.getBlockData();
        if (lightable.isLit()) {
            lightable.setLit(false);
            block.setBlockData(lightable);
            event.setCancelled(true);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        if (type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.campfire_tweaks") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.campfire_tweaks")) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) return;
        Lightable lightable = (Lightable) block.getBlockData();
        if (lightable.isLit()) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.CHARCOAL, 1));
        }
    }

    @Override
    public void cancelTasks() {
        Bukkit.getScheduler().cancelTask(taskId);
    }
}