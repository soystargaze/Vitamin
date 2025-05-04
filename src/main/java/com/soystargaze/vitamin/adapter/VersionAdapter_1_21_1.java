package com.soystargaze.vitamin.adapter;

import org.bukkit.attribute.Attribute;
import org.bukkit.Location;

public class VersionAdapter_1_21_1 implements VersionAdapter {

    private final Attribute genericArmor;
    private final Attribute genericMaxHP;

    @SuppressWarnings("JavaReflectionMemberAccess")
    public VersionAdapter_1_21_1() {
        Attribute arm = null;
        try {
            arm = (Attribute) Attribute.class.getField("GENERIC_ARMOR").get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.genericArmor = arm;

        Attribute mhp = null;
        try {
            mhp = (Attribute) Attribute.class.getField("GENERIC_MAX_HEALTH").get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.genericMaxHP = mhp;
    }

    @Override
    public Attribute getArmorAttribute() {
        return genericArmor;
    }

    @Override
    public Attribute getMaxHPAttribute() {
        return genericMaxHP;
    }

    @Override
    public void playSlideSound(Location location, String soundKey, float volume, float pitch) {
    }
}