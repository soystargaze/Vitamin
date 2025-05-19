# Vitamin+ | Take Vanilla to the next level

**Vitamin+** is your Minecraft power-up—boosting gameplay with new mechanics and seamless upgrades while keeping the vanilla charm. Level up your experience without changing the game you love!

[![Discord](https://img.shields.io/discord/1079917552588816484?label=Discord&logo=discord&logoColor=white&color=FFA500&style=for-the-badge)](https://erosmari.com/discord)  ![Made with ❤️ by Stargaze](https://img.shields.io/badge/Made%20with-%E2%9D%A4%EF%B8%8F%20by%20stargaze-FFA500?style=for-the-badge)

![Banner Logo](https://cdn.modrinth.com/data/wKw0THQX/images/2a588e8eda6a2dba50af9305e97d5f60679817b6.png)

---

## 🍊 Key Features

- **Full compatibility with Spigot and PaperMC**
- **Easy configuration:** Manage everything from `config.yml`
- **Modular system:** Enable or disable each feature as needed
- **Permissions support:** Controls player access to specific modules and crafting recipes
- **New mechanics:** Features designed to enhance gameplay without breaking the vanilla feel
- **Multi-language support:** Easily translatable, with preconfigured translations for **English**, **Spanish**, **Chinese**, **German**, **Polish**, **French**, **Korean**, **Japanese**, **Turkish** and **Brazilian Portuguese**

---

## 💊 Available Modules

**Vitamin+** comes loaded with **32 great power-ups** to enhance your gameplay!

<details>
  <summary>Explore</summary>

- **Armor Trim:** The armor trim gives special effects depending on the material, some effects are stackable. (Triggered with `Shift` or Passive effects)
  <details>
    <summary>Materials</summary>

  - **Copper**: Grants *Haste* for **15 seconds** when sneaking is initiated, with a **60-second cooldown**. **Stackable**.
  - **Iron**: Attracts items with a magnet effect within a radius of **1.0 blocks per piece**. **Stackable**.
  - **Redstone**: Gives *Speed* for **5 seconds** when sneaking is initiated, with an increase of **0.05 per piece**. **Stackable**.
  - **Emerald**: Applies *Hero of the Village* when opening merchant inventories (up to **5 stacks**). **Stackable**.
  - **Netherite**: Provides *Fire Resistance* for **10 seconds** with a **30-second cooldown** and *Knockback Resistance*. **Stackable**.
  - **Lapis**: Boosts XP gain by **10% per piece** and may refund between **5% and 15% of XP** when enchanting. **Stackable**.
  - **Quartz**: Grants *Night Vision* while sneaking (requires staying crouched). **Not stackable**.
  - **Diamond**: Increases armor effectiveness by **8% per piece**. **Stackable**.
  - **Amethyst**: Grants *Regeneration* to self and nearby players for **8 seconds** when damaged, with a **15-second cooldown** and a radius of **5.0 blocks**. **Stackable**.
  - **Gold**: Prevents Piglins from attacking unless provoked. **Not stackable** (only one piece needed).
  </details>
- **Auto Tool:** Automatically switches to the best tool when breaking blocks or attacking entities.
- **Bone Meal Expansion:** Use bone meal on crops that are not normally affected by it.
- **Carry On:** Use `Shift + Right-Click (empty hand)` to carry entities and chests. Respects regions protected with [WorldGuard](https://dev.bukkit.org/projects/worldguard) or [Lands](https://www.spigotmc.org/resources/lands-⭕-land-claim-plugin-✅-grief-prevention-protection-gui-management-nations-wars-1-21-support.53313/) to work with members only, also respects `Lootin Chests` if [Lootin](https://www.spigotmc.org/resources/lootin-1-16-1-21-5-no-more-already-looted-chests.90453/) is in the server (can be disabled).
- **Crop Protection:** Prevents crops from being trampled by players.
- **Death Chest:** Automatically creates a chest with your items when you die if it finds a safe location (configurable).
- **Death Map:** It gives you a map with the location of your death on respawn (configurable).
- **Elevator:** Create a teleporting elevator for vertical movement.
- **Elytra Armor:** Elytra provides protection similar to Netherite chestplates (configurable).
- **Enchants Back:** Recover enchantments when disenchanting using empty books in your inventory (configurable). It can also return the [AdvancedEnchantments](https://www.spigotmc.org/resources/1-17-1-21-5-⭕-advancedenchantments-⭐-500-custom-enchants-⭐create-custom-enchantments-✅.43058/) enchantments.
- **Fire Aspect On Tools:** You can use an anvil to apply `Fire Aspect I/II` to tools. It auto-smelts drops when breaking blocks. `Level I` has a 40% chance, while `Level II` has a 100% chance.
- **Health Indicator:** Displays the health and damage to mobs.
- **Invisible Item Frames:** Toggle the visibility of item frames with `Shift + Right-Click (empty hand)`.
- **Leaf Decay:** Leaves disappear faster when cutting down trees (configurable).
- **Oxygen Bottle:** Using an empty bottle in the water gives you some oxygen to breathe (configurable).
- **Pet Protection:** Prevents pets from being accidentally damaged by their owners.
- **Player XP to Books:** Convert your XP into books by `Shift + Right-Clicking` with an empty book.
- **Repair Tools:** Repairs gold, iron and diamond tools and weapons using nuggets, ingots or diamonds in the player's 2x2 inventory crafting grid.
- **Seed Replanter:** Harvest and replant crops with a `Right-Click`.
- **Replayable Vault:** Reuse Trial Vaults after a cooldown period, with a maximum number of uses.
- **Silk Spawners:** Obtain spawners when mining them with `Silk Touch`.
- **Sponge with Lava:** Sponges can now absorb lava.
- **Totem from Inventory:** The Totem of Undying works from anywhere in your inventory.
- **Void Totem:** The Totem of Undying activates when falling into the void.
- **TP to Bed with Compass:** Use a compass with `Right-click` to teleport to your spawn point (configurable).
- **Unlock All Recipes:** Unlock all crafting recipes upon joining the server.
- **Tree Vein Miner:** Chop down entire trees when using `Efficiency V` tools. Hold `Shift` to chop trees one block at a time.
- **Vein Miner:** Mine connected ores in a vein using `Efficiency V` tools. Hold `Shift` to mine ores one block at a time.
- **Villager Follow Emeralds:** Villagers follow players holding emeralds or dropped in the ground.
- **Wall Jump:** Use `Shift` to propel yourself or slide off walls (configurable).
- **Weather Effects:** Rain causes crops to grow faster and sunshine allows mobs to have more babies.
- **Custom Recipes:** Adds new crafting recipes for items that are not normally craftable.
  - [Recipes](https://imgur.com/a/3tePcrc) (You can enable/disable specific recipes using the permissions)

</details>

---

## 📌 Installation Guide

<details><summary>Installation</summary>

## **Prerequisites**
Before installing Vitamin+, make sure your server meets the following requirements:

- **Minecraft Server:** PaperMC **1.21 or higher** (recommended **1.21.4**, the latest stable version).
- **Java:** Version **21 or higher**.

---

## **Step 1: Download the Plugin**
Download the latest version of **Vitamin+** from [Modrinth](https://modrinth.com/plugin/vitamin) and ensure you obtain a valid `.jar` file.

---

## **Step 2: Installation**
1. **Upload the file** `Vitamin.jar` to the `plugins/` folder of your PaperMC server.
2. **Restart the server** to automatically generate the configuration files.
3. **Verify installation** by checking the console. If the installation was successful, you will see a message indicating that the plugin has been loaded correctly.

---

## **Step 3: Initial Configuration**
1. **Navigate to the configuration folder:** `plugins/Vitamin/`
2. **Edit `config.yml`** to adjust settings as you like.
3. **Restart** the server or **reload** the plugin.

---

## **Step 4: Troubleshooting**
- **The plugin does not load:** It is recommended to use **PaperMC 1.21.4**, the latest stable version. Also, ensure you are using Java 21 or higher.

---

## **Support & Contact**
If you encounter issues or have questions, contact support on **[Discord](https://erosmari.com/discord)** or refer to the official plugin documentation.

</details>

---

## 🔐 Commands & Permissions

<details>
  <summary>Commands</summary>

**Vitamin+** also provides a variety of aliases for each command `/vitamin`, `/vita`, and `/vi`.

- `/vitamin module <module> <enable/disable>` - Enables or disables a specific module globally.
- `/vitamin pmodule <module> <enable/disable>` - Enables or disables a specific module individually if enabled globally and has permissions.
- `/vitamin reload` - Reloads the plugin configuration.

</details>

<details>
  <summary>Permissions</summary>

- `vitamin.use` - Allows the use of Vitamin+ commands.
- `vitamin.module` - Grants permission to modify module states.
- `vitamin.pmodule` - Grants permission to modify individual module states if they are enabled globally and have permissions to use them.
- `vitamin.reload` - Allows reloading the plugin configuration.

---

- `vitamin.module.*` - Grants permission to use all modules.
- `vitamin.module.armor_trim` - Allows the use of the Armor Trim module.
- `vitamin.module.auto_tool` - Allows the use of the Auto Tool module.
- `vitamin.module.bone_meal_expansion` - Allows the use of the Bone Meal Expansion module.
- `vitamin.module.carry_on` - Allows the use of the Carry On module.
- `vitamin.module.carry_on.chest` - Allows the pickup of chests with the Carry On module.
- `vitamin.module.carry_on.entity` - Allows the pickup of entities with the Carry On module.
- `vitamin.module.crop_protection` - Allows the use of the Crop Protection module.
- `vitamin.module.death_chest` - Allows the use of the Death Chest module.
- `vitamin.module.death_map` - Allows the use of the Death Map module.
- `vitamin.module.elevator` - Allows the use of the Elevator module.
- `vitamin.module.elytra_armor` - Allows the use of the Elytra Armor module.
- `vitamin.module.enchants_back` - Allows the use of the Enchants Back module.
- `vitamin.module.fire_aspect_tools` - Allows the use of the Fire Aspect On Tools module.
- `vitamin.module.health_indicator` - Allows the use of the Health Indicator module.
- `vitamin.module.invisible_item_frames` - Allows the use of the Invisible Item Frames module.
- `vitamin.module.leaf_decay` - Allows the use of the Leaf Decay module.
- `vitamin.module.oxygen_bottle` - Allows the use of the Oxygen Bottle module.
- `vitamin.module.pet_protection` - Allows the use of the Pet Protection module.
- `vitamin.module.xp_books` - Allows the use of the Player XP to Books module.
- `vitamin.module.repair` - Allows the use of the Repair Tools module.
- `vitamin.module.seed_replanter` - Allows the use of the Seed Replanter module.
- `vitamin.module.replayable_vault` - Allows the use of the Replayable Vault module.
- `vitamin.module.silk_spawners` - Allows the use of the Silk Spawners module.
- `vitamin.module.sponge_with_lava` - Allows the use of the Sponge with Lava module.
- `vitamin.module.totem_from_inventory` - Allows the use of the Totem from Inventory module.
- `vitamin.module.void_totem` - Allows the use of the Void Totem module.
- `vitamin.module.tp_compass` - Allows the use of the TP to Bed with Compass module.
- `vitamin.module.unlock_all_recipes` - Allows the use of the Unlock All Recipes module.
- `vitamin.module.tree_vein_miner` - Allows the use of the Tree Vein Miner module.
- `vitamin.module.vein_miner` - Allows the use of the Vein Miner module.
- `vitamin.module.villager_follow_emeralds` - Allows the use of the Villager Follow Emeralds module.
- `vitamin.module.wall_jump` - Allows the use of the Wall Jump module.
- `vitamin.module.weather_effects` - Allows the use of the Weather Effects module.

---

- `vitamin.craft.*` - Grants permission to use all custom recipes.
- `vitamin.craft.obsidian` - Allows the use of the Obsidian recipe.
- `vitamin.craft.nether_star` - Allows the use of the Nether Star recipe.
- `vitamin.craft.elytra` - Allows the use of the Elytra recipe.
- `vitamin.craft.enchanted_golden_apple` - Allows the use of the Enchanted Golden Apple recipe.
- `vitamin.craft.gilded_blackstone` - Allows the use of the Gilded Blackstone recipe.
- `vitamin.craft.hearts_of_the_sea` - Allows the use of the Heart of the Sea recipe.
- `vitamin.craft.iron_horse_armor` - Allows the use of the Iron Horse Armor recipe.
- `vitamin.craft.golden_horse_armor` - Allows the use of the Golden Horse Armor recipe.
- `vitamin.craft.diamond_horse_armor` - Allows the use of the Diamond Horse Armor recipe.
- `vitamin.craft.calcite` - Allows the use of the Calcite recipe.
- `vitamin.craft.deepslate` - Allows the use of the Deepslate recipe.
- `vitamin.craft.cobbled_deepslate` - Allows the use of the Cobbled Deepslate recipe.
- `vitamin.craft.tuff` - Allows the use of the Tuff recipe.
- `vitamin.craft.dirt_path` - Allows the use of the Dirt Path recipe.
- `vitamin.craft.reinforced_deepslate` - Allows the use of the Reinforced Deepslate recipe.
- `vitamin.craft.lava_bucket` - Allows the use of the Lava Bucket recipe.
- `vitamin.craft.trident` - Allows the use of the Trident recipe.
- `vitamin.craft.totem_of_undying` - Allows the use of the Totem of Undying recipe.
- `vitamin.craft.grass_block` - Allows the use of the Grass Block recipe.
- `vitamin.craft.nametag` - Allows the use of the Name Tag recipe.
- `vitamin.craft.saddle` - Allows the use of the Saddle recipe.

</details> 

---

## 💬 Support & Contact

If you have any questions or encounter issues, feel free to contact us on [Discord](https://erosmari.com/discord).

---

💊 Enhance your **Minecraft** experience with **Vitamin+**! 💊

![bstats](https://bstats.org/signatures/bukkit/Vitamin%20-%20Vanilla%20Enhanced.svg)