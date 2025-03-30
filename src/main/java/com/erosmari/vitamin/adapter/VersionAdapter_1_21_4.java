package com.erosmari.vitamin.adapter;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Sound;

public class VersionAdapter_1_21_4 implements VersionAdapter {

    @Override
    public Attribute getArmorAttribute() {
        return Attribute.ARMOR;
    }

    @Override
    public void playSlideSound(Location location, String soundKey, float volume, float pitch) {
        Sound sound = Registry.SOUND_EVENT.get(NamespacedKey.minecraft(soundKey.toLowerCase()));
        if (sound == null) {
            sound = Sound.BLOCK_SAND_STEP;
        }
        location.getWorld().playSound(location, sound, volume, pitch);
    }
}