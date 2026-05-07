package ru.crystallbloom.crystalladmin.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;
import ru.crystallbloom.crystalladmin.utils.TimeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles all standalone punishment commands without the /adm prefix.
 * Registered at higher priority than Essentials via plugin.yml command registration.
 */
public class PunishmentCommands implements CommandExecutor, TabCompleter {

    private final CrystallAdmin plugin;

    public PunishmentCommands(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crystalladmin.admin")) {
            sender.sendMessage(ColorUtil.color("&cНедостаточно прав."));
            return true;
        }

        String cmd = command.getName().toLowerCase();

        switch (cmd) {

            // ── /ban <player> <reason> ─────────────────────────────────────
            case "ban" -> {
                if (args.length < 2) { usage(sender, "/ban <игрок> <причина>"); return true; }
                banPlayer(sender, args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)), -1L);
            }

            // ── /tempban <player> <time> <reason> ─────────────────────────
            case "tempban" -> {
                if (args.length < 3) { usage(sender, "/tempban <игрок> <время> <причина>"); return true; }
                long dur = TimeUtil.parseDuration(args[1]);
                if (dur == -2L) { sender.sendMessage(ColorUtil.color("&cНеверный формат времени. Пример: 1h, 30m, 7d")); return true; }
                long expires = dur == -1 ? -1 : System.currentTimeMillis() + dur;
                banPlayer(sender, args[0], String.join(" ", Arrays.copyOfRange(args, 2, args.length)), expires);
            }

            // ── /unban <player> ────────────────────────────────────────────
            case "unban" -> {
                if (args.length < 1) { usage(sender, "/unban <игрок>"); return true; }
                boolean ok = plugin.getPunishmentManager().unban(args[0]);
                sender.sendMessage(ok
                        ? ColorUtil.color("&7Игрок &f" + args[0] + " &7разбанен.")
                        : ColorUtil.color("&cАктивный бан не найден для &f" + args[0]));
            }

            // ── /mute <player> <time/perm> <reason> ───────────────────────
            case "mute" -> {
                if (args.length < 3) { usage(sender, "/mute <игрок> <время/perm> <причина>"); return true; }
                mutePlayer(sender, args[0], args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }

            // ── /tempmute <player> <time> <reason> ────────────────────────
            case "tempmute" -> {
                if (args.length < 3) { usage(sender, "/tempmute <игрок> <время> <причина>"); return true; }
                mutePlayer(sender, args[0], args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }

            // ── /unmute <player> ──────────────────────────────────────────
            case "unmute" -> {
                if (args.length < 1) { usage(sender, "/unmute <игрок>"); return true; }
                boolean ok = plugin.getPunishmentManager().unmuteByName(args[0]);
                sender.sendMessage(ok
                        ? ColorUtil.color("&7Игрок &f" + args[0] + " &7размучен.")
                        : ColorUtil.color("&cАктивный мут не найден для &f" + args[0]));
            }

            // ── /warn <player> <time/perm> <reason> ───────────────────────
            case "warn" -> {
                if (args.length < 3) { usage(sender, "/warn <игрок> <время/perm> <причина>"); return true; }
                warnPlayer(sender, args[0], args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }

            // ── /unwarn <player> <number> ─────────────────────────────────
            case "unwarn" -> {
                if (args.length < 2) { usage(sender, "/unwarn <игрок> <номер>"); return true; }
                try {
                    int idx = Integer.parseInt(args[1]);
                    boolean ok = plugin.getPunishmentManager().unwarn(args[0], idx);
                    sender.sendMessage(ok
                            ? ColorUtil.color("&7Варн &c#" + idx + " &7удалён у &f" + args[0])
                            : ColorUtil.color("&cВарн не найден."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtil.color("&cНомер должен быть числом."));
                }
            }

            // ── /kick <player> <reason> ───────────────────────────────────
            case "kick" -> {
                if (args.length < 2) { usage(sender, "/kick <игрок> <причина>"); return true; }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) { sender.sendMessage(ColorUtil.color("&cИгрок не в сети.")); return true; }
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getPunishmentManager().kick(target, reason, sender.getName());
                sender.sendMessage(ColorUtil.color("&7Игрок &f" + target.getName() + " &7кикнут. Причина: &c" + reason));
                plugin.getDiscordManager().logAdmCommand(sender.getName(), "/kick " + target.getName() + " " + reason);
            }
        }

        return true;
    }

    private void banPlayer(CommandSender sender, String playerName, String reason, long expires) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            plugin.getPunishmentManager().ban(target, reason, sender.getName(), expires);
        } else {
            var data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
            if (data == null) { sender.sendMessage(ColorUtil.color("&cИгрок не найден в базе данных.")); return; }
            plugin.getPunishmentManager().banOffline(playerName, (String) data.get("uuid"), reason, sender.getName(), expires);
        }
        sender.sendMessage(ColorUtil.color("&7Игрок &c" + playerName + " &7забанен. Причина: &f" + reason
                + (expires == -1 ? "" : " &8| &7До: &f" + TimeUtil.formatExpiry(expires))));
        plugin.getDiscordManager().logAdmCommand(sender.getName(), "/ban " + playerName + " " + reason);
    }

    private void mutePlayer(CommandSender sender, String playerName, String timeStr, String reason) {
        long dur = TimeUtil.parseDuration(timeStr);
        if (dur == -2L) { sender.sendMessage(ColorUtil.color("&cНеверный формат времени.")); return; }
        long expires = dur == -1 ? -1 : System.currentTimeMillis() + dur;

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) { sender.sendMessage(ColorUtil.color("&cИгрок не в сети.")); return; }
        plugin.getPunishmentManager().mute(target, reason, sender.getName(), expires);
        sender.sendMessage(ColorUtil.color("&7Игрок &e" + playerName + " &7замучен. До: &f"
                + TimeUtil.formatExpiry(expires)));
    }

    private void warnPlayer(CommandSender sender, String playerName, String timeStr, String reason) {
        long dur = TimeUtil.parseDuration(timeStr);
        if (dur == -2L) { sender.sendMessage(ColorUtil.color("&cНеверный формат времени.")); return; }
        long expires = dur == -1 ? -1 : System.currentTimeMillis() + dur;

        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            plugin.getPunishmentManager().warn(target, reason, sender.getName(), expires);
        } else {
            var data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
            if (data == null) { sender.sendMessage(ColorUtil.color("&cИгрок не найден.")); return; }
            plugin.getPunishmentManager().warnOffline(playerName, (String) data.get("uuid"), reason, sender.getName(), expires);
        }
        sender.sendMessage(ColorUtil.color("&7Варн выдан &f" + playerName + ". До: &f" + TimeUtil.formatExpiry(expires)));
    }

    private void usage(CommandSender sender, String usage) {
        sender.sendMessage(ColorUtil.color("&cИспользование: " + usage));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("crystalladmin.admin")) return List.of();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        String cmd = command.getName().toLowerCase();
        if (args.length == 2 && (cmd.equals("tempban") || cmd.equals("mute")
                || cmd.equals("tempmute") || cmd.equals("warn"))) {
            return List.of("30m", "1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d", "perm");
        }
        return List.of();
    }
}