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
                Material.STONE_BRICKS,                      // 0 - project_0
                Material.SMOOTH_STONE,                      // 1 - project_1
                Material.STONE_BRICKS,                      // 2 - project_2
                Material.STONE_BRICKS,                      // 3 - project_3
                Material.STONE_BRICKS,                      // 4 - project_4
                Material.SMOOTH_STONE,                      // 5 - project_5
                Material.CHISELED_STONE_BRICKS              // 6 - project_6
        ));
        MATERIAL_THEMES.put(Material.SANDSTONE, Arrays.asList(
                Material.CUT_RED_SANDSTONE,                  // 0 - Base inferior
                Material.SMOOTH_RED_SANDSTONE,               // 1 - Pilar superior
                Material.SMOOTH_SANDSTONE,                   // 2 - Marco inferior
                Material.CUT_RED_SANDSTONE,                  // 3 - Base superior
                Material.SMOOTH_SANDSTONE,                   // 4 - Marco superior
                Material.SMOOTH_RED_SANDSTONE,               // 5 - Pilar inferior
                Material.CHISELED_SANDSTONE                  // 6 - Centro/núcleo
        ));
        MATERIAL_THEMES.put(Material.RED_SANDSTONE, Arrays.asList(
                Material.CUT_SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.SMOOTH_RED_SANDSTONE,
                Material.CUT_SANDSTONE,
                Material.SMOOTH_RED_SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.CHISELED_RED_SANDSTONE
        ));
        MATERIAL_THEMES.put(Material.BLACKSTONE, Arrays.asList(
                Material.POLISHED_BLACKSTONE_BRICKS,
                Material.GILDED_BLACKSTONE,
                Material.NETHERITE_BLOCK,
                Material.POLISHED_BLACKSTONE_BRICKS,
                Material.NETHERITE_BLOCK,
                Material.GILDED_BLACKSTONE,
                Material.CHISELED_POLISHED_BLACKSTONE
        ));
        MATERIAL_THEMES.put(Material.NETHERRACK, Arrays.asList(
                Material.NETHER_BRICKS,
                Material.MAGMA_BLOCK,
                Material.NETHER_BRICKS,
                Material.NETHER_BRICKS,
                Material.NETHER_BRICKS,
                Material.MAGMA_BLOCK,
                Material.CHISELED_NETHER_BRICKS
        ));
        MATERIAL_THEMES.put(Material.RESIN_BLOCK, Arrays.asList(
                Material.RESIN_BRICKS,
                Material.HONEYCOMB_BLOCK,
                Material.RESIN_BRICKS,
                Material.RESIN_BRICKS,
                Material.RESIN_BRICKS,
                Material.HONEYCOMB_BLOCK,
                Material.CHISELED_RESIN_BRICKS
        ));
    }


    public static boolean hasTheme(Material material) {
        return MATERIAL_THEMES.containsKey(material);
    }

    public static List<BlockDisplay> createWaystoneBlockDisplays(Location baseLoc, Material baseMaterial) {
        List<Material> themeMaterials = MATERIAL_THEMES.getOrDefault(baseMaterial, MATERIAL_THEMES.get(Material.STONE));

        List<BlockDisplay> displays = new ArrayList<>();

        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(0), 1f, 0.25f, 1f, 0f, 0f, 0f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(1), 0.625f, 0.9375f, 0.625f, 0.1875f, 1f, 0.1875f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(2), 0.875f, 0.1875f, 0.875f, 0.0625f, 0.25f, 0.0625f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(3), 1f, 0.25f, 1f, 0f, 1.75f, 0f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(4), 0.875f, 0.1875f, 0.875f, 0.0625f, 1.5625f, 0.0625f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(5), 0.625f, 0.9375f, 0.625f, 0.1875f, 0.0625f, 0.1875f));
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(6), 0.75f, 0.75f, 0.75f, 0.125f, 0.625f, 0.125f));

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
