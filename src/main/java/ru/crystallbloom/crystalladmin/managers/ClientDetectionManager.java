package ru.crystallbloom.crystalladmin.managers;

import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDetectionManager {
    private final CrystallAdmin plugin;
    private final Map<UUID, Integer> reachViolations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBlockPlace = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> placeCount = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> clickTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> violationLevels = new ConcurrentHashMap<>();

    public ClientDetectionManager(CrystallAdmin plugin) { this.plugin = plugin; }

    public String parseBrand(byte[] data) {
        if (data == null || data.length == 0) return "Unknown";
        try {
            int offset = 0;
            while (offset < data.length && (data[offset] & 0x80) != 0) offset++;
            offset++;
            if (offset >= data.length) return "Unknown";
            String raw = new String(data, offset, data.length - offset, java.nio.charset.StandardCharsets.UTF_8).trim().toLowerCase();
            if (raw.isEmpty() || raw.equals("vanilla")) return "Vanilla";
            if (raw.contains("fabric")) return "Fabric";
            if (raw.contains("forge")) return "Forge";
            if (raw.contains("wurst")) return "⚠ Wurst";
            if (raw.contains("meteor")) return "⚠ Meteor";
            return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        } catch (Exception e) { return "Unknown"; }
    }

    public void onReach(Player player, double distance) {
        if (!plugin.getConfig().getBoolean("anticheat.modules.reach.enabled", true)) return;
        if (player.hasPermission("crystalladmin.bypass.anticheat")) return;

        double maxDist = plugin.getConfig().getDouble("anticheat.modules.reach.max-distance", 3.5);
        int maxPing = plugin.getConfig().getInt("anticheat.modules.reach.max-ping", 300);
        int minViolations = plugin.getConfig().getInt("anticheat.modules.reach.min-violations", 5);

        if (player.getPing() > maxPing) return;

        UUID uuid = player.getUniqueId();
        if (distance > maxDist) {
            int vl = reachViolations.merge(uuid, 1, Integer::sum);
            if (vl >= minViolations && vl % 5 == 0) {
                addVL(uuid, "reach", plugin.getConfig().getInt("anticheat.modules.reach.vl-per-violation", 5));
                alert(player, "Reach", String.format("%.1f блоков", distance));
            }
        } else {
            reachViolations.put(uuid, Math.max(0, reachViolations.getOrDefault(uuid, 0) - 1));
        }
    }

    public void onBlockPlace(Player player) {
        if (!plugin.getConfig().getBoolean("anticheat.modules.fast-place.enabled", true)) return;
        if (player.hasPermission("crystalladmin.bypass.anticheat")) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastBlockPlace.get(uuid);

        if (last != null && now - last < 1000) {
            int count = placeCount.merge(uuid, 1, Integer::sum);
            if (count > plugin.getConfig().getInt("anticheat.modules.fast-place.max-blocks-per-second", 15)) {
                addVL(uuid, "fast-place", plugin.getConfig().getInt("anticheat.modules.fast-place.vl-per-violation", 3));
                alert(player, "FastPlace", count + " блоков/сек");
            }
        } else {
            placeCount.put(uuid, 1);
        }
        lastBlockPlace.put(uuid, now);
    }

    public void onPlayerClick(Player player) {
        if (!plugin.getConfig().getBoolean("anticheat.modules.auto-clicker.enabled", true)) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        clickTimestamps.computeIfAbsent(uuid, k -> new ArrayList<>()).add(now);
        List<Long> clicks = clickTimestamps.get(uuid);
        clicks.removeIf(time -> now - time > 10000);

        double cps = clicks.size() / 10.0;
        if (cps > plugin.getConfig().getInt("anticheat.modules.auto-clicker.max-cps", 20)) {
            addVL(uuid, "auto-clicker", plugin.getConfig().getInt("anticheat.modules.auto-clicker.vl-per-violation", 5));
            alert(player, "AutoClicker", String.format("%.1f CPS", cps));
        }
    }

    private void addVL(UUID uuid, String check, int amount) {
        violationLevels.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).merge(check, amount, Integer::sum);
    }

    private void alert(Player player, String check, String details) {
        if (!plugin.getConfig().getBoolean("anticheat.show-alerts", true)) return;
        String msg = plugin.getLocaleManager().msg("anticheat.alert",
                "player", player.getName(), "check", check, "vl", getTotalVL(player.getUniqueId()), "details", details);

        for (Player admin : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("crystalladmin.anticheat.alerts")) {
                admin.sendMessage(ru.crystallbloom.crystalladmin.utils.ColorUtil.color(msg));
            }
        }
    }

    private int getTotalVL(UUID uuid) {
        return violationLevels.getOrDefault(uuid, new HashMap<>()).values().stream().mapToInt(Integer::intValue).sum();
    }

    public void cleanup(UUID uuid) {
        reachViolations.remove(uuid);
        lastBlockPlace.remove(uuid);
        placeCount.remove(uuid);
        clickTimestamps.remove(uuid);
        violationLevels.remove(uuid);
    }
}