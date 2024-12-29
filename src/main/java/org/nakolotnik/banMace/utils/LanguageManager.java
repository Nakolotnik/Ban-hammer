package org.nakolotnik.banMace.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.nakolotnik.banMace.BanMace;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class LanguageManager {
    private final BanMace plugin;
    private final Map<String, String> messages;
    private String currentLanguage;

    public LanguageManager(BanMace plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.currentLanguage = "en";
    }

    public void loadMessages(String language) {
        this.currentLanguage = language;
        File languageFile = new File(plugin.getDataFolder(), "message_" + language + ".yml");

        if (!languageFile.exists()) {
            plugin.getLogger().warning("Language file for " + language + " not found. Falling back to default (en).");
            languageFile = new File(plugin.getDataFolder(), "message_en.yml");
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(languageFile);
        messages.clear();

        if (config.getConfigurationSection("messages") == null) {
            plugin.getLogger().severe("Failed to load messages from " + languageFile.getName());
            return;
        }

        for (String key : config.getConfigurationSection("messages").getKeys(false)) {
            messages.put(key, config.getString("messages." + key));
        }

        plugin.getLogger().info("Loaded " + messages.size() + " messages for language: " + language);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messages.getOrDefault(key, "Message not found: " + key);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return message;
    }

    public String getMessage(String key) {
        return getMessage(key, null);
    }

    public List<String> getAvailableLanguages() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        return Arrays.stream(Objects.requireNonNull(dataFolder.listFiles()))
                .filter(file -> file.getName().startsWith("message_") && file.getName().endsWith(".yml"))
                .map(file -> file.getName().substring(8, file.getName().length() - 4))
                .collect(Collectors.toList());
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public String getAvailableLanguagesString() {
        return String.join(", ", getAvailableLanguages());
    }


    public void checkAndLoadLanguageFiles() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        List<String> defaultLanguages = List.of("en", "ru");
        for (String lang : defaultLanguages) {
            File languageFile = new File(dataFolder, "message_" + lang + ".yml");
            if (!languageFile.exists()) {
                plugin.getLogger().info("Language file for '" + lang + "' is missing. Extracting default file...");
                plugin.saveResource("message_" + lang + ".yml", false);
                plugin.getLogger().info("Extracted 'message_" + lang + ".yml'.");
            }
        }
    }
}
