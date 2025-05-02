package com.erosmari.vitamin.modules;

import com.erosmari.vitamin.modules.core.*;
import com.erosmari.vitamin.utils.LoggingUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;

public class ModuleManager {

    private final JavaPlugin plugin;
    private final Map<String, Listener> modules = new HashMap<>();

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        registerModules();
    }

    private void registerModules() {
        modules.clear();

        addModule("module.auto_tool", new AutoToolModule());
        addModule("module.carry_on", new CarryOnModule(plugin));
        addModule("module.crop_protection", new CropProtectionModule());
        addModule("module.custom_recipes", new CustomRecipesModule());
        addModule("module.elevator", new ElevatorModule(plugin));
        addModule("module.elytra_armor", new ElytraArmorModule(plugin));
        addModule("module.enchants_back", new EnchantsBackModule(plugin));
        addModule("module.fire_aspect_tools", new FireAspectOnToolsModule(plugin));
        addModule("module.invisible_item_frames", new InvisibleItemFramesModule());
        addModule("module.leaf_decay", new LeafDecayModule(plugin));
        addModule("module.pet_protection", new PetOwnerModule());
        addModule("module.player_xp_to_books", new PlayerXptoBooksModule(plugin));
        addModule("module.repair", new RepairModule(plugin));
        addModule("module.seed_replanter", new ReplanterModule());
        addModule("module.silk_spawners", new SilkSpawnersModule(plugin));
        addModule("module.sponge_with_lava", new SpongeWithLavaModule(plugin));
        addModule("module.totem_from_inventory", new TotemFromInventoryModule());
        addModule("module.void_totem", new VoidTotemModule(plugin));
        addModule("module.tp_to_bed_with_compass", new TpToBedModule(plugin));
        addModule("module.unlock_all_recipes", new UnlockRecipesModule(plugin));
        addModule("module.tree_vein_miner", new VeinLogModule(plugin));
        addModule("module.vein_miner", new VeinMinerModule(plugin));
        addModule("module.villager_follow_emeralds", new VillagerTauntModule(plugin));
        addModule("module.wall_jump", new WallJumpModule(plugin));
        addModule("module.weather_effects", new WeatherEffectsModule(plugin));
    }

    private void addModule(String configPath, Listener module) {
        if (plugin.getConfig().getBoolean(configPath, true)) {
            modules.put(configPath, module);
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

    public Listener getModule(String moduleName) {
        String key = moduleName.startsWith("module.") ? moduleName : "module." + moduleName;
        return modules.get(key);
    }
}