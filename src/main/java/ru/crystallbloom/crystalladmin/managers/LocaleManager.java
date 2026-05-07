package ru.crystallbloom.crystalladmin.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.crystallbloom.crystalladmin.CrystallAdmin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocaleManager {

    private final CrystallAdmin plugin;
    private FileConfiguration messages;
    private String currentLanguage;

    public LocaleManager(CrystallAdmin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currentLanguage = plugin.getConfig().getString("general.language", "ru");

        String fileName = "messages_" + currentLanguage + ".yml";
        File langFile = new File(plugin.getDataFolder(), fileName);

        // Copy from jar if doesn't exist
        if (!langFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        messages = YamlConfiguration.loadConfiguration(langFile);

        // Load defaults from jar as fallback
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaultConfig);
        }

        plugin.getLogger().info("[Locale] Loaded language: " + currentLanguage);
    }

    /**
     * Get a message by path with placeholder replacements.
     * Example: msg("punishments.ban-success", "player", "Steve", "reason", "Cheating")
     */
    public String msg(String path, Object... replacements) {
        String message = messages.getString(path, "&cMissing translation: " + path);

        // Replace placeholders
        if (replacements != null && replacements.length >= 2) {
            Map<String, String> placeholders = new HashMap<>();
            for (int i = 0; i < replacements.length - 1; i += 2) {
                placeholders.put(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
            }
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }

    /**
     * Get a list of messages (for help pages, etc.)
     */
    public List<String> msgList(String path) {
        return messages.getStringList(path);
    }

    /**
     * Check if path exists in current language file
     */
    public boolean has(String path) {
        return messages.contains(path);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}