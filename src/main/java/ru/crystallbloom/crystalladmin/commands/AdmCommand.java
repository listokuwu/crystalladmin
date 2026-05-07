package ru.crystallbloom.crystalladmin.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.gui.ReportsGUI;
import ru.crystallbloom.crystalladmin.models.BanModel;
import ru.crystallbloom.crystalladmin.models.MuteModel;
import ru.crystallbloom.crystalladmin.models.WarnModel;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;
import ru.crystallbloom.crystalladmin.utils.TimeUtil;

import java.util.*;

public class AdmCommand implements CommandExecutor, TabCompleter {

    private final CrystallAdmin plugin;
    private final ReportsGUI reportsGUI;

    private static final List<String> SUBCOMMANDS = List.of(
            "cp", "checkplayer", "chat", "bc", "broadcast", "a", "anon",
            "reports", "vanish", "v", "class", "freeze", "f",
            "spy", "history", "hist", "alert", "tpo",
            "ban", "tempban", "unban", "mute", "tempmute", "unmute",
            "warn", "unwarn", "kick",
            "ss", "sstime", "ssclear",
            "note", "notes", "delnote",
            "invsee", "invec",
            "checkore",
            "laggclear", "lc",
            "reload", "help"
    );

    public AdmCommand(CrystallAdmin plugin) {
        this.plugin = plugin;
        this.reportsGUI = new ReportsGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crystalladmin.admin")) {
            sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.no-permission")));
            return true;
        }
        if (args.length == 0) { sendHelp(sender, 1); return true; }

        String sub = args[0].toLowerCase();

        // Log to Discord (skip help/reload to avoid spam)
        if (sender instanceof Player p && !sub.equals("help") && !sub.equals("reload")) {
            plugin.getDiscordManager().logAdmCommand(p.getName(),
                    "/adm " + String.join(" ", args));
        }

        switch (sub) {

            // ─────────────── CHECK PLAYER ────────────────────────────────
            case "checkplayer", "cp", "cpa" -> {
                if (args.length < 2) { usage(sender, "/adm cp <ник>"); return true; }
                handleCheckPlayer(sender, args[1]);
            }

            // ─────────────── ADMIN CHAT ──────────────────────────────────
            case "chat" -> {
                requirePlayer(sender);
                if (sender instanceof Player p) plugin.getAdminChatManager().toggle(p);
            }

            // ─────────────── BROADCAST ───────────────────────────────────
            case "bc", "broadcast" -> {
                if (args.length < 2) { usage(sender, "/adm bc <текст>"); return true; }
                String msg = joinArgs(args, 1);
                String fmt = plugin.getConfig().getString("broadcast.format",
                        "&8[&6ОБЪЯВЛЕНИЕ&8] &e%message%").replace("%message%", msg);
                Bukkit.broadcast(ColorUtil.color(fmt));
                plugin.getDiscordManager().logBroadcast(sender.getName(), msg);
            }

            // ─────────────── ANONYMOUS MESSAGE ───────────────────────────
            case "a", "anon" -> {
                if (args.length < 3) { usage(sender, "/adm a <игрок> <текст>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-not-found", "player", args[1]))); return true; }
                String fmt = plugin.getConfig().getString("anonymous.format",
                                "&2ОТВЕТ АДМИНИСТРАЦИИ&8: &a%message%")
                        .replace("%message%", joinArgs(args, 2));
                target.sendMessage(ColorUtil.color(fmt));
                sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("misc.anonymous-sent", "player", target.getName())));
            }

            // ─────────────── REPORTS GUI ─────────────────────────────────
            case "reports" -> {
                if (!requirePlayer(sender)) return true;
                reportsGUI.open((Player) sender);
            }

            // ─────────────── VANISH ──────────────────────────────────────
            case "vanish", "v" -> {
                if (!requirePlayer(sender)) return true;
                plugin.getVanishManager().toggle((Player) sender);
            }

            // ─────────────── CLASS (LP shortcut) ─────────────────────────
            case "class" -> {
                if (args.length < 3) { usage(sender, "/adm class <игрок> <группа>"); return true; }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + args[1] + " parent set " + args[2]);
                sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("misc.lp-class-set", "player", args[1], "group", args[2])));
                plugin.getDiscordManager().log("👥 КЛАСС: **" + args[1] + "** → `" + args[2] + "` (by **" + sender.getName() + "**)");
            }

            // ─────────────── FREEZE ──────────────────────────────────────
            case "freeze", "f" -> {
                if (args.length < 2) { usage(sender, "/adm freeze <игрок>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }
                if (plugin.getFreezeManager().isFrozen(target)) {
                    plugin.getFreezeManager().unfreeze(target);
                    sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("freeze.unfrozen", "player", target.getName())));
                } else {
                    plugin.getFreezeManager().freeze(target);
                    sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("freeze.frozen", "player", target.getName())));
                }
            }

            // ─────────────── SPY ─────────────────────────────────────────
            case "spy" -> {
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { usage(sender, "/adm spy <игрок>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }
                plugin.getSpyManager().toggleSpy((Player) sender, target);
            }

            // ─────────────── HISTORY ─────────────────────────────────────
            case "history", "hist" -> {
                if (args.length < 2) { usage(sender, "/adm history <игрок>"); return true; }
                handleHistory(sender, args[1]);
            }

            // ─────────────── ALERT ───────────────────────────────────────
            case "alert" -> {
                if (args.length < 2) { usage(sender, "/adm alert <текст>"); return true; }
                String msg = joinArgs(args, 1);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.showTitle(net.kyori.adventure.title.Title.title(
                            ColorUtil.color("&c" + msg),
                            ColorUtil.color("&8— &fАдминистрация CrystallBloom &8—"),
                            net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(3500),
                                    java.time.Duration.ofMillis(500))));
                    p.sendActionBar(ColorUtil.color("&c⚠ " + msg));
                }
                sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("misc.alert-sent")));
            }

            // ─────────────── TPO (silent tp) ─────────────────────────────
            case "tpo" -> {
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { usage(sender, "/adm tpo <игрок>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }
                ((Player) sender).teleport(target.getLocation());
                sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("misc.teleported", "player", target.getName())));
            }

            // ─────────────── INVSEE ──────────────────────────────────────
            case "invsee" -> {
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { usage(sender, "/adm invsee <игрок>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }
                // Create a copy of the player's inventory as a chest GUI
                Inventory view = Bukkit.createInventory(null, 54,
                        ColorUtil.color("&8Инвентарь &f" + target.getName()));
                // Copy main inventory (36 slots)
                ItemStack[] contents = target.getInventory().getContents();
                for (int i = 0; i < Math.min(contents.length, 36); i++) {
                    if (contents[i] != null) view.setItem(i, contents[i].clone());
                }
                // Armor in slots 36-39
                ItemStack[] armor = target.getInventory().getArmorContents();
                for (int i = 0; i < armor.length; i++) {
                    if (armor[i] != null) view.setItem(36 + i, armor[i].clone());
                }
                // Off-hand in slot 40
                ItemStack offhand = target.getInventory().getItemInOffHand();
                if (!offhand.getType().isAir()) view.setItem(40, offhand.clone());

                ((Player) sender).openInventory(view);
            }

            // ─────────────── INVEC (ender chest) ─────────────────────────
            case "invec" -> {
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { usage(sender, "/adm invec <игрок>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }
                Inventory ec = Bukkit.createInventory(null, 27,
                        ColorUtil.color("&8Эндер сундук &f" + target.getName()));
                ItemStack[] ecContents = target.getEnderChest().getContents();
                for (int i = 0; i < ecContents.length; i++) {
                    if (ecContents[i] != null) ec.setItem(i, ecContents[i].clone());
                }
                ((Player) sender).openInventory(ec);
            }

            // ─────────────── SS ──────────────────────────────────────────
            case "ss" -> {
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { usage(sender, "/adm ss <игрок>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }
                plugin.getSsManager().startSS(target, (Player) sender);
            }

            // /adm ssclear <player> — завершить проверку (прошёл)
            case "ssclear" -> {
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { usage(sender, "/adm ssclear <игрок>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }
                plugin.getSsManager().clearSS(target, (Player) sender);
            }

            // /adm sstime — показать кто сейчас на проверке
            case "sstime" -> {
                sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("screenshare.list-header")));
                boolean any = false;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getSsManager().isUnderSS(p.getUniqueId())) {
                        sender.sendMessage(ColorUtil.color("  &8» &c" + p.getName()));
                        any = true;
                    }
                }
                if (!any) sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("screenshare.list-empty")));
            }

            // ─────────────── STAFF NOTES ─────────────────────────────────
            case "note" -> {
                if (args.length < 3) { usage(sender, "/adm note <игрок> <текст>"); return true; }
                String targetName = args[1];
                String noteText = joinArgs(args, 2);
                // Resolve UUID
                var data = plugin.getDatabaseManager().getPlayerDataByName(targetName);
                if (data == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-not-found", "player", targetName))); return true; }
                plugin.getStaffNotesManager().addNote(
                        (String) data.get("uuid"), targetName, sender.getName(), noteText);
                sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("notes.added", "player", targetName)));
            }

            // /adm notes <player> — показать заметки
            case "notes" -> {
                if (args.length < 2) { usage(sender, "/adm notes <игрок>"); return true; }
                handleNotes(sender, args[1]);
            }

            // /adm delnote <player> <id> — удалить заметку
            case "delnote" -> {
                if (args.length < 3) { usage(sender, "/adm delnote <игрок> <id>"); return true; }
                try {
                    int id = Integer.parseInt(args[2]);
                    boolean ok = plugin.getStaffNotesManager().deleteNote(id);
                    sender.sendMessage(ok
                            ? ColorUtil.color("&7Заметка &c#" + id + " &7удалена.")
                            : ColorUtil.color(plugin.getLocaleManager().msg("notes.not-found")));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtil.color("&cID должен быть числом."));
                }
            }

            // ─────────────── CHECK ORE (X-Ray) ───────────────────────────
            case "checkore" -> {
                if (args.length < 2) { usage(sender, "/adm checkore <игрок>"); return true; }
                String report = plugin.getOreMonitorManager().buildReport(args[1]);
                if (report == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-not-found", "player", args[1]))); return true; }
                sender.sendMessage(ColorUtil.color(report));
            }

            // ─────────────── LAGG CLEAR ──────────────────────────────────
            case "laggclear", "lc" -> {
                plugin.getLaggClearManager().executeClear(sender);
            }

            // ─────────────── BAN / TEMPBAN / UNBAN ──────────────────────
            case "ban" -> {
                if (args.length < 3) { usage(sender, "/adm ban <игрок> <причина>"); return true; }
                banPlayer(sender, args[1], joinArgs(args, 2), -1L);
            }
            case "tempban", "tban" -> {
                if (args.length < 4) { usage(sender, "/adm tempban <игрок> <время> <причина>"); return true; }
                long dur = TimeUtil.parseDuration(args[2]);
                if (dur == -2L) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.invalid-time-format"))); return true; }
                banPlayer(sender, args[1], joinArgs(args, 3), dur == -1 ? -1 : System.currentTimeMillis() + dur);
            }
            case "unban" -> {
                if (args.length < 2) { usage(sender, "/adm unban <игрок>"); return true; }
                boolean ok = plugin.getPunishmentManager().unban(args[1]);
                sender.sendMessage(ok
                        ? ColorUtil.color(plugin.getLocaleManager().msg("punishments.unban-success", "player", args[1]))
                        : ColorUtil.color(plugin.getLocaleManager().msg("punishments.unban-not-found", "player", args[1])));
            }

            // ─────────────── MUTE / UNMUTE ───────────────────────────────
            case "mute", "tempmute" -> {
                if (args.length < 4) { usage(sender, "/adm mute <игрок> <время/perm> <причина>"); return true; }
                mutePlayer(sender, args[1], args[2], joinArgs(args, 3));
            }
            case "unmute" -> {
                if (args.length < 2) { usage(sender, "/adm unmute <игрок>"); return true; }
                boolean ok = plugin.getPunishmentManager().unmuteByName(args[1]);
                sender.sendMessage(ok
                        ? ColorUtil.color(plugin.getLocaleManager().msg("punishments.unmute-success", "player", args[1]))
                        : ColorUtil.color(plugin.getLocaleManager().msg("punishments.unmute-not-found", "player", args[1])));
            }

            // ─────────────── WARN / UNWARN ───────────────────────────────
            case "warn" -> {
                if (args.length < 4) { usage(sender, "/adm warn <игрок> <время/perm> <причина>"); return true; }
                warnPlayer(sender, args[1], args[2], joinArgs(args, 3));
            }
            case "unwarn" -> {
                if (args.length < 3) { usage(sender, "/adm unwarn <игрок> <номер>"); return true; }
                try {
                    int idx = Integer.parseInt(args[2]);
                    boolean ok = plugin.getPunishmentManager().unwarn(args[1], idx);
                    sender.sendMessage(ok
                            ? ColorUtil.color("&7Варн &c#" + idx + " &7удалён у &f" + args[1])
                            : ColorUtil.color(plugin.getLocaleManager().msg("punishments.unwarn-not-found")));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtil.color("&cНомер должен быть числом."));
                }
            }

            // ─────────────── KICK ────────────────────────────────────────
            case "kick" -> {
                if (args.length < 3) { usage(sender, "/adm kick <игрок> <причина>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }
                plugin.getPunishmentManager().kick(target, joinArgs(args, 2), sender.getName());
                sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("punishments.kick-success", "player", target.getName())));
            }

            // ─────────────── RELOAD ──────────────────────────────────────
            case "reload" -> {
                // 1. Reload bukkit config
                plugin.reloadConfig();
                // 2. Reload language files (picks up language change + new messages)
                plugin.getLocaleManager().reload();
                // 3. Reload lagg clear timers
                plugin.getLaggClearManager().reload();
                sender.sendMessage(ColorUtil.color(
                        plugin.getLocaleManager().msg("general.config-reloaded")));
                plugin.getLogger().info("[CrystallAdmin] Config reloaded by " + sender.getName());
            }

            // ─────────────── HELP ────────────────────────────────────────
            case "help" -> {
                int page = 1;
                if (args.length >= 2) {
                    try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                }
                sendHelp(sender, page);
            }

            default -> sendHelp(sender, 1);
        }
        return true;
    }

    // ═══════════════════════ HANDLERS ═══════════════════════════════════

    private void handleCheckPlayer(CommandSender sender, String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
            if (data == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-not-found", "player", playerName))));
                return;
            }

            String uuid = (String) data.get("uuid");
            Player online = Bukkit.getPlayer(playerName);

            int ping = online != null ? online.getPing() : -1;
            String version = (String) data.get("client_version");
            String brand   = (String) data.get("client_brand");
            String flags   = (String) data.get("client_flags");
            String ip      = (String) data.get("last_ip");

            List<Map<String, String>> alts = (ip != null && !ip.isEmpty())
                    ? plugin.getDatabaseManager().getAltsForIp(ip, uuid) : List.of();

            BanModel ban  = plugin.getDatabaseManager().getActiveBan(uuid);
            MuteModel mute = plugin.getDatabaseManager().getActiveMute(uuid);
            List<WarnModel> warns = plugin.getDatabaseManager().getActiveWarns(uuid);
            List<Map<String, Object>> notes = plugin.getStaffNotesManager().getNotes(uuid);
            int maxWarns = plugin.getConfig().getInt("warns.max-display", 3);

            long playtime = (long) data.get("total_playtime");
            if (online != null) {
                long start = plugin.getPlayerDataManager().getSessionStart(online.getUniqueId());
                playtime += System.currentTimeMillis() - start;
            }

            String spawnWorld = (String) data.get("spawn_world");
            double sx = (double) data.get("spawn_x"), sy = (double) data.get("spawn_y"), sz = (double) data.get("spawn_z");
            var L = plugin.getLocaleManager();
            String spawnStr = spawnWorld != null
                    ? spawnWorld + " [" + (int)sx + ", " + (int)sy + ", " + (int)sz + "]"
                    : L.msg("cp.spawn-none");

            final long finalPlaytime = playtime;
            Bukkit.getScheduler().runTask(plugin, () -> {
                String status = online != null ? L.msg("cp.status-online") : L.msg("cp.status-offline");
                sender.sendMessage(ColorUtil.color(L.msg("cp.header")));
                sender.sendMessage(ColorUtil.color("  &4&l" + data.get("name") + "  &8|  " + status));
                sender.sendMessage(ColorUtil.color(L.msg("cp.header")));
                sender.sendMessage(ColorUtil.color(L.msg("cp.uuid",    "value", uuid)));
                sender.sendMessage(ColorUtil.color(L.msg("cp.client",  "value", brand)));
                sender.sendMessage(ColorUtil.color(L.msg("cp.version", "value", version)));
                sender.sendMessage(ColorUtil.color(ping >= 0
                        ? L.msg("cp.ping", "value", ping)
                        : L.msg("cp.ping-offline")));
                sender.sendMessage(ColorUtil.color(L.msg("cp.ip", "value", ip != null ? ip : "—")));
                sender.sendMessage(ColorUtil.color(
                        (flags == null || flags.isEmpty())
                                ? L.msg("cp.flags-clean")
                                : L.msg("cp.flags-dirty", "value", flags)));
                sender.sendMessage(ColorUtil.color(L.msg("cp.last-seen", "value", TimeUtil.formatDate((long) data.get("last_seen")))));
                sender.sendMessage(ColorUtil.color(L.msg("cp.playtime",  "value", TimeUtil.formatPlaytime(finalPlaytime))));
                sender.sendMessage(ColorUtil.color(L.msg("cp.spawn",     "value", spawnStr)));
                sender.sendMessage(ColorUtil.color(L.msg("cp.warns", "current", warns.size(), "max", maxWarns)));

                // Alts
                if (!alts.isEmpty()) {
                    sender.sendMessage(ColorUtil.color(L.msg("cp.alts-header")));
                    for (var alt : alts)
                        sender.sendMessage(ColorUtil.color(L.msg("cp.alts-entry", "name", alt.get("name"))));
                }

                // Active punishments
                if (ban != null)
                    sender.sendMessage(ColorUtil.color(L.msg("cp.ban-active",
                            "reason", ban.getReason(), "expires", TimeUtil.formatExpiry(ban.getExpires()))));
                if (mute != null)
                    sender.sendMessage(ColorUtil.color(L.msg("cp.mute-active",
                            "reason", mute.getReason(), "expires", TimeUtil.formatExpiry(mute.getExpires()))));

                // Warns list
                if (!warns.isEmpty()) {
                    sender.sendMessage(ColorUtil.color(L.msg("cp.warns-header")));
                    for (var w : warns) {
                        sender.sendMessage(ColorUtil.color(L.msg("cp.warns-entry",
                                "id", w.getId(), "reason", w.getReason(),
                                "admin", w.getAdminName(), "expires", TimeUtil.formatExpiry(w.getExpires()))));
                    }
                }

                // Staff notes
                if (!notes.isEmpty()) {
                    sender.sendMessage(ColorUtil.color(L.msg("cp.notes-header")));
                    for (var n : notes) {
                        sender.sendMessage(ColorUtil.color(L.msg("cp.notes-entry",
                                "id", n.get("id"), "author", n.get("author"),
                                "note", n.get("note"), "date", TimeUtil.formatDate((long) n.get("timestamp")))));
                    }
                }

                sender.sendMessage(ColorUtil.color(L.msg("cp.header")));
            });
        });
    }

    private void handleHistory(CommandSender sender, String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
            if (data == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-not-found", "player", "?"))));
                return;
            }
            String uuid = (String) data.get("uuid");
            var bans  = plugin.getDatabaseManager().getBanHistory(uuid);
            var mutes = plugin.getDatabaseManager().getMuteHistory(uuid);
            var warns = plugin.getDatabaseManager().getAllWarns(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                var L = plugin.getLocaleManager();
                sender.sendMessage(ColorUtil.color(L.msg("history.header")));
                sender.sendMessage(ColorUtil.color(L.msg("history.title", "player", playerName)));
                sender.sendMessage(ColorUtil.color(L.msg("history.header")));
                sender.sendMessage(ColorUtil.color(L.msg("history.bans-header", "count", bans.size())));
                for (var b : bans)
                    sender.sendMessage(ColorUtil.color("    &8[" + TimeUtil.formatDate(b.getTimestamp())
                            + "] &f" + b.getReason() + " &8| &7" + b.getAdminName()
                            + " | " + TimeUtil.formatExpiry(b.getExpires())));
                sender.sendMessage(ColorUtil.color(L.msg("history.mutes-header", "count", mutes.size())));
                for (var m : mutes)
                    sender.sendMessage(ColorUtil.color("    &8[" + TimeUtil.formatDate(m.getTimestamp())
                            + "] &f" + m.getReason() + " &8| &7" + m.getAdminName()
                            + " | " + TimeUtil.formatExpiry(m.getExpires())));
                sender.sendMessage(ColorUtil.color(L.msg("history.warns-header", "count", warns.size())));
                for (var w : warns)
                    sender.sendMessage(ColorUtil.color("    &8[#" + w.getId() + " | " + TimeUtil.formatDate(w.getTimestamp())
                            + "] &f" + w.getReason() + " &8| &7" + w.getAdminName()
                            + " | " + TimeUtil.formatExpiry(w.getExpires())));
                sender.sendMessage(ColorUtil.color(L.msg("history.footer")));
            });
        });
    }

    private void handleNotes(CommandSender sender, String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
            if (data == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-not-found", "player", "?"))));
                return;
            }
            var notes = plugin.getStaffNotesManager().getNotes((String) data.get("uuid"));
            Bukkit.getScheduler().runTask(plugin, () -> {
                var L = plugin.getLocaleManager();
                sender.sendMessage(ColorUtil.color(L.msg("notes.list-header")));
                sender.sendMessage(ColorUtil.color(L.msg("notes.list-title", "player", playerName, "count", notes.size())));
                if (notes.isEmpty()) {
                    sender.sendMessage(ColorUtil.color(L.msg("notes.list-empty")));
                } else {
                    for (var n : notes) {
                        sender.sendMessage(ColorUtil.color(L.msg("cp.notes-entry",
                                "id", n.get("id"), "author", n.get("author"),
                                "note", n.get("note"), "date", TimeUtil.formatDate((long) n.get("timestamp")))));
                    }
                }
                sender.sendMessage(ColorUtil.color(L.msg("notes.list-footer")));
            });
        });
    }

    // ═══════════════════════ PUNISHMENT HELPERS ══════════════════════════

    private void banPlayer(CommandSender sender, String name, String reason, long expires) {
        Player target = Bukkit.getPlayer(name);
        if (target != null) {
            plugin.getPunishmentManager().ban(target, reason, sender.getName(), expires);
        } else {
            var data = plugin.getDatabaseManager().getPlayerDataByName(name);
            if (data == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-not-found", "player", "?"))); return; }
            plugin.getPunishmentManager().banOffline(name, (String) data.get("uuid"), reason, sender.getName(), expires);
        }
        sender.sendMessage(ColorUtil.color("&7Игрок &c" + name + " &7забанен. Причина: &f" + reason));
    }

    private void mutePlayer(CommandSender sender, String name, String timeStr, String reason) {
        long dur = TimeUtil.parseDuration(timeStr);
        if (dur == -2L) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.invalid-time-format"))); return; }
        long expires = dur == -1 ? -1 : System.currentTimeMillis() + dur;
        Player target = Bukkit.getPlayer(name);
        if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return; }
        plugin.getPunishmentManager().mute(target, reason, sender.getName(), expires);
        sender.sendMessage(ColorUtil.color("&7Игрок &e" + name + " &7замучен до: &f" + TimeUtil.formatExpiry(expires)));
    }

    private void warnPlayer(CommandSender sender, String name, String timeStr, String reason) {
        long dur = TimeUtil.parseDuration(timeStr);
        if (dur == -2L) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.invalid-time-format"))); return; }
        long expires = dur == -1 ? -1 : System.currentTimeMillis() + dur;
        Player target = Bukkit.getPlayer(name);
        if (target != null) {
            plugin.getPunishmentManager().warn(target, reason, sender.getName(), expires);
        } else {
            var data = plugin.getDatabaseManager().getPlayerDataByName(name);
            if (data == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-not-found", "player", "?"))); return; }
            plugin.getPunishmentManager().warnOffline(name, (String) data.get("uuid"), reason, sender.getName(), expires);
        }
        sender.sendMessage(ColorUtil.color("&7Варн выдан &f" + name + ". До: &f" + TimeUtil.formatExpiry(expires)));
    }

    // ═══════════════════════ HELP ════════════════════════════════════════

    private void sendHelp(CommandSender sender, int page) {
        // All strings come from messages_en.yml / messages_ru.yml
        // So /adm reload + language change works instantly
        var locale = plugin.getLocaleManager();

        sender.sendMessage(ColorUtil.color(locale.msg("help.header")));
        sender.sendMessage(ColorUtil.color(locale.msg("help.title", "page", page, "total", 3)));
        sender.sendMessage(ColorUtil.color(locale.msg("help.header")));

        java.util.List<String> lines = locale.msgList("help.commands.page" + page);
        for (String line : lines) {
            sender.sendMessage(ColorUtil.color(line));
        }

        if (page < 3) {
            sender.sendMessage(ColorUtil.color(locale.msg("help.next-page", "page", page + 1)));
        }
        sender.sendMessage(ColorUtil.color(locale.msg("help.footer")));
    }

    // ═══════════════════════ UTILS ═══════════════════════════════════════

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-only")));
            return false;
        }
        return true;
    }

    private void usage(CommandSender sender, String usage) {
        sender.sendMessage(ColorUtil.color("&cUsage: " + usage));
    }

    private String joinArgs(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    // ═══════════════════════ TAB COMPLETE ════════════════════════════════

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("crystalladmin.admin")) return List.of();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(input))
                    .toList();
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            boolean needsPlayer = List.of(
                    "cp", "checkplayer", "cpa", "a", "anon", "class", "freeze", "f",
                    "spy", "history", "hist", "tpo", "ban", "tempban", "tban",
                    "unban", "mute", "tempmute", "unmute", "warn", "unwarn", "kick",
                    "ss", "ssclear", "note", "notes", "delnote", "invsee", "invec", "checkore"
            ).contains(sub);
            if (needsPlayer) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return names;
            }
            if (sub.equals("help")) return List.of("1", "2", "3");
        }

        if (args.length == 3) {
            if (List.of("tempban", "tban", "mute", "tempmute", "warn").contains(sub)) {
                return List.of("30m", "1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d", "1mo", "perm");
            }
        }

        return List.of();
    }
}