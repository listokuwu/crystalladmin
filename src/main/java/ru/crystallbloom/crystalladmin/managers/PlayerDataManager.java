package ru.crystallbloom.crystalladmin.managers;

import ru.crystallbloom.crystalladmin.CrystallAdmin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final CrystallAdmin plugin;

    // UUID -> session start time
    private final Map<UUID, Long> sessionStarts = new ConcurrentHashMap<>();
    // UUID -> last report timestamp
    private final Map<UUID, Long> reportCooldowns = new ConcurrentHashMap<>();
    // UUID -> pending new reports count (shown on admin login)
    private int pendingReportCount = 0;

    public PlayerDataManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    public void startSession(UUID uuid) {
        sessionStarts.put(uuid, System.currentTimeMillis());
    }

    public long getSessionStart(UUID uuid) {
        return sessionStarts.getOrDefault(uuid, System.currentTimeMillis());
    }

    public long endSession(UUID uuid) {
        Long start = sessionStarts.remove(uuid);
        if (start == null) return 0;
        return System.currentTimeMillis() - start;
    }

    public boolean isOnReportCooldown(UUID uuid) {
        long cooldownMs = plugin.getConfig().getLong("reports.cooldown", 60) * 1000L;
        Long last = reportCooldowns.get(uuid);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < cooldownMs;
    }

    public long getRemainingCooldown(UUID uuid) {
        long cooldownMs = plugin.getConfig().getLong("reports.cooldown", 60) * 1000L;
        Long last = reportCooldowns.get(uuid);
        if (last == null) return 0;
        long remaining = cooldownMs - (System.currentTimeMillis() - last);
        return Math.max(0, remaining / 1000);
    }

    public void setReportCooldown(UUID uuid) {
        reportCooldowns.put(uuid, System.currentTimeMillis());
    }

    public void setPendingReportCount(int count) {
        this.pendingReportCount = count;
    }

    public int getPendingReportCount() {
        return pendingReportCount;
    }
}