package org.nakolotnik.banMace.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final String url;
    private final JavaPlugin plugin;

    public DiscordWebhook(String url, JavaPlugin plugin) {
        this.url = url;
        this.plugin = plugin;
    }

    public void sendEmbed(EmbedObject embed) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL hookUrl = new URL(this.url);
                HttpsURLConnection connection = (HttpsURLConnection) hookUrl.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("User-Agent", "BanMace-Plugin/1.0");
                connection.setDoOutput(true);

                JsonObject jsonPayload = new JsonObject();

                JsonArray embedsArray = new JsonArray();
                embedsArray.add(embed.toJsonObject());
                jsonPayload.add("embeds", embedsArray);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Failed to send Discord webhook. Response code: " + responseCode);
                     try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            plugin.getLogger().warning("Discord Response: " + responseLine);
                        }
                     }
                }
                connection.disconnect();

            } catch (Exception e) {
                plugin.getLogger().warning("Could not send Discord webhook message:");
                e.printStackTrace();
            }
        });
    }

    public static class EmbedObject {
        private String title;
        private String description;
        private Integer color;
        private String authorName;
        private String authorIconUrl;
        private String footer;

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public EmbedObject setColor(int color) {
            this.color = color;
            return this;
        }

        public EmbedObject setAuthor(String name, String iconUrl) {
            this.authorName = name;
            this.authorIconUrl = iconUrl;
            return this;
        }

        public EmbedObject setFooter(String footer) {
            this.footer = footer;
            return this;
        }

        public JsonObject toJsonObject() {
            JsonObject embed = new JsonObject();
            if (title != null) embed.addProperty("title", title);
            if (description != null) embed.addProperty("description", description);
            if (color != null) embed.addProperty("color", color);
            embed.addProperty("timestamp", java.time.Instant.now().toString());

            if (authorName != null) {
                JsonObject author = new JsonObject();
                author.addProperty("name", authorName);
                if (authorIconUrl != null) author.addProperty("icon_url", authorIconUrl);
                embed.add("author", author);
            }

            if (footer != null) {
                JsonObject footerObj = new JsonObject();
                footerObj.addProperty("text", footer);
                embed.add("footer", footerObj);
            }

            return embed;
        }
    }
}