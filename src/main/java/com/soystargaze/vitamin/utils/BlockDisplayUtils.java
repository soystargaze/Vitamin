package com.soystargaze.vitamin.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class BlockDisplayUtils {

    private static final Map<Material, List<Material>> MATERIAL_THEMES = new HashMap<>();

    static {
        MATERIAL_THEMES.put(Material.STONE, Arrays.asList(
                Material.CHISELED_STONE_BRICKS,
                Material.STONE_BRICKS,
                Material.STONE,
                Material.SMOOTH_QUARTZ,
                Material.SMOOTH_QUARTZ,
                Material.STONE_BRICKS,
                Material.CHISELED_STONE_BRICKS,
                Material.SMOOTH_QUARTZ,
                Material.STONE
        ));

        MATERIAL_THEMES.put(Material.SANDSTONE, Arrays.asList(
                Material.CHISELED_SANDSTONE,
                Material.SANDSTONE,
                Material.SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.SANDSTONE,
                Material.CHISELED_SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.SANDSTONE
        ));
    }

    public static boolean hasTheme(Material material) {
        return MATERIAL_THEMES.containsKey(material);
    }

    public static List<BlockDisplay> createWaystoneBlockDisplays(Location baseLoc, Material baseMaterial) {
        List<Material> themeMaterials = MATERIAL_THEMES.getOrDefault(baseMaterial, MATERIAL_THEMES.get(Material.STONE));

        List<BlockDisplay> displays = new ArrayList<>();

        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(0), 1f, 0.25f, 1f, 0f, 0f, 0f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(1), 0.875f, 0.125f, 0.875f, 0.0625f, 0.25f, 0.0625f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(2), 0.625f, 1f, 0.625f, 0.1875f, 0.4375f, 0.1875f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(3), 0.75f, 0.125f, 0.75f, 0.125f, 0.3125f, 0.125f));

        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(4), 0.75f, 0.125f, 0.75f, 0.125f, 1.4375f, 0.125f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(5), 0.875f, 0.125f, 0.875f, 0.0625f, 1.5f, 0.0625f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(6), 1f, 0.1875f, 1f, 0f, 1.625f, 0f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(7), 0.75f, 0.125f, 0.75f, 0.125f, 1.75f, 0.125f));

        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(8), 0.625f, 0.125f, 0.625f, 0.1875f, 1.8125f, 0.1875f));

        return displays;
    }

    private static BlockDisplay createBlockDisplay(Location baseLoc, Material material,
                                                   float scaleX, float scaleY, float scaleZ,
                                                   float offsetX, float offsetY, float offsetZ) {
        BlockDisplay blockDisplay = (BlockDisplay) baseLoc.getWorld().spawnEntity(
                baseLoc.clone().add(offsetX, offsetY, offsetZ), EntityType.BLOCK_DISPLAY);

        blockDisplay.setBlock(material.createBlockData());

        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scaleX, scaleY, scaleZ),
                new AxisAngle4f(0, 0, 0, 1)
        );

        blockDisplay.setTransformation(transformation);
        blockDisplay.setBrightness(new Display.Brightness(15, 15));
        blockDisplay.addScoreboardTag("vitaminwaystone");
        blockDisplay.addScoreboardTag("waystone_" + baseLoc.getBlockX() + "_" + baseLoc.getBlockY() + "_" + baseLoc.getBlockZ());
        blockDisplay.setPersistent(true);

        return blockDisplay;
    }
}
