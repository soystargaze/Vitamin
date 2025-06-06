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
                Material.STONE_BRICKS,      // 0 - project_0
                Material.SMOOTH_STONE,      // 1 - project_1
                Material.STONE_BRICKS,      // 2 - project_2
                Material.STONE_BRICKS,      // 3 - project_3
                Material.STONE_BRICKS,      // 4 - project_4
                Material.SMOOTH_STONE,      // 5 - project_5
                Material.LODESTONE          // 6 - project_6
        ));
    }


    public static boolean hasTheme(Material material) {
        return MATERIAL_THEMES.containsKey(material);
    }

    public static List<BlockDisplay> createWaystoneBlockDisplays(Location baseLoc, Material baseMaterial) {
        List<Material> themeMaterials = MATERIAL_THEMES.getOrDefault(baseMaterial, MATERIAL_THEMES.get(Material.STONE));

        List<BlockDisplay> displays = new ArrayList<>();

        // project_0: Base inferior
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(0), 1f, 0.25f, 1f, 0f, 0f, 0f));

        // project_1: Pilar superior
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(1), 0.625f, 0.9375f, 0.625f, 0.1875f, 1f, 0.1875f));

        // project_2: Marco inferior
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(2), 0.875f, 0.1875f, 0.875f, 0.0625f, 0.25f, 0.0625f));

        // project_3: Base superior
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(3), 1f, 0.25f, 1f, 0f, 1.75f, 0f));

        // project_4: Marco superior
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(4), 0.875f, 0.1875f, 0.875f, 0.0625f, 1.5625f, 0.0625f));

        // project_5: Pilar inferior
        displays.add(createBlockDisplay(baseLoc, themeMaterials.get(5), 0.625f, 0.9375f, 0.625f, 0.1875f, 0.0625f, 0.1875f));

        // project_6: Centro/núcleo (lodestone)
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
