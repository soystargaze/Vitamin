package com.soystargaze.vitamin.adapter;

import org.bukkit.attribute.Attribute;
import org.bukkit.Location;

public interface VersionAdapter {
    Attribute getArmorAttribute();

    Attribute getMaxHPAttribute();

    void playSlideSound(Location location, String soundKey, float volume, float pitch);
}