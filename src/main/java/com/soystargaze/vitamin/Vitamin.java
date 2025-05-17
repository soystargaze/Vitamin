package com.soystargaze.vitamin;

import com.soystargaze.vitamin.adapter.VersionAdapter;
import com.soystargaze.vitamin.adapter.*;
import com.soystargaze.vitamin.commands.VitaminCommandManager;
import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.modules.ModuleManager;
import com.soystargaze.vitamin.utils.*;
import com.soystargaze.vitamin.utils.text.*;
import com.soystargaze.vitamin.utils.updater.*;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.io.File;

public class Vitamin extends JavaPlugin implements Listener {

    private static Vitamin instance;
    private ModuleManager moduleManager;
    private VitaminCommandManager commandManager;
    private VersionAdapter versionAdapter;
    private static final int BSTATS_PLUGIN_ID = 24855;

    @Override
    public void onEnable() {
        instance = this;

        try {
            setupVersionAdapter();
            initializePlugin();
        } catch (Exception e) {
            final String KEY = "plugin.enable_error";
            TextHandler.get().registerTemporaryTranslation(KEY, "Plugin cannot be enabled: {0}");
            TextHandler.get().logTranslated(KEY, e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        AsyncExecutor.shutdown();
        TextHandler.get().logTranslated("plugin.disabled");
        instance = null;
        DatabaseHandler.close();
    }

    private void initializePlugin() {
        try {
            loadConfigurations();
            DatabaseHandler.initialize(this);
            TextHandler.get().logTranslated("plugin.separator");
            moduleManager = new ModuleManager(this);
            getServer().getPluginManager().registerEvents(new UpdateOnFullLoad(), this);
            getServer().getPluginManager().registerEvents(new UpdateOnJoinListener(), this);
            TextHandler.get().logTranslated("plugin.separator");
            initializeMetrics();
        } catch (Exception e) {
            TextHandler.get().logTranslated("plugin.enable_error", e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void loadConfigurations() {
        ConfigHandler.setup(this);
        getConfig().options().copyDefaults(true);
        saveConfig();

        TextHandler.init(this);
        setupTranslations();
        ConsoleUtils.displayAsciiArt(this);

        AsyncExecutor.initialize();
        initializeCommandManager();
        ConsoleUtils.displaySuccessMessage(this);
    }

    private void setupTranslations() {
        File translationsFolder = new File(getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            TextHandler.get().logTranslated("translations.folder_error");
            return;
        }

        String[] defaults = {
                "en_us.yml","es_es.yml","fr_fr.yml","de_de.yml",
                "pt_br.yml","pl_pl.yml","zh_cn.yml","ko_kr.yml","tr_tr.yml",
                "ja_jp.yml"
        };

        boolean replace = getConfig().getBoolean("translations.force-update", true);
        for (String file : defaults) {
            try {
                saveResource("Translations/" + file, replace);
            } catch (Exception e) {
                TextHandler.get().registerTemporaryTranslation(
                        "translations.save_error",
                        "Language cannot be saved: {0}"
                );
                TextHandler.get().logTranslated("translations.save_error", file);
            }
        }

        String lang = ConfigHandler.getLanguage();
        if (TextHandler.get().isLanguageAvailable(lang)) {
            TextHandler.get().loadTranslations(this, lang);
        } else {
            TextHandler.get().registerTemporaryTranslation(
                    "translations.language_not_found",
                    "Language not found: {0}"
            );
            TextHandler.get().logTranslated("translations.language_not_found", lang);
            TextHandler.get().loadTranslations(this, TextHandler.get().getActiveLanguage());
        }
    }

    private void initializeCommandManager() {
        try {
            if (commandManager == null) {
                commandManager = new VitaminCommandManager(this);
                commandManager.registerCommands();
            }
        } catch (Exception e) {
            TextHandler.get().logTranslated("command.register_error", e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeMetrics() {
        try {
            new Metrics(this, BSTATS_PLUGIN_ID);
        } catch (Exception e) {
            TextHandler.get().registerTemporaryTranslation(
                    "bstats.error",
                    "BStats error: {0}"
            );
            TextHandler.get().logTranslated("bstats.error", e.getMessage());
        }
    }

    private void setupVersionAdapter() {
        String raw = Bukkit.getBukkitVersion();
        String version = raw.replaceFirst(".*?(\\d+\\.\\d+\\.\\d+).*", "$1");
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);
        if (major == 1 && minor == 21 && patch >= 3) {
            versionAdapter = new VersionAdapter_1_21_4();
        } else {
            versionAdapter = new VersionAdapter_1_21_1();
        }
    }

    public VersionAdapter getVersionAdapter() {
        return versionAdapter;
    }

    public static Vitamin getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}