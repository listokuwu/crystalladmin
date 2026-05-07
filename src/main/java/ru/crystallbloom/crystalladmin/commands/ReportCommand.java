package ru.crystallbloom.crystalladmin.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

import java.util.Arrays;

public class ReportCommand implements CommandExecutor {

    private final CrystallAdmin plugin;

    public ReportCommand(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color("&cТолько для игроков."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ColorUtil.color("&cИспользование: /report <текст>"));
            return true;
        }

        if (plugin.getPlayerDataManager().isOnReportCooldown(player.getUniqueId())) {
            long remaining = plugin.getPlayerDataManager().getRemainingCooldown(player.getUniqueId());
            player.sendMessage(ColorUtil.color("&cВы недавно отправляли репорт. Подождите &f" + remaining + "с"));
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        Location loc = player.getLocation();

        // Save with location
        plugin.getDatabaseManager().addReport(
                player.getUniqueId().toString(),
                player.getName(),
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                message
        );

        plugin.getPlayerDataManager().setReportCooldown(player.getUniqueId());

        player.sendMessage(ColorUtil.color(
                "\n&8[&2РЕПОРТ&8] &7Ваш репорт отправлен администрации.\n" +
                        "&8Сообщение: &f" + message + "\n"
        ));

        // Notify admins
        Component adminMsg = ColorUtil.color(
                "&8[&2РЕПОРТ&8] &7Новый репорт от &f" + player.getName() + "&8: &f" + message
        );
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("crystalladmin.admin")) {
                admin.sendMessage(adminMsg);
                admin.sendActionBar(ColorUtil.color("&c📩 Новый репорт от " + player.getName()));
            }
        }

        plugin.getDiscordManager().logReport(player.getName(), message);
        return true;
    }
}