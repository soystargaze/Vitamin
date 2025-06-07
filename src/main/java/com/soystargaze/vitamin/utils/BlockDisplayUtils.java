package com.soystargaze.vitamin.utils;

import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.*;

public class BlockDisplayUtils {

    private static final Map<Material, List<Material>> MATERIAL_THEMES = new HashMap<>();

    static {
        MATERIAL_THEMES.put(Material.STONE, Arrays.asList(
                Material.STONE_BRICKS,          // Base inferior
                Material.SMOOTH_STONE,          // Pilar superior
                Material.STONE_BRICKS,          // Marco inferior
                Material.STONE_BRICKS,          // Base superior
                Material.STONE_BRICKS,          // Marco superior
                Material.SMOOTH_STONE,          // Pilar inferior
                Material.CHISELED_STONE_BRICKS  // Centro/núcleo
        ));
    }

    public static void loadThemes(Plugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "waystone_themes.yml");
        if (!configFile.exists()) {
            plugin.saveResource("waystone_themes.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection themesSection = config.getConfigurationSection("themes");
        if (themesSection == null) {
            TextHandler.get().logTranslated("waystone.no_themes_found");
            return;
        }

        for (String key : themesSection.getKeys(false)) {
            Material baseMaterial = Material.getMaterial(key.toUpperCase());
            if (baseMaterial == null || !baseMaterial.isBlock()) {
                TextHandler.get().logTranslated("waystone.invalid_theme_key", key);
                continue;
            }
            List<String> materialNames = themesSection.getStringList(key);
            if (materialNames.size() != 7) {
                TextHandler.get().logTranslated("waystone.theme_invalid_size", key);
                continue;
            }
            List<Material> materials = new ArrayList<>();
            for (String name : materialNames) {
                Material material = Material.getMaterial(name.toUpperCase());
                if (material == null || !material.isBlock()) {
                    TextHandler.get().logTranslated("waystone.invalid_block_material", name, key);
                    materials.clear();
                    break;
                }
                materials.add(material);
            }
            if (materials.size() == 7) {
                MATERIAL_THEMES.put(baseMaterial, materials);
            }
        }
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