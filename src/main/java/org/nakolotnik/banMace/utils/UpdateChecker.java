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

public class UpdateChecker implements Listener {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/ban-mace/version";
    private final BanMace plugin;
    private String latestVersion;
    private boolean updateAvailable = false;
    private String updateMessage;
    private boolean alreadyLogged = false; // Флаг, предотвращающий двойное логирование

    public UpdateChecker(BanMace plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin); // Регистрируем обработчик событий
        checkForUpdates(); // Проверка обновлений при запуске
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

                latestVersion = parseLatestVersion(response.toString());
                if (latestVersion != null && isNewerVersion(latestVersion)) {
                    updateAvailable = true;
                    updateMessage = "§6[BanMace] A new version is available: " + latestVersion +
                            ". Download it here: https://modrinth.com/plugin/ban-mace";

                    // Логируем в консоль только один раз
                    if (!alreadyLogged) {
                        plugin.getLogger().warning(updateMessage);
                        alreadyLogged = true; // Устанавливаем флаг, чтобы больше не логировать
                    }
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

    /**
     * Уведомляет админов при входе в игру, если есть обновление.
     */
    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        if (!updateAvailable || !plugin.getConfig().getBoolean("update_notify_in_game", true)) {
            return; // Если обновления нет или отключено уведомление, выходим
        }

        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("banmace.update.notify")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) { // Проверяем, что игрок всё ещё в игре
                    player.sendMessage(updateMessage);
                }
            }, 40L); // Задержка 2 секунды (40 тиков), чтобы не терялось среди других сообщений
        }
    }
}
