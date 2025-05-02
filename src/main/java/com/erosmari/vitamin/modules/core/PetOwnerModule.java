package com.erosmari.vitamin.modules.core;

import com.erosmari.vitamin.database.DatabaseHandler;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class PetOwnerModule implements Listener {

    public PetOwnerModule() {
    }

    @EventHandler
    public void onPetDamage(final EntityDamageByEntityEvent event) {
        if (!isValidTameableVictim(event.getEntity())) {
            return;
        }

        final Tameable tameable = (Tameable) event.getEntity();
        final Player attacker = getAttackerFromDamager(event.getDamager());

        if (attacker != null) {
            if (!attacker.hasPermission("vitamin.module.pet_protection") ||
                    !DatabaseHandler.isModuleEnabledForPlayer(attacker.getUniqueId(), "module.pet_protection")) {
                return;
            }
        }

        handlePetDamage(event, tameable, attacker);
    }

    private boolean isValidTameableVictim(final Entity victim) {
        if (victim instanceof Tameable tameable) {
            return tameable.getOwner() != null;
        }
        return false;
    }

    private Player getAttackerFromDamager(final Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            final ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player;
        }
        return null;
    }

    private void handlePetDamage(final EntityDamageByEntityEvent event, final Tameable tameable, final Player attacker) {
        if (attacker != null && tameable.getOwner() != null && tameable.getOwner().equals(attacker)) {
            event.setCancelled(true);
        }
    }
}