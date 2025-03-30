package com.erosmari.vitamin.adapter;

import org.bukkit.attribute.Attribute;
import org.bukkit.Location;

public interface VersionAdapter {
    Attribute getArmorAttribute();

    void playSlideSound(Location location, String soundKey, float volume, float pitch);
}