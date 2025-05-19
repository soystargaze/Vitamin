package com.soystargaze.vitamin.modules.core;

import com.saicone.rtag.RtagBlock;
import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.database.ReactivationData;
import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReplayableVaultModule implements Listener {

    private final JavaPlugin plugin;

    public ReplayableVaultModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.VAULT) {
            return;
        }

        RtagBlock editor = RtagBlock.of(block);
        Boolean isOminous = editor.get("server_data", "ominous");
        Material requiredKey = (isOminous != null && isOminous) ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY;

        if (event.getPlayer().getInventory().getItemInMainHand().getType() != requiredKey) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.replayable_vault") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.replayable_vault")) {
            return;
        }

        Location vaultLoc = block.getLocation();
        UUID playerId = player.getUniqueId();

        ReactivationData data = DatabaseHandler.getReactivationData(vaultLoc, playerId);

        long currentTime = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("replayable_vault.cooldown", 3600000); // Default: 1 hour
        int maxReactivations = plugin.getConfig().getInt("replayable_vault.max_reactivations", 2);
        int maxOpenings = 1 + maxReactivations;

        int openingCount = data != null ? data.openingCount() : 0;
        long lastOpeningTime = data != null ? data.lastOpeningTime() : 0;

        if (openingCount < maxOpenings && (openingCount == 0 || lastOpeningTime + cooldown < currentTime)) {
            removePlayerFromRewardedList(vaultLoc, playerId);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                int newOpeningCount = openingCount + 1;
                DatabaseHandler.saveReactivationData(vaultLoc, playerId, new ReactivationData(newOpeningCount, currentTime));

                int remainingUses = maxOpenings - newOpeningCount;
                if (newOpeningCount == 1) {
                    TextHandler.get().sendMessage(player, "replayable_vault.first_use", remainingUses, cooldown / 1000);
                } else {
                    TextHandler.get().sendMessage(player, "replayable_vault.remaining_uses", remainingUses);
                }

                addPlayerToRewardedList(vaultLoc, playerId);
            }, 20L);
        } else {
            event.setCancelled(true);
            if (openingCount >= maxOpenings) {
                TextHandler.get().sendMessage(player, "replayable_vault.max_uses_reached");
            } else {
                long timeLeft = (lastOpeningTime + cooldown - currentTime) / 1000;
                TextHandler.get().sendMessage(player, "replayable_vault.cooldown", timeLeft);
            }
        }
    }

    private void removePlayerFromRewardedList(Location vaultLoc, UUID playerId) {
        Block block = vaultLoc.getBlock();
        if (block.getType() != Material.VAULT) return;

        RtagBlock editor = RtagBlock.of(block);
        List<String> rewardedPlayers = editor.get("server_data", "rewarded_players");

        if (rewardedPlayers != null && rewardedPlayers.remove(playerId.toString())) {
            editor.set(rewardedPlayers, "server_data", "rewarded_players");
            editor.load();
        }
    }

    private void addPlayerToRewardedList(Location vaultLoc, UUID playerId) {
        Block block = vaultLoc.getBlock();
        if (block.getType() != Material.VAULT) return;

        RtagBlock editor = RtagBlock.of(block);
        List<String> rewardedPlayers = editor.get("server_data", "rewarded_players");

        if (rewardedPlayers == null) {
            rewardedPlayers = new ArrayList<>();
        }

        if (!rewardedPlayers.contains(playerId.toString())) {
            rewardedPlayers.add(playerId.toString());
            editor.set(rewardedPlayers, "server_data", "rewarded_players");
            editor.load();
        }
    }
}