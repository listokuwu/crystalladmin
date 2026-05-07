package ru.crystallbloom.crystalladmin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

public class ChatListener implements Listener {

    private final CrystallAdmin plugin;

    public ChatListener(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Check mute
        if (plugin.getPunishmentManager().isMuted(player)) {
            event.setCancelled(true);
            String muteMsg = plugin.getPunishmentManager().getMuteMessage(player);
            if (muteMsg != null) player.sendMessage(ColorUtil.color(muteMsg));
            return;
        }

        // Admin chat mode
        if (player.hasPermission("crystalladmin.admin")) {
            // Quick prefix — message starts with "#"
            String quickPrefix = plugin.getConfig().getString("admin-chat.quick-prefix", "#");
            if (message.startsWith(quickPrefix + " ") || message.equals(quickPrefix)) {
                event.setCancelled(true);
                String adminMsg = message.length() > quickPrefix.length() + 1
                        ? message.substring(quickPrefix.length() + 1)
                        : "";
                if (!adminMsg.isEmpty()) {
                    plugin.getAdminChatManager().sendAdminMessage(player, adminMsg);
                }
                return;
            }

            // Toggled admin chat mode
            if (plugin.getAdminChatManager().isInAdminChat(player)) {
                event.setCancelled(true);
                plugin.getAdminChatManager().sendAdminMessage(player, message);
            }
        }
    }
}