package ru.crystallbloom.crystalladmin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

public class VanishListener implements Listener {

    private final CrystallAdmin plugin;

    public VanishListener(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    /**
     * Intercept /list, /who, /players commands so vanished admins
     * don't appear in them for regular players.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onListCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crystalladmin.admin")) return;

        String cmd = event.getMessage().toLowerCase().trim();
        if (cmd.equals("/list") || cmd.equals("/who") || cmd.equals("/players")) {
            // Cancel and send custom list without vanished players
            event.setCancelled(true);
            StringBuilder sb = new StringBuilder("&7Игроки онлайн: &f");
            int count = 0;
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (!plugin.getVanishManager().isVanished(p)) {
                    if (count > 0) sb.append("&8, &f");
                    sb.append(p.getName());
                    count++;
                }
            }
            player.sendMessage(ColorUtil.color(sb.toString() + " &8(&7" + count + "&8)"));
        }
    }
}