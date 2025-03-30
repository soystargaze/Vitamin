package com.erosmari.vitamin.adapter;

import org.bukkit.attribute.Attribute;
import org.bukkit.Location;

public class VersionAdapter_1_21_1 implements VersionAdapter {

    private final Attribute genericArmor;

    @SuppressWarnings("JavaReflectionMemberAccess")
    public VersionAdapter_1_21_1() {
        Attribute temp = null;
        try {
            temp = (Attribute) Attribute.class.getField("GENERIC_ARMOR").get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.genericArmor = temp;
    }

    @Override
    public Attribute getArmorAttribute() {
        return genericArmor;
    }

    @Override
    public void playSlideSound(Location location, String soundKey, float volume, float pitch) {
    }
}