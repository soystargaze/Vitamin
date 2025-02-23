package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

public final class ReplanterModule implements Listener {

    private static final Map<Material, CropInfo> CROP_INFO = new EnumMap<>(Material.class);

    static {
        CROP_INFO.put(Material.WHEAT, new CropInfo(Material.WHEAT_SEEDS));
        CROP_INFO.put(Material.CARROTS, new CropInfo(Material.CARROT));
        CROP_INFO.put(Material.POTATOES, new CropInfo(Material.POTATO));
        CROP_INFO.put(Material.BEETROOTS, new CropInfo(Material.BEETROOT_SEEDS));
    }

    public ReplanterModule() {
    }

    @EventHandler
    public void onCropInteract(final PlayerInteractEvent event) {
        Action action = event.getAction();
        if (!(action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material blockType = block.getType();

        CropInfo cropInfo = CROP_INFO.get(blockType);
        if (cropInfo == null) {
            return;
        }

        if (!isCropMature(block)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.replanter") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.replanter")) {
            return;
        }

        Material seedType = cropInfo.seedType();

        event.setCancelled(true);

        removeSeedFromInventory(player, seedType);

        harvestCrop(block, player, seedType);

        block.setType(blockType);
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable);
        }

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 1.0F, 1.0F);
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 1, 0.5),
                10, 0.2, 0.2, 0.2, 0);
    }

    private void harvestCrop(Block block, Player player, Material seedType) {
        Collection<ItemStack> drops = block.getDrops(new ItemStack(Material.WOODEN_HOE));
        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            if (drop.getType() == seedType) {
                int amount = drop.getAmount();
                if (amount > 1) {
                    drop.setAmount(amount - 1);
                } else {
                    iterator.remove();
                }
                break;
            }
        }
        for (ItemStack item : drops) {
            player.getInventory().addItem(item);
        }
    }

    private boolean isCropMature(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    private void removeSeedFromInventory(Player player, Material seedType) {
        int seedSlot = player.getInventory().first(seedType);
        if (seedSlot >= 0) {
            ItemStack seedItem = player.getInventory().getItem(seedSlot);
            if (seedItem != null && seedItem.getAmount() > 0) {
                seedItem.setAmount(seedItem.getAmount() - 1);
            }
        }
    }

    private record CropInfo(Material seedType) {
    }
}