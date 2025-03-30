package com.erosmari.vitamin;

import com.erosmari.vitamin.adapter.VersionAdapter;
import com.erosmari.vitamin.adapter.VersionAdapter_1_21_1;
import com.erosmari.vitamin.adapter.VersionAdapter_1_21_4;
import com.erosmari.vitamin.commands.VitaminCommandManager;
import com.erosmari.vitamin.config.ConfigHandler;
import com.erosmari.vitamin.database.DatabaseHandler;
import com.erosmari.vitamin.modules.ModuleManager;
import com.erosmari.vitamin.utils.AsyncExecutor;
import com.erosmari.vitamin.utils.ConsoleUtils;
import com.erosmari.vitamin.utils.LoggingUtils;
import com.erosmari.vitamin.utils.TranslationHandler;
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
            LoggingUtils.logTranslated("plugin.enable_error", e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        AsyncExecutor.shutdown();
        LoggingUtils.logTranslated("plugin.disabled");
        instance = null;
        DatabaseHandler.close();
    }

    private void initializePlugin() {
        try {
            ConsoleUtils.displayAsciiArt(this);
            loadConfigurations();
            DatabaseHandler.initialize(this);
            LoggingUtils.logTranslated("plugin.separator");
            moduleManager = new ModuleManager(this);
            LoggingUtils.logTranslated("plugin.separator");
            initializeMetrics();
        } catch (Exception e) {
            LoggingUtils.logTranslated("plugin.enable_error", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void loadConfigurations() {
        ConfigHandler.setup(this);
        getConfig().options().copyDefaults(true);
        saveConfig();

        setupTranslations();
        TranslationHandler.loadTranslations(this, ConfigHandler.getLanguage());
        ConsoleUtils.displaySuccessMessage(this);

        AsyncExecutor.initialize();

        initializeCommandManager();
    }

    private void setupTranslations() {
        File translationsFolder = new File(getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            LoggingUtils.logTranslated("translations.folder_error");
            return;
        }

        String[] defaultLanguages = {"en_us.yml"};
        for (String languageFile : defaultLanguages) {
            saveDefaultTranslation(languageFile);
        }

        File[] translationFiles = translationsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (translationFiles != null) {
            for (File file : translationFiles) {
                String language = file.getName().replace(".yml", "");
                TranslationHandler.loadTranslations(this, language);
            }
        }

        String configuredLanguage = ConfigHandler.getLanguage();
        if (TranslationHandler.isLanguageAvailable(configuredLanguage)) {
            TranslationHandler.setActiveLanguage(configuredLanguage);
        } else {
            final String LANG_NOT_FOUND_KEY = "translations.language_not_found";
            TranslationHandler.registerTemporaryTranslation(LANG_NOT_FOUND_KEY, "Language not found: {0}");
            LoggingUtils.logTranslated(LANG_NOT_FOUND_KEY, configuredLanguage);
        }
    }

    private void saveDefaultTranslation(String fileName) {
        File translationFile = new File(getDataFolder(), "Translations/" + fileName);
        if (!translationFile.exists()) {
            try {
                saveResource("Translations/" + fileName, false);
            } catch (Exception e) {
                final String LANG_NOT_SAVED_KEY = "translations.save_error";
                TranslationHandler.registerTemporaryTranslation(LANG_NOT_SAVED_KEY, "Language cannot be saved: {0}");
                LoggingUtils.logTranslated(LANG_NOT_SAVED_KEY, fileName);
            }
        }
    }

    private void initializeCommandManager() {
        try {
            if (commandManager == null) {
                commandManager = new VitaminCommandManager(this);
                commandManager.registerCommands();
            }
        } catch (Exception e) {
            LoggingUtils.logTranslated("command.register_error", e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeMetrics() {
        try {
            new Metrics(this, BSTATS_PLUGIN_ID);
        } catch (Exception e) {
            final String BSTATS_ERROR = "bstats.error";
            TranslationHandler.registerTemporaryTranslation(BSTATS_ERROR, "BStats error: {0}");
            LoggingUtils.logTranslated(BSTATS_ERROR, e.getMessage());
        }
    }

    private void setupVersionAdapter() {
        String version = Bukkit.getVersion();
        if (version.contains("1.21.3") || version.contains("1.21.4")) {
            versionAdapter = new VersionAdapter_1_21_4();
        } else if (version.contains("1.21.1") || version.contains("1.21")) {
            versionAdapter = new VersionAdapter_1_21_1();
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