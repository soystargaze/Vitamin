package com.soystargaze.vitamin.commands;

import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.utils.LogUtils;
import com.soystargaze.vitamin.utils.text.TextHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class RestoreInventoryListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey restoreKey;
    private final RestoreCommand restoreCommand;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public RestoreInventoryListener(JavaPlugin plugin, RestoreCommand restoreCommand) {
        this.plugin = plugin;
        this.restoreKey = new NamespacedKey(plugin, "restore_id");
        this.restoreCommand = restoreCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component titleComponent = event.getView().title();
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(titleComponent);
        
        String titleCheck = ConfigHandler.getString("gui.restore.title", "<gold>Restore:");
        String[] titleParts = titleCheck.split("%player%");

        if (titleParts.length == 0) {
            return;
        }

        String titlePrefixRaw = titleParts[0].trim();
        String plainPrefix = PlainTextComponentSerializer.plainText().serialize(parseToComponent(titlePrefixRaw));

        if (!plainTitle.startsWith(plainPrefix)) {
            return;
        }
        
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player admin)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        boolean hasRestoreKey = meta.getPersistentDataContainer().has(restoreKey, PersistentDataType.STRING);

        if (!hasRestoreKey) {
            return;
        }

        String chestId = meta.getPersistentDataContainer().get(restoreKey, PersistentDataType.STRING);

        if (chestId == null) {
            return;
        }

        ItemStack restoredContainer = restoreCommand.createRestoreContainer(chestId);
        if (restoredContainer == null) {
            TextHandler.get().sendMessage(admin, "commands.restore.error");
            return;
        }

        if (admin.getInventory().firstEmpty() == -1) {
            TextHandler.get().sendMessage(admin, "commands.restore.inventory_full");
            return;
        }

        admin.getInventory().addItem(restoredContainer);

        String containerName = clickedItem.getType().name().toLowerCase().replace("_", " ");
        TextHandler.get().sendMessage(admin, "commands.restore.success", containerName, chestId.substring(0, 8));
        LogUtils.logRestoration(admin.getName(), clickedItem.getType().name(), chestId);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> admin.closeInventory(), 1L);
    }

    private Component parseToComponent(String text) {
        if (text == null) return Component.empty();
        if (text.contains("&") || text.contains("§")) {
            return legacySerializer.deserialize(text);
        }
        return MiniMessage.miniMessage().deserialize(text);
    }
}