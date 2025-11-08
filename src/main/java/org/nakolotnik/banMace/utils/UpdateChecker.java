package org.nakolotnik.banMace.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nakolotnik.banMace.BanMace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker implements Listener {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/ban-mace/version";
    private final BanMace plugin;
    private String latestVersion;
    private boolean updateAvailable = false;
    private String updateMessage;
    private boolean alreadyLogged = false;

    public UpdateChecker(BanMace plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        checkForUpdates();
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(MODRINTH_API_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "BanMace Plugin Update Checker/" + plugin.getDescription().getVersion());

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();

                latestVersion = parseLatestVersion(response.toString());
                String currentVersion = plugin.getDescription().getVersion();

                if (latestVersion != null && isNewerVersion(latestVersion, currentVersion)) {
                    updateAvailable = true;
                    updateMessage = "§6[BanMace] A new version is available: §e" + latestVersion +
                            "§6. You are on §c" + currentVersion + "§6. Download it here: https://modrinth.com/plugin/ban-mace";

                    if (!alreadyLogged) {
                        plugin.getLogger().warning(updateMessage);
                        alreadyLogged = true;
                    }
                } else {
                    plugin.getLogger().info("You are using the latest version of BanMace (" + currentVersion + ").");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to check for updates: " + e.getMessage());
            }
        });
    }


    private String parseLatestVersion(String jsonResponse) {
        try {
            Pattern pattern = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse version response: " + e.getMessage());
        }
        return null;
    }

    private boolean isNewerVersion(String latestVersionStr, String currentVersionStr) {
        latestVersionStr = latestVersionStr.replaceAll("[^0-9.]", "");
        currentVersionStr = currentVersionStr.replaceAll("[^0-9.]", "");

        String[] latestParts = latestVersionStr.split("\\.");
        String[] currentParts = currentVersionStr.split("\\.");

        int maxLength = Math.max(latestParts.length, currentParts.length);

        for (int i = 0; i < maxLength; i++) {
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            }
            if (latestPart < currentPart) {
                return false;
            }
        }

        return false;
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        if (!updateAvailable || !plugin.getConfig().getBoolean("update_notify_in_game", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("banmace.update.notify")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(updateMessage);
                }
            }, 40L);
        }
    }
}