package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public class HealthIndicatorModule implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, String> entityOriginalNames = new ConcurrentHashMap<>();
    private static final double HOLOGRAM_VIEW_DISTANCE = 15.0;
    private static final int HEALTH_UPDATE_TICKS = 10;
    private static final int HOLOGRAM_LIFETIME_TICKS = 15;

    public HealthIndicatorModule(JavaPlugin plugin) {
        this.plugin = plugin;
        startTasks();
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity livingEntity) || entity instanceof Player || entity instanceof ArmorStand) {
            return;
        }

        UUID entityId = entity.getUniqueId();
        String originalName = livingEntity.getCustomName();
        if (originalName != null) {
            entityOriginalNames.put(entityId, originalName);
        }

        if (livingEntity.isValid()) {
            updateEntityHealthDisplay(livingEntity);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;

        Entity damager = event.getDamager();
        Entity entity = event.getEntity();

        if (!(damager instanceof Player player) || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (!player.hasPermission("vitamin.module.health_indicator") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.health_indicator")) {
            return;
        }

        double damage = event.getFinalDamage();
        boolean isCritical = event.isCritical();

        if (player.getLocation().distance(livingEntity.getLocation()) <= HOLOGRAM_VIEW_DISTANCE) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && livingEntity.isValid()) {
                    showDamageHologram(livingEntity, damage, isCritical);
                }
            });
        }

        if (livingEntity.isValid()) {
            updateEntityHealthDisplay(livingEntity);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        entityOriginalNames.remove(event.getEntity().getUniqueId());
    }

    private void startTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                processHealthUpdates();
            }
        }.runTaskTimer(plugin, 0L, HEALTH_UPDATE_TICKS);
    }

    private void processHealthUpdates() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity livingEntity) ||
                        entity instanceof Player ||
                        entity instanceof ArmorStand ||
                        !entity.isValid() ||
                        entity.isDead()) {
                    continue;
                }

                boolean playerNearby = false;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.hasPermission("vitamin.module.health_indicator") ||
                            !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.health_indicator")) {
                        return;
                    }

                    if (player.getWorld().equals(world) &&
                            player.getLocation().distance(entity.getLocation()) <= HOLOGRAM_VIEW_DISTANCE) {
                        playerNearby = true;
                        break;
                    }
                }

                if (playerNearby) {
                    updateEntityHealthDisplay(livingEntity);
                } else {
                    restoreOriginalName(livingEntity);
                }
            }
        }
    }

    private void updateEntityHealthDisplay(LivingEntity entity) {
        if (entity instanceof ArmorStand || entity instanceof Player || !entity.isValid()) {
            return;
        }

        double health = entity.getHealth();
        AttributeInstance maxHPInstance = entity.getAttribute(Vitamin.getInstance().getVersionAdapter().getMaxHPAttribute());
        if (maxHPInstance == null) {
            return;
        }

        double maxHealth = maxHPInstance.getValue();
        double healthPercentage = health / maxHealth;

        ChatColor healthColor = getHealthColor(healthPercentage);
        String healthDisplay = healthColor + "❤" + ChatColor.WHITE + " " + String.format("%.1f", health) + "/" + String.format("%.1f", maxHealth);

        UUID entityId = entity.getUniqueId();
        String originalName = entityOriginalNames.getOrDefault(entityId, getEntityDisplayName(entity.getType()));

        if (!originalName.isEmpty()) {
            healthDisplay = ChatColor.WHITE + originalName + " " + healthDisplay;
        }

        entity.setCustomName(healthDisplay);
        entity.setCustomNameVisible(true);
    }

    private ChatColor getHealthColor(double healthPercentage) {
        if (healthPercentage > 0.8) return ChatColor.GREEN;
        if (healthPercentage > 0.6) return ChatColor.YELLOW;
        if (healthPercentage > 0.4) return ChatColor.GOLD;
        if (healthPercentage > 0.2) return ChatColor.RED;
        return ChatColor.DARK_RED;
    }

    private void restoreOriginalName(LivingEntity entity) {
        String originalName = entityOriginalNames.get(entity.getUniqueId());
        if (originalName != null) {
            entity.setCustomName(originalName);
            entity.setCustomNameVisible(!originalName.isEmpty());
        } else {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
    }

    private String getEntityDisplayName(EntityType type) {
        String name = type.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private void showDamageHologram(LivingEntity entity, double damage, boolean isCritical) {
        Location location = entity.getLocation().add(0, entity.getHeight(), 0);
        ArmorStand hologram = createDamageHologram(location);

        String damageText = (isCritical ? ChatColor.GOLD : ChatColor.RED) +
                "-" + String.format("%.1f", damage) + " ❤";
        hologram.setCustomName(damageText);

        Location initialLoc = hologram.getLocation().clone();
        final double v0 = 0.075;
        final double g = 0.01;
        final double horizontalSpeed = -0.02;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= HOLOGRAM_LIFETIME_TICKS || !hologram.isValid()) {
                    hologram.remove();
                    this.cancel();
                    return;
                }

                double deltaY = v0 * ticks - 0.5 * g * ticks * ticks;
                double deltaZ = horizontalSpeed * ticks;

                Location newLoc = initialLoc.clone();
                newLoc.add(0, deltaY, deltaZ);
                hologram.teleport(newLoc);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ArmorStand createDamageHologram(Location location) {
        ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCustomNameVisible(true);
        hologram.setMarker(true);
        hologram.setSmall(true);
        hologram.setCanPickupItems(false);
        hologram.setInvulnerable(true);
        hologram.setBasePlate(false);
        hologram.setArms(false);
        return hologram;
    }
}