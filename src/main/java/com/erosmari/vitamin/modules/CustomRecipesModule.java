package com.erosmari.vitamin.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomRecipesModule implements Listener {

    public CustomRecipesModule(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerRecipes();
    }

    public void registerRecipes() {
        ItemStack obsidian = new ItemStack(Material.OBSIDIAN,2);
        ShapelessRecipe obsidianRecipe = new ShapelessRecipe(new NamespacedKey("vitamin", "obsidian_from_lava_and_ice"), obsidian);
        obsidianRecipe.addIngredient(Material.LAVA_BUCKET);
        obsidianRecipe.addIngredient(Material.PACKED_ICE);
        Bukkit.addRecipe(obsidianRecipe);

        ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
        ShapedRecipe netherStarRecipe = new ShapedRecipe(new NamespacedKey("vitamin", "nether_star_from_wither"), netherStar);
        netherStarRecipe.shape("WWW", "SSS", "BSB");
        netherStarRecipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        netherStarRecipe.setIngredient('S', Material.SOUL_SAND);
        netherStarRecipe.setIngredient('B', Material.BLAZE_ROD);
        Bukkit.addRecipe(netherStarRecipe);

        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ShapedRecipe elytraRecipe = new ShapedRecipe(new NamespacedKey("vitamin", "elytra_from_membrane_and_chestplate"), elytra);
        elytraRecipe.shape("P P", "P P", " L ");
        elytraRecipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
        elytraRecipe.setIngredient('L', Material.NETHERITE_CHESTPLATE);
        Bukkit.addRecipe(elytraRecipe);

        ItemStack enchantedGoldenApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        ShapedRecipe enchantedAppleRecipe = new ShapedRecipe(new NamespacedKey("vitamin", "enchanted_golden_apple"), enchantedGoldenApple);
        enchantedAppleRecipe.shape("GAG", "AGA", "GAG");
        enchantedAppleRecipe.setIngredient('G', Material.GOLD_BLOCK);
        enchantedAppleRecipe.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(enchantedAppleRecipe);

        ItemStack gildedBlackstone = new ItemStack(Material.GILDED_BLACKSTONE);
        ShapelessRecipe gildedBlackstoneRecipe = new ShapelessRecipe(new NamespacedKey("vitamin", "gilded_blackstone"), gildedBlackstone);
        gildedBlackstoneRecipe.addIngredient(Material.BLACKSTONE);
        gildedBlackstoneRecipe.addIngredient(Material.GOLD_NUGGET);
        Bukkit.addRecipe(gildedBlackstoneRecipe);

        ItemStack heartOfTheSea = new ItemStack(Material.HEART_OF_THE_SEA);
        ShapedRecipe heartOfSeaRecipe = new ShapedRecipe(new NamespacedKey("vitamin", "heart_of_the_sea"), heartOfTheSea);
        heartOfSeaRecipe.shape("NSN", "SCS", "NSN");
        heartOfSeaRecipe.setIngredient('N', Material.NAUTILUS_SHELL);
        heartOfSeaRecipe.setIngredient('S', Material.PRISMARINE_SHARD);
        heartOfSeaRecipe.setIngredient('C', Material.PRISMARINE_CRYSTALS);
        Bukkit.addRecipe(heartOfSeaRecipe);

        ItemStack horseArmor = new ItemStack(Material.IRON_HORSE_ARMOR);
        ShapelessRecipe horseArmorRecipe = new ShapelessRecipe(new NamespacedKey("vitamin", "iron_horse_armor"), horseArmor);
        horseArmorRecipe.addIngredient(Material.SADDLE);
        horseArmorRecipe.addIngredient(Material.IRON_INGOT);
        horseArmorRecipe.addIngredient(Material.IRON_INGOT);
        horseArmorRecipe.addIngredient(Material.IRON_INGOT);
        horseArmorRecipe.addIngredient(Material.IRON_INGOT);
        Bukkit.addRecipe(horseArmorRecipe);

        ItemStack goldenHorseArmor = new ItemStack(Material.GOLDEN_HORSE_ARMOR);
        ShapelessRecipe goldenHorseArmorRecipe = new ShapelessRecipe(new NamespacedKey("vitamin", "golden_horse_armor"), goldenHorseArmor);
        goldenHorseArmorRecipe.addIngredient(Material.SADDLE);
        goldenHorseArmorRecipe.addIngredient(Material.GOLD_INGOT);
        goldenHorseArmorRecipe.addIngredient(Material.GOLD_INGOT);
        goldenHorseArmorRecipe.addIngredient(Material.GOLD_INGOT);
        goldenHorseArmorRecipe.addIngredient(Material.GOLD_INGOT);
        Bukkit.addRecipe(goldenHorseArmorRecipe);

        ItemStack diamondHorseArmor = new ItemStack(Material.DIAMOND_HORSE_ARMOR);
        ShapelessRecipe diamondHorseArmorRecipe = new ShapelessRecipe(new NamespacedKey("vitamin", "diamond_horse_armor"), diamondHorseArmor);
        diamondHorseArmorRecipe.addIngredient(Material.SADDLE);
        diamondHorseArmorRecipe.addIngredient(Material.DIAMOND);
        diamondHorseArmorRecipe.addIngredient(Material.DIAMOND);
        diamondHorseArmorRecipe.addIngredient(Material.DIAMOND);
        diamondHorseArmorRecipe.addIngredient(Material.DIAMOND);
        Bukkit.addRecipe(diamondHorseArmorRecipe);

        ItemStack calcite = new ItemStack(Material.CALCITE);
        ShapelessRecipe calciteRecipe = new ShapelessRecipe(new NamespacedKey("vitamin", "calcite"), calcite);
        calciteRecipe.addIngredient(Material.STONE);
        calciteRecipe.addIngredient(Material.BONE_MEAL);
        Bukkit.addRecipe(calciteRecipe);

        ItemStack deepslate = new ItemStack(Material.DEEPSLATE);
        ShapelessRecipe deepslateRecipe = new ShapelessRecipe(new NamespacedKey("vitamin", "deepslate"), deepslate);
        deepslateRecipe.addIngredient(Material.STONE);
        deepslateRecipe.addIngredient(Material.COAL);
        Bukkit.addRecipe(deepslateRecipe);

        ItemStack cobbledDeepslate = new ItemStack(Material.COBBLED_DEEPSLATE, 4);
        ShapedRecipe cobbledDeepslateRecipe = new ShapedRecipe(new NamespacedKey("vitamin", "cobbled_deepslate"), cobbledDeepslate);
        cobbledDeepslateRecipe.shape("DD", "DD");
        cobbledDeepslateRecipe.setIngredient('D', Material.DEEPSLATE);
        Bukkit.addRecipe(cobbledDeepslateRecipe);

        ItemStack tuff = new ItemStack(Material.TUFF);
        ShapelessRecipe tuffRecipe = new ShapelessRecipe(new NamespacedKey("vitamin", "tuff"), tuff);
        tuffRecipe.addIngredient(Material.STONE);
        tuffRecipe.addIngredient(Material.GRAVEL);
        Bukkit.addRecipe(tuffRecipe);

        ItemStack dirtPath = new ItemStack(Material.DIRT_PATH, 3);
        ShapedRecipe dirtPathRecipe = new ShapedRecipe(new NamespacedKey("vitamin", "dirt_path"), dirtPath);
        dirtPathRecipe.shape("DDD");
        dirtPathRecipe.setIngredient('D', Material.DIRT);
        Bukkit.addRecipe(dirtPathRecipe);

        ItemStack reinforcedDeepslate = new ItemStack(Material.REINFORCED_DEEPSLATE);
        ShapedRecipe reinforcedDeepslateRecipe = new ShapedRecipe(new NamespacedKey("vitamin", "reinforced_deepslate"), reinforcedDeepslate);
        reinforcedDeepslateRecipe.shape(" N ", "NDN", " N ");
        reinforcedDeepslateRecipe.setIngredient('D', Material.DEEPSLATE);
        reinforcedDeepslateRecipe.setIngredient('N', Material.ANCIENT_DEBRIS);
        Bukkit.addRecipe(reinforcedDeepslateRecipe);
    }
}