package com.soystargaze.vitamin.modules.paper;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.modern.ModernLoggingUtils;
import com.soystargaze.vitamin.utils.text.modern.ModernTranslationHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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

public class PaperDeathMapModule implements Listener {

    private final Component displayName;
    private final List<Component> lore;
    private final Scale scale;

    public PaperDeathMapModule(JavaPlugin plugin) {
        this.displayName = ModernTranslationHandler.getComponent("death_map.map_item_name");
        this.lore = List.of( ModernTranslationHandler.getComponent("death_map.map_item_lore") );

        this.scale = Scale.valueOf(
                plugin.getConfig()
                        .getString("death_map.map_scale", "NORMAL")
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
        if (!player.hasPermission("vitamin.module.death_map")
                || !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.death_chest")
        ) return;
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

        ItemStack mapItem = createMapItem(view);
        player.getInventory().addItem(mapItem);

        ModernLoggingUtils.sendMessage(
                player,
                "death_map.map_given",
                deathLoc.getBlockX(),
                deathLoc.getBlockY(),
                deathLoc.getBlockZ()
        );
        ModernLoggingUtils.logTranslated(
                "death_map.map_given",
                deathLoc.getBlockX(),
                deathLoc.getBlockY(),
                deathLoc.getBlockZ()
        );
    }

    private ItemStack createMapItem(MapView view) {
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP, 1);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.displayName(displayName);
            meta.lore(lore);
            meta.setMapView(view);
            mapItem.setItemMeta(meta);
        }
        return mapItem;
    }
}