package com.soystargaze.vitamin.modules.core;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.Keyed;

import java.util.ArrayList;
import java.util.List;

public class CustomRecipesModule implements Listener {

    private static boolean recipesRegistered = false;
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    public CustomRecipesModule() {
        if (!recipesRegistered) {
            registerRecipes();
            recipesRegistered = true;
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof org.bukkit.entity.Player player)) return;
        Recipe recipe = event.getRecipe();
        if (recipe instanceof Keyed keyed) {
            NamespacedKey key = keyed.getKey();
            if (registeredRecipes.contains(key)) {
                String permission = "vitamin.craft." + key.getKey();
                if (!player.hasPermission(permission)) {
                    event.getInventory().setResult(new ItemStack(Material.AIR));
                }
            }
        }
    }

    public void registerRecipes() {
        NamespacedKey obsidianKey = new NamespacedKey("vitamin", "obsidian");
        Bukkit.removeRecipe(obsidianKey);
        ItemStack obsidian = new ItemStack(Material.OBSIDIAN, 2);
        ShapelessRecipe obsidianRecipe = new ShapelessRecipe(obsidianKey, obsidian);
        obsidianRecipe.addIngredient(Material.LAVA_BUCKET);
        obsidianRecipe.addIngredient(Material.PACKED_ICE);
        Bukkit.addRecipe(obsidianRecipe);
        registeredRecipes.add(obsidianKey);

        NamespacedKey netherStarKey = new NamespacedKey("vitamin", "nether_star");
        Bukkit.removeRecipe(netherStarKey);
        ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
        ShapedRecipe netherStarRecipe = new ShapedRecipe(netherStarKey, netherStar);
        netherStarRecipe.shape("WWW", "SSS", "BSB");
        netherStarRecipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        netherStarRecipe.setIngredient('S', Material.SOUL_SAND);
        netherStarRecipe.setIngredient('B', Material.BLAZE_ROD);
        Bukkit.addRecipe(netherStarRecipe);
        registeredRecipes.add(netherStarKey);

        NamespacedKey elytraKey = new NamespacedKey("vitamin", "elytra");
        Bukkit.removeRecipe(elytraKey);
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ShapedRecipe elytraRecipe = new ShapedRecipe(elytraKey, elytra);
        elytraRecipe.shape("P P", "P P", " L ");
        elytraRecipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
        elytraRecipe.setIngredient('L', Material.NETHERITE_CHESTPLATE);
        Bukkit.addRecipe(elytraRecipe);
        registeredRecipes.add(elytraKey);

        NamespacedKey enchantedAppleKey = new NamespacedKey("vitamin", "enchanted_golden_apple");
        Bukkit.removeRecipe(enchantedAppleKey);
        ItemStack enchantedGoldenApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        ShapedRecipe enchantedAppleRecipe = new ShapedRecipe(enchantedAppleKey, enchantedGoldenApple);
        enchantedAppleRecipe.shape("GAG", "AGA", "GAG");
        enchantedAppleRecipe.setIngredient('G', Material.GOLD_BLOCK);
        enchantedAppleRecipe.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(enchantedAppleRecipe);
        registeredRecipes.add(enchantedAppleKey);

        NamespacedKey gildedBlackstoneKey = new NamespacedKey("vitamin", "gilded_blackstone");
        Bukkit.removeRecipe(gildedBlackstoneKey);
        ItemStack gildedBlackstone = new ItemStack(Material.GILDED_BLACKSTONE);
        ShapelessRecipe gildedBlackstoneRecipe = new ShapelessRecipe(gildedBlackstoneKey, gildedBlackstone);
        gildedBlackstoneRecipe.addIngredient(Material.BLACKSTONE);
        gildedBlackstoneRecipe.addIngredient(Material.GOLD_NUGGET);
        Bukkit.addRecipe(gildedBlackstoneRecipe);
        registeredRecipes.add(gildedBlackstoneKey);

        NamespacedKey heartOfSeaKey = new NamespacedKey("vitamin", "heart_of_the_sea");
        Bukkit.removeRecipe(heartOfSeaKey);
        ItemStack heartOfTheSea = new ItemStack(Material.HEART_OF_THE_SEA);
        ShapedRecipe heartOfSeaRecipe = new ShapedRecipe(heartOfSeaKey, heartOfTheSea);
        heartOfSeaRecipe.shape("NSN", "SCS", "NSN");
        heartOfSeaRecipe.setIngredient('N', Material.NAUTILUS_SHELL);
        heartOfSeaRecipe.setIngredient('S', Material.PRISMARINE_SHARD);
        heartOfSeaRecipe.setIngredient('C', Material.PRISMARINE_CRYSTALS);
        Bukkit.addRecipe(heartOfSeaRecipe);
        registeredRecipes.add(heartOfSeaKey);

        NamespacedKey ironHorseArmorKey = new NamespacedKey("vitamin", "iron_horse_armor");
        Bukkit.removeRecipe(ironHorseArmorKey);
        ItemStack horseArmor = new ItemStack(Material.IRON_HORSE_ARMOR);
        ShapelessRecipe horseArmorRecipe = new ShapelessRecipe(ironHorseArmorKey, horseArmor);
        horseArmorRecipe.addIngredient(Material.SADDLE);
        horseArmorRecipe.addIngredient(Material.IRON_INGOT);
        horseArmorRecipe.addIngredient(Material.IRON_INGOT);
        horseArmorRecipe.addIngredient(Material.IRON_INGOT);
        horseArmorRecipe.addIngredient(Material.IRON_INGOT);
        Bukkit.addRecipe(horseArmorRecipe);
        registeredRecipes.add(ironHorseArmorKey);

        NamespacedKey goldenHorseArmorKey = new NamespacedKey("vitamin", "golden_horse_armor");
        Bukkit.removeRecipe(goldenHorseArmorKey);
        ItemStack goldenHorseArmor = new ItemStack(Material.GOLDEN_HORSE_ARMOR);
        ShapelessRecipe goldenHorseArmorRecipe = new ShapelessRecipe(goldenHorseArmorKey, goldenHorseArmor);
        goldenHorseArmorRecipe.addIngredient(Material.SADDLE);
        goldenHorseArmorRecipe.addIngredient(Material.GOLD_INGOT);
        goldenHorseArmorRecipe.addIngredient(Material.GOLD_INGOT);
        goldenHorseArmorRecipe.addIngredient(Material.GOLD_INGOT);
        goldenHorseArmorRecipe.addIngredient(Material.GOLD_INGOT);
        Bukkit.addRecipe(goldenHorseArmorRecipe);
        registeredRecipes.add(goldenHorseArmorKey);

        NamespacedKey diamondHorseArmorKey = new NamespacedKey("vitamin", "diamond_horse_armor");
        Bukkit.removeRecipe(diamondHorseArmorKey);
        ItemStack diamondHorseArmor = new ItemStack(Material.DIAMOND_HORSE_ARMOR);
        ShapelessRecipe diamondHorseArmorRecipe = new ShapelessRecipe(diamondHorseArmorKey, diamondHorseArmor);
        diamondHorseArmorRecipe.addIngredient(Material.SADDLE);
        diamondHorseArmorRecipe.addIngredient(Material.DIAMOND);
        diamondHorseArmorRecipe.addIngredient(Material.DIAMOND);
        diamondHorseArmorRecipe.addIngredient(Material.DIAMOND);
        diamondHorseArmorRecipe.addIngredient(Material.DIAMOND);
        Bukkit.addRecipe(diamondHorseArmorRecipe);
        registeredRecipes.add(diamondHorseArmorKey);

        NamespacedKey calciteKey = new NamespacedKey("vitamin", "calcite");
        Bukkit.removeRecipe(calciteKey);
        ItemStack calcite = new ItemStack(Material.CALCITE);
        ShapelessRecipe calciteRecipe = new ShapelessRecipe(calciteKey, calcite);
        calciteRecipe.addIngredient(Material.STONE);
        calciteRecipe.addIngredient(Material.BONE_MEAL);
        Bukkit.addRecipe(calciteRecipe);
        registeredRecipes.add(calciteKey);

        NamespacedKey deepslateKey = new NamespacedKey("vitamin", "deepslate");
        Bukkit.removeRecipe(deepslateKey);
        ItemStack deepslate = new ItemStack(Material.DEEPSLATE);
        ShapelessRecipe deepslateRecipe = new ShapelessRecipe(deepslateKey, deepslate);
        deepslateRecipe.addIngredient(Material.STONE);
        deepslateRecipe.addIngredient(Material.COAL);
        Bukkit.addRecipe(deepslateRecipe);
        registeredRecipes.add(deepslateKey);

        NamespacedKey cobbledDeepslateKey = new NamespacedKey("vitamin", "cobbled_deepslate");
        Bukkit.removeRecipe(cobbledDeepslateKey);
        ItemStack cobbledDeepslate = new ItemStack(Material.COBBLED_DEEPSLATE, 4);
        ShapedRecipe cobbledDeepslateRecipe = new ShapedRecipe(cobbledDeepslateKey, cobbledDeepslate);
        cobbledDeepslateRecipe.shape("DD", "DD");
        cobbledDeepslateRecipe.setIngredient('D', Material.DEEPSLATE);
        Bukkit.addRecipe(cobbledDeepslateRecipe);
        registeredRecipes.add(cobbledDeepslateKey);

        NamespacedKey tuffKey = new NamespacedKey("vitamin", "tuff");
        Bukkit.removeRecipe(tuffKey);
        ItemStack tuff = new ItemStack(Material.TUFF);
        ShapelessRecipe tuffRecipe = new ShapelessRecipe(tuffKey, tuff);
        tuffRecipe.addIngredient(Material.STONE);
        tuffRecipe.addIngredient(Material.GRAVEL);
        Bukkit.addRecipe(tuffRecipe);
        registeredRecipes.add(tuffKey);

        NamespacedKey dirtPathKey = new NamespacedKey("vitamin", "dirt_path");
        Bukkit.removeRecipe(dirtPathKey);
        ItemStack dirtPath = new ItemStack(Material.DIRT_PATH, 3);
        ShapedRecipe dirtPathRecipe = new ShapedRecipe(dirtPathKey, dirtPath);
        dirtPathRecipe.shape("DDD");
        dirtPathRecipe.setIngredient('D', Material.DIRT);
        Bukkit.addRecipe(dirtPathRecipe);
        registeredRecipes.add(dirtPathKey);

        NamespacedKey reinforcedDeepslateKey = new NamespacedKey("vitamin", "reinforced_deepslate");
        Bukkit.removeRecipe(reinforcedDeepslateKey);
        ItemStack reinforcedDeepslate = new ItemStack(Material.REINFORCED_DEEPSLATE);
        ShapelessRecipe reinforcedDeepslateRecipe = new ShapelessRecipe(reinforcedDeepslateKey, reinforcedDeepslate);
        reinforcedDeepslateRecipe.addIngredient(Material.DEEPSLATE);
        reinforcedDeepslateRecipe.addIngredient(Material.NETHERITE_SCRAP);
        Bukkit.addRecipe(reinforcedDeepslateRecipe);
        registeredRecipes.add(reinforcedDeepslateKey);

        NamespacedKey lavaBucketKey = new NamespacedKey("vitamin", "lava_bucket");
        Bukkit.removeRecipe(lavaBucketKey);
        ItemStack lavaBucket = new ItemStack(Material.LAVA_BUCKET);
        ShapelessRecipe lavaBucketRecipe = new ShapelessRecipe(lavaBucketKey, lavaBucket);
        lavaBucketRecipe.addIngredient(Material.WATER_BUCKET);
        lavaBucketRecipe.addIngredient(Material.MAGMA_CREAM);
        Bukkit.addRecipe(lavaBucketRecipe);
        registeredRecipes.add(lavaBucketKey);

        NamespacedKey tridentKey = new NamespacedKey("vitamin", "trident");
        Bukkit.removeRecipe(tridentKey);
        ItemStack trident = new ItemStack(Material.TRIDENT);
        ShapedRecipe tridentRecipe = new ShapedRecipe(tridentKey, trident);
        tridentRecipe.shape("SSS", "CPC", " D ");
        tridentRecipe.setIngredient('P', Material.PRISMARINE);
        tridentRecipe.setIngredient('S', Material.PRISMARINE_SHARD);
        tridentRecipe.setIngredient('C', Material.PRISMARINE_CRYSTALS);
        tridentRecipe.setIngredient('D', Material.STICK);
        Bukkit.addRecipe(tridentRecipe);
        registeredRecipes.add(tridentKey);

        NamespacedKey totemOfUndyingKey = new NamespacedKey("vitamin", "totem_of_undying");
        Bukkit.removeRecipe(totemOfUndyingKey);
        ItemStack totemOfUndying = new ItemStack(Material.TOTEM_OF_UNDYING);
        ShapedRecipe totemOfUndyingRecipe = new ShapedRecipe(totemOfUndyingKey, totemOfUndying);
        totemOfUndyingRecipe.shape(" E ", "CAC", " C ");
        totemOfUndyingRecipe.setIngredient('A', Material.ENCHANTED_GOLDEN_APPLE);
        totemOfUndyingRecipe.setIngredient('E', Material.EMERALD);
        totemOfUndyingRecipe.setIngredient('C', Material.GOLD_INGOT);
        Bukkit.addRecipe(totemOfUndyingRecipe);
        registeredRecipes.add(totemOfUndyingKey);

        NamespacedKey grassBlockKey = new NamespacedKey("vitamin", "grass_block");
        Bukkit.removeRecipe(grassBlockKey);
        ItemStack grassBlock = new ItemStack(Material.GRASS_BLOCK);
        ShapelessRecipe grassBlockRecipe = new ShapelessRecipe(grassBlockKey, grassBlock);
        grassBlockRecipe.addIngredient(Material.DIRT);
        grassBlockRecipe.addIngredient(Material.BONE_MEAL);
        Bukkit.addRecipe(grassBlockRecipe);
        registeredRecipes.add(grassBlockKey);

        NamespacedKey nameTagKey = new NamespacedKey("vitamin", "nametag");
        Bukkit.removeRecipe(nameTagKey);
        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        ShapedRecipe nameTagRecipe = new ShapedRecipe(nameTagKey, nameTag);
        nameTagRecipe.shape(" C ", " P ");
        nameTagRecipe.setIngredient('P', Material.PAPER);
        nameTagRecipe.setIngredient('C', Material.CHAIN);
        Bukkit.addRecipe(nameTagRecipe);
        registeredRecipes.add(nameTagKey);

        NamespacedKey saddleKey = new NamespacedKey("vitamin", "saddle");
        Bukkit.removeRecipe(saddleKey);
        ItemStack saddle = new ItemStack(Material.SADDLE);
        ShapedRecipe saddleRecipe = new ShapedRecipe(saddleKey, saddle);
        saddleRecipe.shape("PPP", "C C");
        saddleRecipe.setIngredient('P', Material.LEATHER);
        saddleRecipe.setIngredient('C', Material.CHAIN);
        Bukkit.addRecipe(saddleRecipe);
        registeredRecipes.add(saddleKey);
    }

    public void unregisterRecipes() {
        for (NamespacedKey key : registeredRecipes) {
            Bukkit.removeRecipe(key);
        }
        registeredRecipes.clear();
        recipesRegistered = false;
    }
}