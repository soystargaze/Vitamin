package com.soystargaze.vitamin.modules;

import com.soystargaze.vitamin.modules.core.*;
import com.soystargaze.vitamin.modules.paper.*;
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
            // —— Spigot-only (without Plugin) ——
            new ModuleDef("module.auto_tool",
                    p -> new AutoToolModule(),
                    null
            ),
            new ModuleDef("module.crop_protection",
                    p -> new CropProtectionModule(),
                    null
            ),
            new ModuleDef("module.custom_recipes",
                    p -> new CustomRecipesModule(),
                    null
            ),
            new ModuleDef("module.armor_trim",
                    p -> new ArmorTrimModule(),
                    null
            ),
            new ModuleDef("module.invisible_item_frames",
                    p -> new InvisibleItemFramesModule(),
                    null
            ),
            new ModuleDef("module.pet_protection",
                    p -> new PetProtectionModule(),
                    null
            ),
            new ModuleDef("module.seed_replanter",
                    p -> new ReplanterModule(),
                    null
            ),
            new ModuleDef("module.totem_from_inventory",
                    p -> new TotemFromInventoryModule(),
                    null
            ),

            // —— Spigot-only (with Plugin) ——
            new ModuleDef("module.bone_meal_expansion",
                    BoneMealExpansionModule::new,
                    null
            ),
            new ModuleDef("module.carry_on",
                    CarryOnModule::new,
                    null
            ),
            new ModuleDef("module.death_chest",
                    DeathChestModule::new,
                    null
            ),
            new ModuleDef("module.elytra_armor",
                    ElytraArmorModule::new,
                    null
            ),
            new ModuleDef("module.enchants_back",
                    EnchantsBackModule::new,
                    null
            ),
            new ModuleDef("module.fire_aspect_tools",
                    FireAspectOnToolsModule::new,
                    null
            ),
            new ModuleDef("module.leaf_decay",
                    LeafDecayModule::new,
                    null
            ),
            new ModuleDef("module.oxygen_bottle",
                    OxygenBottleModule::new,
                    null
            ),
            new ModuleDef("module.repair",
                    RepairModule::new,
                    null
            ),
            new ModuleDef("module.replayable_vault",
                    ReplayableVaultModule::new,
                    null),
            new ModuleDef("module.silk_spawners",
                    SilkSpawnersModule::new,
                    null
            ),
            new ModuleDef("module.sponge_with_lava",
                    SpongeWithLavaModule::new,
                    null
            ),
            new ModuleDef("module.void_totem",
                    VoidTotemModule::new,
                    null
            ),
            new ModuleDef("module.unlock_all_recipes",
                    UnlockRecipesModule::new,
                    null
            ),
            new ModuleDef("module.tree_vein_miner",
                    VeinLogModule::new,
                    null
            ),
            new ModuleDef("module.vein_miner",
                    VeinMinerModule::new,
                    null
            ),
            new ModuleDef("module.weather_effects",
                    WeatherEffectsModule::new,
                    null
            ),

            // —— Modules with Paper hooks ——
            new ModuleDef("module.death_map",
                    DeathMapModule::new,
                    PaperDeathMapModule::new
            ),
            new ModuleDef("module.elevator",
                    ElevatorModule::new,
                    PaperElevatorModule::new
            ),
            new ModuleDef("module.player_xp_to_books",
                    PlayerXptoBooksModule::new,
                    PaperPlayerXptoBooksModule::new
            ),
            new ModuleDef("module.tp_to_bed_with_compass",
                    TpToBedModule::new,
                    PaperTpToBedModule::new
            ),
            new ModuleDef("module.villager_follow_emeralds",
                    VillagerTauntModule::new,
                    PaperVillagerTauntModule::new
            ),
            new ModuleDef("module.wall_jump",
                    WallJumpModule::new,
                    PaperWallJumpModule::new
            ),
            new ModuleDef("module.waystone",
                    WaystoneModule::new,
                    PaperWaystoneModule::new
            )
    );

    private final JavaPlugin plugin;
    private final boolean isPaper;
    private final Map<String, Listener> modules = new HashMap<>();
    private final List<Listener> systemListeners = new ArrayList<>();

    public ModuleManager(JavaPlugin plugin) {
        this.plugin  = plugin;
        this.isPaper = detectPaper();
        registerModules();
        registerSystemListeners();
    }

    private boolean detectPaper() {
        try {
            Class.forName("com.destroystokyo.paper.event.player.PlayerJumpEvent");
            TextHandler.get().logTranslated("plugin.paper_detected");
            TextHandler.get().logTranslated("plugin.separator");
            return true;
        } catch (ClassNotFoundException e) {
            TextHandler.get().logTranslated("plugin.paper_not_detected");
            TextHandler.get().logTranslated("plugin.separator");
            return false;
        }
    }

    private void registerModules() {
        modules.clear();
        HandlerList.unregisterAll(plugin);

        for (ModuleDef def : DEFS) {
            Listener module = def.create(plugin, isPaper);
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