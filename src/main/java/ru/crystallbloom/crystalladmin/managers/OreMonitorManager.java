package ru.crystallbloom.crystalladmin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OreMonitorManager {
    private final CrystallAdmin plugin;
    private final Map<UUID, Map<String, Integer>> lastNotified = new ConcurrentHashMap<>();
    private final Map<String, Double> serverAverages = new ConcurrentHashMap<>();

    public OreMonitorManager(CrystallAdmin plugin) { this.plugin = plugin; }

    public boolean isTrackedOre(Material mat) {
        return plugin.getConfig().getBoolean("ore-monitor.enabled", true) &&
                plugin.getConfig().getStringList("ore-monitor.tracked-ores").contains(mat.name());
    }

    public boolean isBackgroundBlock(Material mat) {
        return plugin.getConfig().getStringList("ore-monitor.background-blocks").contains(mat.name());
    }

    public void onOreMined(Player player, Material ore) {
        plugin.getDatabaseManager().incrementOre(player.getUniqueId().toString(), player.getName(), ore.name());

        if (!plugin.getConfig().getBoolean("ore-monitor.notify-on-mine", true)) return;

        int count = plugin.getDatabaseManager().getOreCountLastHour(player.getUniqueId().toString(), ore.name());
        int threshold = plugin.getConfig().getInt("ore-monitor.notify-threshold", 5);

        Map<String, Integer> playerCache = lastNotified.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        int last = playerCache.getOrDefault(ore.name(), 0);

        if (count >= threshold && (last == 0 || count - last >= 5 || count == threshold)) {
            playerCache.put(ore.name(), count);
            String oreName = getOreName(ore.name());
            String msg = plugin.getLocaleManager().msg("xray.notification", "player", player.getName(), "ore", oreName, "count", count);

            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("crystalladmin.admin")) {
                    admin.sendMessage(ColorUtil.color(msg));
                }
            }
        }
    }

    public void onBackgroundMined(Player player) {
        plugin.getDatabaseManager().incrementBackground(player.getUniqueId().toString(), player.getName());
    }

    public String buildReport(String playerName) {
        var data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
        if (data == null) return null;

        String uuid = (String) data.get("uuid");
        Map<String, Integer> stats = plugin.getDatabaseManager().getAllOreStats(uuid);
        long bgCount = stats.getOrDefault("__bg__", 0);

        double suspiciousRatio = plugin.getConfig().getDouble("anticheat.modules.xray.notify-threshold", 3.0);
        StringBuilder sb = new StringBuilder();

        sb.append(plugin.getLocaleManager().msg("xray.report-header")).append("\n");
        sb.append(plugin.getLocaleManager().msg("xray.report-title", "player", playerName)).append("\n");
        sb.append(plugin.getLocaleManager().msg("xray.report-header")).append("\n");
        sb.append(plugin.getLocaleManager().msg("xray.report-background", "count", bgCount)).append("\n\n");

        long totalDiamonds = 0;
        for (Map.Entry<String, Integer> e : stats.entrySet()) {
            if (e.getKey().equals("__bg__")) continue;
            String ore = getOreName(e.getKey());
            int count = e.getValue();
            double ratio = bgCount > 0 ? (count * 100.0 / bgCount) : 0;
            boolean sus = ratio > suspiciousRatio && count >= 5;

            sb.append("  &8» ").append(ore).append(": &f").append(count)
                    .append(" &8| ").append(sus ? "&c" : "&a").append(String.format("%.2f%%", ratio))
                    .append(sus ? plugin.getLocaleManager().msg("xray.report-suspicious") : "").append("\n");

            if (e.getKey().contains("DIAMOND")) totalDiamonds += count;
        }

        sb.append("\n").append(plugin.getLocaleManager().msg("xray.report-overall", "count", totalDiamonds));
        if (bgCount > 0 && totalDiamonds > 0) {
            double ratio = totalDiamonds * 100.0 / bgCount;
            boolean sus = ratio > suspiciousRatio;
            sb.append(" &8| ").append(sus ? "&c" : "&a").append(String.format("%.2f%%", ratio));
            if (sus) sb.append(" &c&l← X-RAY?");
        }
        sb.append("\n").append(plugin.getLocaleManager().msg("xray.report-footer"));

        return sb.toString();
    }

    private String getOreName(String key) {
        Map<String, String> names = Map.of(
                "DIAMOND_ORE", "💎 алмаз",
                "DEEPSLATE_DIAMOND_ORE", "💎 алмаз (deep)",
                "ANCIENT_DEBRIS", "🔥 незерит",
                "EMERALD_ORE", "💚 изумруд"
        );
        return names.getOrDefault(key, key.toLowerCase());
    }

    public void clearCache(UUID uuid) { lastNotified.remove(uuid); }
}