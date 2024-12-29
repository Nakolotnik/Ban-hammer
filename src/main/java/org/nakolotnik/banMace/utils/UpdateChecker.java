package org.nakolotnik.banMace.utils;

import org.bukkit.Bukkit;
import org.nakolotnik.banMace.BanMace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/ban-mace/version";
    private final BanMace plugin;

    public UpdateChecker(BanMace plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(MODRINTH_API_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "BanMace Plugin Update Checker");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                String latestVersion = parseLatestVersion(response.toString());
                if (latestVersion != null && isNewerVersion(latestVersion)) {
                    plugin.getLogger().warning("A new version of BanMace is available: " + latestVersion +
                            ". Download it here: https://modrinth.com/plugin/ban-mace");
                } else {
                    plugin.getLogger().info("You are using the latest version of BanMace.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private String parseLatestVersion(String jsonResponse) {
        try {
            String[] parts = jsonResponse.split("\"");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("version_number")) {
                    return parts[i + 2];
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse version response: " + e.getMessage());
        }
        return null;
    }

    private boolean isNewerVersion(String latestVersion) {
        String currentVersion = plugin.getDescription().getVersion();
        return latestVersion.compareTo(currentVersion) > 0;
    }
}
