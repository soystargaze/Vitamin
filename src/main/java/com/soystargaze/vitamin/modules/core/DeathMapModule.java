package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.legacy.LegacyLoggingUtils;
import com.soystargaze.vitamin.utils.text.legacy.LegacyTranslationHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.List;

@SuppressWarnings("deprecation")
public class DeathMapModule implements Listener {

    private final String displayName;
    private final List<String> lore;
    private final Scale scale;

    public DeathMapModule(JavaPlugin plugin) {
        String lang = plugin.getConfig().getString("language", "en_us");
        LegacyTranslationHandler.loadTranslations(plugin, lang);
        LegacyTranslationHandler.setActiveLanguage(lang);

        Component nameCmp = LegacyTranslationHandler.getComponent("death_map.map_item_name");
        this.displayName = ChatColor.translateAlternateColorCodes(
                '&',
                LegacyComponentSerializer.legacyAmpersand().serialize(nameCmp)
        );

        Component loreCmp = LegacyTranslationHandler.getComponent("death_map.map_item_lore");
        this.lore = List.of(
                ChatColor.translateAlternateColorCodes(
                        '&',
                        LegacyComponentSerializer.legacyAmpersand().serialize(loreCmp)
                )
        );

        this.scale = Scale.valueOf(
                plugin.getConfig()
                        .getString("death_map.map-scale", "NORMAL")
                        .toUpperCase()
        );
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!player.hasPermission("vitamin.module.death_map")
                || !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.death_chest")
        ) return;
        Location deathLoc = player.getLocation();
        DatabaseHandler.saveDeathLocation(player.getUniqueId(), deathLoc);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = DatabaseHandler.getDeathLocation(player.getUniqueId());
        if (deathLoc == null) return;

        World world = deathLoc.getWorld();
        MapView view = Bukkit.createMap(world);
        view.setCenterX(deathLoc.getBlockX());
        view.setCenterZ(deathLoc.getBlockZ());
        view.setScale(scale);
        view.setScale(scale);

        view.setTrackingPosition(true);
        view.addRenderer(new MapRenderer() {
            private boolean rendered = false;
            @Override
            public void render(@NotNull MapView mv,
                               @NotNull MapCanvas canvas,
                               @NotNull Player p) {
                if (rendered) return;
                canvas.setPixelColor(64, 64, new Color(255, 0, 0));
                rendered = true;
            }
        });

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP, 1);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setMapView(view);
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            mapItem.setItemMeta(meta);
        }
        player.getInventory().addItem(mapItem);

        LegacyLoggingUtils.sendMessage(
                player,
                "death_map.map_given",
                deathLoc.getBlockX(),
                deathLoc.getBlockY(),
                deathLoc.getBlockZ()
        );
        LegacyLoggingUtils.logTranslated(
                "death_map.map_given",
                deathLoc.getBlockX(),
                deathLoc.getBlockY(),
                deathLoc.getBlockZ()
        );
    }
}