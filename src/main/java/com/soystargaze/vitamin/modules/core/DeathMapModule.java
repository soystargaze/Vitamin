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
import org.bukkit.event.server.MapInitializeEvent;
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

    private final JavaPlugin plugin;
    private final String displayName;
    private final List<String> lore;
    private final Scale scale;

    public DeathMapModule(JavaPlugin plugin) {
        this.plugin = plugin;

        Component nameCmp = LegacyTranslationHandler.getComponent("death_map.map_item_name");
        String rawName = LegacyComponentSerializer.legacyAmpersand().serialize(nameCmp);
        this.displayName = ChatColor.translateAlternateColorCodes('&', rawName);

        Component loreCmp = LegacyTranslationHandler.getComponent("death_map.map_item_lore");
        String rawLore = LegacyComponentSerializer.legacyAmpersand().serialize(loreCmp);
        this.lore = List.of(ChatColor.translateAlternateColorCodes('&', rawLore));

        this.scale = Scale.valueOf(
                plugin.getConfig().getString("death_map.map-scale", "NORMAL").toUpperCase()
        );

        Bukkit.getScheduler().runTask(plugin, this::reapplyRenderers);
    }

    private void reapplyRenderers() {
        for (short mapId : DatabaseHandler.getDeathMapIds()) {
            try {
                MapView view = Bukkit.getMap(mapId);
                if (view != null) {
                    view.setTrackingPosition(true);
                    view.setUnlimitedTracking(true);
                    view.addRenderer(createDeathRenderer());
                }
            } catch (Throwable t) {
                LegacyLoggingUtils.logTranslated("death_map.map_renderer_error", mapId);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!player.hasPermission("vitamin.module.death_map")
                || !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.death_map")
        ) return;
        Location deathLoc = player.getLocation();
        DatabaseHandler.saveDeathLocation(player.getUniqueId(), deathLoc);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.death_map")
                || !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.death_map")
        ) return;
        Location deathLoc = DatabaseHandler.getDeathLocation(player.getUniqueId());
        if (deathLoc == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            World world = deathLoc.getWorld();
            MapView view = Bukkit.createMap(world);
            view.setCenterX(deathLoc.getBlockX());
            view.setCenterZ(deathLoc.getBlockZ());
            view.setScale(scale);
            view.setTrackingPosition(true);
            view.setUnlimitedTracking(true);
            view.addRenderer(createDeathRenderer());

            DatabaseHandler.saveDeathMapId(player.getUniqueId(), (short) view.getId());

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
        });
    }

    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        short id = (short) event.getMap().getId();
        if (DatabaseHandler.getDeathMapIds().contains(id)) {
            MapView view = event.getMap();
            view.setTrackingPosition(true);
            view.setUnlimitedTracking(true);
            view.addRenderer(createDeathRenderer());
        }
    }

    private MapRenderer createDeathRenderer() {
        return new MapRenderer() {
            private boolean rendered = false;

            @Override
            public void render(@NotNull MapView mv,
                               @NotNull MapCanvas canvas,
                               @NotNull Player p) {
                if (rendered) return;
                int cx = 64, cy = 64;
                Color black = new Color(0, 0, 0);
                Color white = new Color(255, 255, 255);


                for (int d = -2; d <= 2; d++) {
                    drawSquare(canvas, cx + d, cy + d, black);
                    drawSquare(canvas, cx + d, cy - d, black);
                }
                for (int d = -1; d <= 1; d++) {
                    canvas.setPixelColor(cx + d, cy + d, white);
                    canvas.setPixelColor(cx + d, cy - d, white);
                }

                rendered = true;
            }

            private void drawSquare(MapCanvas canvas, int x, int y, Color color) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        canvas.setPixelColor(x + dx, y + dy, color);
                    }
                }
            }
        };
    }
}