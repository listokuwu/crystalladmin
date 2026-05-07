package ru.crystallbloom.crystalladmin.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;
import ru.crystallbloom.crystalladmin.utils.TimeUtil;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SSManager {

    private final CrystallAdmin plugin;
    private final Map<UUID, Integer> ssTimers = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> ssTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> ssAdmins = new ConcurrentHashMap<>();

    public SSManager(CrystallAdmin plugin) { this.plugin = plugin; }

    public boolean isUnderSS(UUID uuid) { return ssTimers.containsKey(uuid); }

    public void startSS(Player target, Player admin) {
        if (!plugin.getConfig().getBoolean("moderation.screenshare.enabled", true)) {
            admin.sendMessage(ColorUtil.color("&cScreenshare is disabled in config."));
            return;
        }
        UUID uuid = target.getUniqueId();
        String contact = plugin.getConfig().getString("general.contact", "@mcsreal");
        int timerSeconds = plugin.getConfig().getInt("moderation.screenshare.timer-seconds", 180);
        int reminderInterval = plugin.getConfig().getInt("moderation.screenshare.reminder-interval", 30);

        plugin.getFreezeManager().freezeSilent(uuid);
        ssTimers.put(uuid, timerSeconds);
        ssAdmins.put(uuid, admin.getName());
        showSSTitle(target, timerSeconds, contact);

        admin.sendMessage(ColorUtil.color(
                plugin.getLocaleManager().msg("screenshare.started",
                        "player", target.getName(), "time", TimeUtil.formatDuration(timerSeconds * 1000L))));

        plugin.getAdminChatManager().broadcastRaw(ColorUtil.color(
                plugin.getLocaleManager().msg("screenshare.started",
                        "player", target.getName(), "time", TimeUtil.formatDuration(timerSeconds * 1000L))));

        plugin.getDiscordManager().log("🔍 **SS** | Player: **" + target.getName()
                + "** | Admin: **" + admin.getName() + "** | Timer: " + timerSeconds + "s");

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player t = Bukkit.getPlayer(uuid);
            if (t == null || !t.isOnline()) { cancelTask(uuid); return; }

            int remaining = ssTimers.merge(uuid, -1, Integer::sum);
            showSSTitle(t, remaining, contact);

            if (remaining > 0 && remaining % reminderInterval == 0) {
                t.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg(
                        "screenshare.reminder", "time", TimeUtil.formatDuration(remaining * 1000L))));
            }

            if (remaining <= 0) {
                cancelTask(uuid);
                String banReason = plugin.getConfig().getString(
                        "moderation.screenshare.auto-ban-reason", "SS check refused");
                plugin.getPunishmentManager().ban(t, banReason, "SYSTEM", -1L);
                plugin.getAdminChatManager().broadcastRaw(ColorUtil.color(
                        plugin.getLocaleManager().msg("screenshare.timer-expired", "player", t.getName())));
            }
        }, 20L, 20L);

        ssTasks.put(uuid, task);
    }

    public void clearSS(Player target, Player admin) {
        UUID uuid = target.getUniqueId();
        if (!isUnderSS(uuid)) {
            admin.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("screenshare.not-under-check")));
            return;
        }
        cancelTask(uuid);
        plugin.getFreezeManager().unfreezeSilent(uuid);
        target.clearTitle();
        target.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("screenshare.player-cleared")));
        admin.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("screenshare.cleared", "player", target.getName())));
        plugin.getDiscordManager().log("✅ **SS PASSED** | Player: **" + target.getName() + "** | Admin: **" + admin.getName() + "**");
    }

    public void handleQuit(UUID uuid, String playerName) {
        if (!isUnderSS(uuid)) return;
        cancelTask(uuid);
        String banReason = plugin.getConfig().getString("moderation.screenshare.auto-ban-reason", "SS check — disconnect");
        plugin.getDatabaseManager().addBan(uuid.toString(), playerName, banReason, "SYSTEM", -1L);
        plugin.getDiscordManager().logPunishment("AUTO-BAN (SS)", playerName, banReason, "SYSTEM", "Permanent");
        plugin.getAdminChatManager().broadcastRaw(ColorUtil.color(
                plugin.getLocaleManager().msg("screenshare.auto-banned", "player", playerName)));
    }

    private void showSSTitle(Player player, int secondsRemaining, String contact) {
        String titleCfg    = plugin.getConfig().getString("moderation.screenshare.title", "&c&lSCREENSHARE");
        String subtitleCfg = plugin.getConfig().getString("moderation.screenshare.subtitle", "&fContact: &6{contact}")
                .replace("{contact}", contact).replace("%contact%", contact);
        Component title    = ColorUtil.color(titleCfg);
        Component subtitle = ColorUtil.color(subtitleCfg + " &8(&c" + secondsRemaining + "s&8)");
        player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(500))));
    }

    private void cancelTask(UUID uuid) {
        ssTimers.remove(uuid); ssAdmins.remove(uuid);
        BukkitTask task = ssTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void cancelAll() {
        for (UUID uuid : new java.util.HashSet<>(ssTimers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                String reason = plugin.getConfig().getString(
                        "moderation.screenshare.auto-ban-reason", "SS check — server shutdown");
                plugin.getDatabaseManager().addBan(uuid.toString(), p.getName(), reason, "SYSTEM", -1L);
            }
            cancelTask(uuid);
        }
    }
}