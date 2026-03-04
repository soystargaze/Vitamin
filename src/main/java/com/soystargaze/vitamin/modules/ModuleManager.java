package com.soystargaze.vitamin.modules;

import com.soystargaze.vitamin.modules.core.*;
import com.soystargaze.vitamin.utils.text.TextHandler;
import com.soystargaze.vitamin.utils.updater.UpdateOnFullLoad;
import com.soystargaze.vitamin.utils.updater.UpdateOnJoinListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {

    private static final List<ModuleDef> DEFS = List.of(
            new ModuleDef("module.auto_tool",
                    p -> new AutoToolModule()
            ),
            new ModuleDef("module.crop_protection",
                    p -> new CropProtectionModule()
            ),
            new ModuleDef("module.custom_recipes",
                    p -> new CustomRecipesModule()
            ),
            new ModuleDef("module.armor_trim",
                    p -> new ArmorTrimModule()
            ),
            new ModuleDef("module.invisible_item_frames",
                    p -> new InvisibleItemFramesModule()
            ),
            new ModuleDef("module.pet_protection",
                    p -> new PetProtectionModule()
            ),
            new ModuleDef("module.seed_replanter",
                    p -> new ReplanterModule()
            ),
            new ModuleDef("module.totem_from_inventory",
                    p -> new TotemFromInventoryModule()
            ),
            new ModuleDef("module.bone_meal_expansion",
                    BoneMealExpansionModule::new
            ),
            new ModuleDef("module.campfire_tweaks",
                    CampfireTweaksModule::new
            ),
            new ModuleDef("module.death_chest",
                    DeathChestModule::new
            ),
            new ModuleDef("module.elytra_armor",
                    ElytraArmorModule::new
            ),
            new ModuleDef("module.enchants_back",
                    EnchantsBackModule::new
            ),
            new ModuleDef("module.fire_aspect_tools",
                    FireAspectOnToolsModule::new
            ),
            new ModuleDef("module.leaf_decay",
                    LeafDecayModule::new
            ),
            new ModuleDef("module.oxygen_bottle",
                    OxygenBottleModule::new
            ),
            new ModuleDef("module.repair",
                    RepairModule::new
            ),
            new ModuleDef("module.replayable_vault",
                    ReplayableVaultModule::new),
            new ModuleDef("module.silk_spawners",
                    SilkSpawnersModule::new
            ),
            new ModuleDef("module.sponge_with_lava",
                    SpongeWithLavaModule::new
            ),
            new ModuleDef("module.void_totem",
                    VoidTotemModule::new
            ),
            new ModuleDef("module.unlock_all_recipes",
                    UnlockRecipesModule::new
            ),
            new ModuleDef("module.tree_vein_miner",
                    VeinLogModule::new
            ),
            new ModuleDef("module.vein_miner",
                    VeinMinerModule::new
            ),
            new ModuleDef("module.weather_effects",
                    WeatherEffectsModule::new
            ),
            new ModuleDef("module.death_map",
                    DeathMapModule::new
            ),
            new ModuleDef("module.elevator",
                    ElevatorModule::new
            ),
            new ModuleDef("module.player_xp_to_books",
                    PlayerXptoBooksModule::new
            ),
            new ModuleDef("module.tp_to_bed_with_compass",
                    TpToBedModule::new
            ),
            new ModuleDef("module.villager_follow_emeralds",
                    VillagerTauntModule::new
            ),
            new ModuleDef("module.wall_jump",
                    WallJumpModule::new
            )
    );

    private final JavaPlugin plugin;
    private final Map<String, Listener> modules = new HashMap<>();
    private final List<Listener> systemListeners = new ArrayList<>();

    public ModuleManager(JavaPlugin plugin) {
        this.plugin  = plugin;
        registerModules();
        registerSystemListeners();
    }

    private void registerModules() {
        for (Listener module : modules.values()) {
            if (module instanceof CancellableModule) {
                ((CancellableModule) module).cancelTasks();
            }
        }

        for (Listener module : modules.values()) {
            HandlerList.unregisterAll(module);
        }

        modules.clear();

        for (ModuleDef def : DEFS) {
            Listener module = def.create(plugin);
            addModule(def.configPath(), module);
        }
    }

    private void registerSystemListeners() {
        for (Listener listener : systemListeners) {
            HandlerList.unregisterAll(listener);
        }
        systemListeners.clear();
        addUpdateListeners();
    }

    public void addUpdateListeners() {
        UpdateOnFullLoad updateListener = new UpdateOnFullLoad();
        UpdateOnJoinListener joinListener = new UpdateOnJoinListener();

        addSystemListener(updateListener);
        addSystemListener(joinListener);
    }

    public void addSystemListener(Listener listener) {
        systemListeners.add(listener);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    private void addModule(String configPath, Listener module) {
        String moduleName = getModuleName(configPath);
        if (plugin.getConfig().getBoolean(configPath, true)) {
            modules.put(configPath, module);
            Bukkit.getPluginManager().registerEvents(module, plugin);
            TextHandler.get().logTranslated("module.enabled", moduleName);
        } else {
            TextHandler.get().logTranslated("module.disabled", moduleName);
        }
    }

    private static String getModuleName(String configPath) {
        return configPath.replace("module.", "");
    }

    public void reloadModules() {
        plugin.reloadConfig();
        registerModules();
        registerSystemListeners();
    }

    public Listener getModule(String moduleName) {
        String key = moduleName.startsWith("module.") ? moduleName : "module." + moduleName;
        return modules.get(key);
    }
}