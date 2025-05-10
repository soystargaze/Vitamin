package com.soystargaze.vitamin.modules.core;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Particle;
import org.bukkit.Sound;
import com.soystargaze.vitamin.database.DatabaseHandler;

import java.util.UUID;

public class BoneMealExpansionModule implements Listener {

    private final JavaPlugin plugin;

    public BoneMealExpansionModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!player.hasPermission("vitamin.module.bone_meal_expansion") ||
                !DatabaseHandler.isModuleEnabledForPlayer(playerId, "module.bone_meal_expansion")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BONE_MEAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();

        if ((type == Material.SUGAR_CANE && plugin.getConfig().getBoolean("bone_meal_expansion.crops.sugar_cane", true)) ||
                (type == Material.CACTUS && plugin.getConfig().getBoolean("bone_meal_expansion.crops.cactus", true)) ||
                (type == Material.BAMBOO && plugin.getConfig().getBoolean("bone_meal_expansion.crops.bamboo", true))) {
            handleStackableCrop(block, type, item);
        }
        else if ((type == Material.NETHER_WART && plugin.getConfig().getBoolean("bone_meal_expansion.crops.nether_wart", true)) ||
                (type == Material.PUMPKIN_STEM && plugin.getConfig().getBoolean("bone_meal_expansion.crops.pumpkin_stem", true)) ||
                (type == Material.MELON_STEM && plugin.getConfig().getBoolean("bone_meal_expansion.crops.melon_stem", true))) {
            handleAgeableCrop(block, item);
        }
    }

    private void handleStackableCrop(Block block, Material material, ItemStack item) {
        Block above = block.getRelative(0, 1, 0);
        if (above.getType() == Material.AIR) {
            above.setType(material);
            consumeBoneMeal(item);
            above.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, above.getLocation().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0.0);
            above.getWorld().playSound(above.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
        }
    }

    private void handleAgeableCrop(Block block, ItemStack item) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            int currentAge = ageable.getAge();
            int maxAge = ageable.getMaximumAge();
            if (currentAge < maxAge) {
                ageable.setAge(currentAge + 1);
                block.setBlockData(ageable);
                consumeBoneMeal(item);
                block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0.0);
                block.getWorld().playSound(block.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
            }
        }
    }

    private void consumeBoneMeal(ItemStack item) {
        if (item.getType() == Material.BONE_MEAL) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                item.setAmount(0);
            }
        }
    }
}