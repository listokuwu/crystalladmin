package ru.crystallbloom.crystalladmin.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

public class StandaloneCommands implements CommandExecutor {
    private final CrystallAdmin plugin;

    public StandaloneCommands(CrystallAdmin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("crystalladmin.admin")) {
            sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.no-permission")));
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "checkplayer", "cp", "cpa" -> {
                if (args.length < 1) { sender.sendMessage(ColorUtil.color("&c/checkplayer <player>")); return true; }
                Bukkit.dispatchCommand(sender, "adm cp " + args[0]);
            }
            case "checkore" -> {
                if (args.length < 1) { sender.sendMessage(ColorUtil.color("&c/checkore <player>")); return true; }
                Bukkit.dispatchCommand(sender, "adm checkore " + args[0]);
            }
            case "screenshare", "ss" -> {
                if (args.length < 1) { sender.sendMessage(ColorUtil.color("&c/ss <player>")); return true; }
                Bukkit.dispatchCommand(sender, "adm ss " + args[0]);
            }
            case "staffnote", "note" -> {
                if (args.length < 2) { sender.sendMessage(ColorUtil.color("&c/note <player> <text>")); return true; }
                Bukkit.dispatchCommand(sender, "adm note " + String.join(" ", args));
            }
            case "adminchat", "ac" -> Bukkit.dispatchCommand(sender, "adm chat");
            case "invsee" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-only"))); return true; }
                if (args.length < 1) { sender.sendMessage(ColorUtil.color("&c/invsee <player>")); return true; }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }

                Inventory view = Bukkit.createInventory(null, 54, ColorUtil.color("&8Inventory: &f" + target.getName()));
                ItemStack[] contents = target.getInventory().getContents();
                for (int i = 0; i < Math.min(contents.length, 36); i++) {
                    if (contents[i] != null) view.setItem(i, contents[i].clone());
                }
                ItemStack[] armor = target.getInventory().getArmorContents();
                for (int i = 0; i < armor.length; i++) {
                    if (armor[i] != null) view.setItem(36 + i, armor[i].clone());
                }
                p.openInventory(view);
            }
            case "enderchest", "ec", "invec" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-only"))); return true; }
                if (args.length < 1) { sender.sendMessage(ColorUtil.color("&c/ec <player>")); return true; }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) { sender.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("general.player-offline"))); return true; }

                Inventory ec = Bukkit.createInventory(null, 27, ColorUtil.color("&8Ender Chest: &f" + target.getName()));
                ItemStack[] ecContents = target.getEnderChest().getContents();
                for (int i = 0; i < ecContents.length; i++) {
                    if (ecContents[i] != null) ec.setItem(i, ecContents[i].clone());
                }
                p.openInventory(ec);
            }
        }
        return true;
    }
}