package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.utils.LoggingUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class ModuleManager {

    private final JavaPlugin plugin;

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        registerModules();
    }

    private void registerModules() {

        addModule("module.auto_tool", new AutoToolModule());
        addModule("module.carry_on", new CarryOnModule(plugin));
        addModule("module.double_jump", new DoubleJumpModule(plugin));
        addModule("module.elevator", new ElevatorModule(plugin));
        addModule("module.elytra_armor", new ElytraArmorModule(plugin));
        addModule("module.enchants_back", new EnchantsBackModule(plugin));
        addModule("module.fire_aspect_tools", new FireAspectOnToolsModule(plugin));
        addModule("module.invisible_item_frames", new InvisibleItemFramesModule(plugin));
        addModule("module.leaf_decay", new LeafDecayModule(plugin));
        addModule("module.pet_protection", new PetOwnerModule(plugin));
        addModule("module.player_xp_to_books", new PlayerXptoBooksModule(plugin));
        addModule("module.seed_replanter", new ReplanterModule(plugin));
        addModule("module.silk_spawners", new SilkSpawnersModule(plugin));
        addModule("module.sponge_with_lava", new SpongeWithLavaModule(plugin));
        addModule("module.totem_from_inventory", new TotemFromInventoryModule(plugin));
        addModule("module.void_totem", new VoidTotemModule(plugin));
        addModule("module.tp_to_bed_with_compass", new TpToBedModule(plugin));
        addModule("module.unlock_all_recipes", new UnlockRecipesModule(plugin));
        addModule("module.tree_vein_miner", new VeinLogModule(plugin));
        addModule("module.vein_miner", new VeinMinerModule(plugin));
        addModule("module.villager_follow_emeralds", new VillagerTauntModule(plugin));
        addModule("module.wall_jump", new WallJumpModule(plugin));
    }

    private void addModule(String configPath, Listener module) {
        if (plugin.getConfig().getBoolean(configPath, true)) {
            Bukkit.getPluginManager().registerEvents(module, plugin);
            LoggingUtils.logTranslated("module.enabled", configPath);
        } else {
            LoggingUtils.logTranslated("module.disabled", configPath);
        }
    }

    public void reloadModules() {
        plugin.reloadConfig();
        HandlerList.unregisterAll(plugin);
        registerModules();
    }
}