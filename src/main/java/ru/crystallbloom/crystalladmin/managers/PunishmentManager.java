package ru.crystallbloom.crystalladmin.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.database.DatabaseManager;
import ru.crystallbloom.crystalladmin.models.BanModel;
import ru.crystallbloom.crystalladmin.models.MuteModel;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;
import ru.crystallbloom.crystalladmin.utils.TimeUtil;

public class PunishmentManager {

    private final CrystallAdmin plugin;
    private final DatabaseManager db;

    public PunishmentManager(CrystallAdmin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    // ────────────────────────────── BAN ──────────────────────────────

    public void ban(Player target, String reason, String adminName, long expires) {
        db.addBan(target.getUniqueId().toString(), target.getName(), reason, adminName, expires);
        plugin.getDiscordManager().logPunishment("БАН", target.getName(), reason, adminName, TimeUtil.formatExpiry(expires));
        target.kick(buildBanScreen(reason, adminName, expires));
    }

    public void banOffline(String name, String uuid, String reason, String adminName, long expires) {
        db.addBan(uuid, name, reason, adminName, expires);
        plugin.getDiscordManager().logPunishment("БАН (офлайн)", name, reason, adminName, TimeUtil.formatExpiry(expires));
    }

    public boolean unban(String playerName) {
        var data = db.getPlayerDataByName(playerName);
        if (data == null) return false;
        String uuid = (String) data.get("uuid");
        if (db.getActiveBan(uuid) == null) return false;
        db.deactivateBans(uuid);
        plugin.getDiscordManager().log("✅ РАЗБАН: **" + playerName + "**");
        return true;
    }

    public Component buildBanScreen(String reason, String adminName, long expires) {
        // Always re-read from config so /adm reload works
        String serverName = plugin.getConfig().getString("general.server-name", "CrystallBloom");
        String contact    = plugin.getConfig().getString("general.contact", "@mcsreal");
        String platform   = plugin.getConfig().getString("general.contact-platform", "ТГ");

        String defaultPerm = "&c&l{server} &8\u00bb &f\u041f\u0435\u0440\u043c\u0430\u043d\u0435\u043d\u0442\u043d\u0430\u044f \u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u043a\u0430\n&8&m              \n&7\u041f\u0440\u0438\u0447\u0438\u043d\u0430 &8\u00bb &f{reason}\n&8&m              \n&c\u0410\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440: &6{admin}\n&f\u041e\u0431\u0436\u0430\u043b\u043e\u0432\u0430\u043d\u0438\u0435: &6{platform} {contact}";
        String defaultTemp = "&c&l{server} &8\u00bb &f\u0412\u0440\u0435\u043c\u0435\u043d\u043d\u0430\u044f \u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u043a\u0430\n&8&m              \n&7\u041f\u0440\u0438\u0447\u0438\u043d\u0430 &8\u00bb &f{reason}\n&7\u0418\u0441\u0442\u0435\u043a\u0430\u0435\u0442 &8\u00bb &f{duration}\n&8&m              \n&c\u0410\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440: &6{admin}\n&f\u041e\u0431\u0436\u0430\u043b\u043e\u0432\u0430\u043d\u0438\u0435: &6{platform} {contact}";

        String template = (expires == -1)
                ? plugin.getConfig().getString("screens.ban-permanent", defaultPerm)
                : plugin.getConfig().getString("screens.ban-temp", defaultTemp);

        String durationStr = expires == -1 ? "\u041d\u0430\u0432\u0441\u0435\u0433\u0434\u0430" : TimeUtil.formatExpiry(expires);

        // Support both {placeholder} and %placeholder% styles
        String result = template
                .replace("{reason}",   reason)      .replace("%reason%",   reason)
                .replace("{admin}",    adminName)   .replace("%admin%",    adminName)
                .replace("{duration}", durationStr) .replace("%duration%", durationStr)
                .replace("{server}",   serverName)  .replace("%server%",   serverName)
                .replace("{contact}",  contact)     .replace("%contact%",  contact)
                .replace("{platform}", platform)    .replace("%platform%", platform);

        return ColorUtil.color(result);
    }

    public Component buildKickScreen(String reason, String adminName) {
        String serverName = plugin.getConfig().getString("general.server-name", "CrystallBloom");
        String defaultKick = "&c&l{server} &8\u00bb &f\u0412\u044b \u0431\u044b\u043b\u0438 \u043a\u0438\u043a\u043d\u0443\u0442\u044b\n&8&m              \n&7\u041f\u0440\u0438\u0447\u0438\u043d\u0430 &8\u00bb &f{reason}\n&8&m              \n&f\u0410\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440: &c{admin}";
        String template = plugin.getConfig().getString("screens.kick", defaultKick);
        return ColorUtil.color(template
                .replace("{reason}", reason)    .replace("%reason%", reason)
                .replace("{admin}",  adminName) .replace("%admin%",  adminName)
                .replace("{server}", serverName).replace("%server%", serverName));
    }

    public BanModel getActiveBan(String uuid) { return db.getActiveBan(uuid); }

    // ────────────────────────────── KICK ──────────────────────────────

    public void kick(Player target, String reason, String adminName) {
        plugin.getDiscordManager().logPunishment("КИК", target.getName(), reason, adminName, "—");
        target.kick(buildKickScreen(reason, adminName));
    }

    // ────────────────────────────── MUTE ──────────────────────────────

    public void mute(Player target, String reason, String adminName, long expires) {
        db.addMute(target.getUniqueId().toString(), target.getName(), reason, adminName, expires);
        plugin.getDiscordManager().logPunishment("МУТ", target.getName(), reason, adminName, TimeUtil.formatExpiry(expires));
        target.sendMessage(ColorUtil.color(
                "\n&8[&cМУТ&8] &fВы заглушены администратором &c" + adminName +
                        "\n&8Причина: &f" + reason +
                        "\n&8До: &f" + TimeUtil.formatExpiry(expires) + "\n"
        ));
    }

    public boolean unmuteByName(String name) {
        var data = db.getPlayerDataByName(name);
        if (data == null) return false;
        String uuid = (String) data.get("uuid");
        if (db.getActiveMute(uuid) == null) return false;
        db.deactivateMutes(uuid);
        plugin.getDiscordManager().log("🔈 РАЗМУТ: **" + name + "**");
        Player online = Bukkit.getPlayer(name);
        if (online != null) online.sendMessage(ColorUtil.color("&aВаш мут был снят администрацией."));
        return true;
    }

    public boolean isMuted(Player player) {
        MuteModel mute = db.getActiveMute(player.getUniqueId().toString());
        if (mute == null) return false;
        if (mute.isExpired()) { db.deactivateMutes(player.getUniqueId().toString()); return false; }
        return true;
    }

    public String getMuteMessage(Player player) {
        MuteModel mute = db.getActiveMute(player.getUniqueId().toString());
        if (mute == null) return null;
        return "&8[&cМУТ&8] &fВы заглушены. Причина: &c" + mute.getReason()
                + " &8| &fОсталось: &c" + TimeUtil.formatRemaining(mute.getExpires());
    }

    // ────────────────────────────── WARN ──────────────────────────────

    public void warn(Player target, String reason, String adminName, long expires) {
        db.addWarn(target.getUniqueId().toString(), target.getName(), reason, adminName, expires);
        plugin.getDiscordManager().logPunishment("ВАРН", target.getName(), reason, adminName, TimeUtil.formatExpiry(expires));
        int maxW  = plugin.getConfig().getInt("warns.max-display", 3);
        int curW  = db.getActiveWarns(target.getUniqueId().toString()).size();
        target.sendMessage(ColorUtil.color(
                "\n&8&m                    &r\n" +
                        "&c  ⚠ ПРЕДУПРЕЖДЕНИЕ\n\n" +
                        "&fВы получили предупреждение от &c" + adminName + "\n" +
                        "&8Причина: &f" + reason + "\n" +
                        "&8Истекает: &f" + TimeUtil.formatExpiry(expires) + "\n" +
                        "&8Предупреждений: &c" + curW + "&8/&c" + maxW + "\n" +
                        "&8&m                    "
        ));
    }

    public void warnOffline(String name, String uuid, String reason, String adminName, long expires) {
        db.addWarn(uuid, name, reason, adminName, expires);
        plugin.getDiscordManager().logPunishment("ВАРН (офлайн)", name, reason, adminName, TimeUtil.formatExpiry(expires));
    }

    public boolean unwarn(String playerName, int warnIndex) {
        var data = db.getPlayerDataByName(playerName);
        if (data == null) return false;
        var warns = db.getActiveWarns((String) data.get("uuid"));
        if (warnIndex < 1 || warnIndex > warns.size()) return false;
        db.removeWarnById(warns.get(warnIndex - 1).getId());
        return true;
    }
}