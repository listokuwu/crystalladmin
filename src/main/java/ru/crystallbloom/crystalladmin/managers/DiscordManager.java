package ru.crystallbloom.crystalladmin.managers;

import org.bukkit.Bukkit;
import ru.crystallbloom.crystalladmin.CrystallAdmin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DiscordManager {

    private final CrystallAdmin plugin;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public DiscordManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", false);
    }

    private String getWebhookUrl() {
        return plugin.getConfig().getString("discord.webhook-url", "");
    }

    public void log(String message) {
        if (!isEnabled()) return;
        String webhookUrl = getWebhookUrl();
        if (webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) return;

        String timestamp = LocalDateTime.now().format(formatter);
        String fullMessage = "`[" + timestamp + "]` " + message;
        sendAsync(webhookUrl, fullMessage);
    }

    public void logAdmCommand(String admin, String command) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("discord.log-adm-commands", true)) return;
        log("🔧 **ADM** | **" + admin + "** использовал: `" + command + "`");
    }

    public void logPunishment(String type, String player, String reason, String admin, String expires) {
        if (!isEnabled()) return;
        String emoji = switch (type.split(" ")[0]) {
            case "БАН" -> "🔨";
            case "МУТ" -> "🔇";
            case "ВАРН" -> "⚠️";
            default -> "📋";
        };
        log(emoji + " **" + type + "** | Игрок: **" + player + "** | Причина: `" + reason +
                "` | Адмін: **" + admin + "** | До: `" + expires + "`");
    }

    public void logReport(String reporter, String message) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("discord.log-reports", true)) return;
        log("📩 **РЕПОРТ** от **" + reporter + "**: `" + message + "`");
    }

    public void logBroadcast(String admin, String message) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("discord.log-broadcast", true)) return;
        log("📢 **ОБЪЯВЛЕНИЕ** от **" + admin + "**: `" + message + "`");
    }

    private void sendAsync(String webhookUrl, String content) {
        // Escape JSON special characters
        String escaped = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        String json = "{\"content\":\"" + escaped + "\",\"username\":\"CrystallAdmin\"}";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "CrystallAdmin/1.0");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != 204 && responseCode != 200) {
                    plugin.getLogger().warning("Discord webhook returned code: " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }
}