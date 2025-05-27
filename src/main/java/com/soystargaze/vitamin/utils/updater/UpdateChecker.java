package com.soystargaze.vitamin.utils.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.soystargaze.vitamin.Vitamin;
import com.soystargaze.vitamin.utils.AsyncExecutor;
import com.soystargaze.vitamin.utils.text.TextHandler;
import org.bukkit.entity.Player;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {

    private static final String API_URL = "https://api.modrinth.com/v2/project/%s/version";
    private static final String PROJECT_ID = "vitamin";
    @SuppressWarnings("deprecation")
    private static final String CURRENT_VERSION = Vitamin.getInstance().getDescription().getVersion();
    private static final String DOWNLOAD_URL = "https://modrinth.com/plugin/vitamin";

    public static void checkForUpdates(Player player) {
        AsyncExecutor.getExecutor().execute(() -> {
            try {
                String url = String.format(API_URL, PROJECT_ID);
                URI uri = new URI(url);
                URL urlObj = uri.toURL();
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "soystargaze/Vitamin/" + CURRENT_VERSION + " (dev@soystargaze.com)");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Gson gson = new Gson();
                    JsonArray versions = gson.fromJson(response.toString(), JsonArray.class);
                    if (!versions.isEmpty()) {
                        JsonObject latestVersion = versions.get(0).getAsJsonObject();
                        String latestVersionNumber = latestVersion.get("version_number").getAsString();

                        if (!CURRENT_VERSION.equals(latestVersionNumber)) {
                            if (player != null && player.isOnline()) {
                                TextHandler.get().sendAndLog(player, "plugin.update_available", latestVersionNumber, DOWNLOAD_URL);
                            } else {
                                TextHandler.get().logTranslated("plugin.update_available", latestVersionNumber, DOWNLOAD_URL);
                            }
                        } else {
                            if (player != null && player.isOnline()) {
                                TextHandler.get().sendAndLog(player, "plugin.no_update_available");
                            } else {
                                TextHandler.get().logTranslated("plugin.no_update_available");
                            }
                        }
                    }
                } else {
                    if (player != null && player.isOnline()) {
                        TextHandler.get().sendAndLog(player, "plugin.update_check_failed", String.valueOf(responseCode));
                    } else {
                        TextHandler.get().logTranslated("plugin.update_check_failed", String.valueOf(responseCode));
                    }
                }
            } catch (Exception e) {
                if (player != null && player.isOnline()) {
                    TextHandler.get().sendAndLog(player, "plugin.update_check_error", e.getMessage());
                } else {
                    TextHandler.get().logTranslated("plugin.update_check_error", e.getMessage());
                }
            }
        });
    }
}