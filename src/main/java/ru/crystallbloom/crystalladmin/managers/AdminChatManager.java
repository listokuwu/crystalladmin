package ru.crystallbloom.crystalladmin.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AdminChatManager {

    private final CrystallAdmin plugin;
    private final Set<UUID> adminChatMode = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AdminChatManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    public void toggle(Player player) {
        if (adminChatMode.contains(player.getUniqueId())) {
            adminChatMode.remove(player.getUniqueId());
            player.sendMessage(ColorUtil.color("&8[&4ADMINCHAT&8] &7Режим &cвыключен&7."));
        } else {
            adminChatMode.add(player.getUniqueId());
            player.sendMessage(ColorUtil.color("&8[&4ADMINCHAT&8] &7Режим &aвключен&7. Все сообщения идут в админ-чат."));
        }
    }

    public boolean isInAdminChat(Player player) {
        return adminChatMode.contains(player.getUniqueId());
    }

    public void sendAdminMessage(Player sender, String message) {
        String format = plugin.getConfig().getString("admin-chat.format",
                "&8[&4ADMIN&8] &c%player%&8: &7%message%");
        Component component = ColorUtil.color(format
                .replace("%player%", sender.getName())
                .replace("%message%", message));
        broadcastRaw(component);
        plugin.getLogger().info("[ADMINCHAT] " + sender.getName() + ": " + message);
    }

    /** Broadcast any pre-built component to all online admins */
    public void broadcastRaw(Component component) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("crystalladmin.admin")) {
                p.sendMessage(component);
            }
        }
    }
}