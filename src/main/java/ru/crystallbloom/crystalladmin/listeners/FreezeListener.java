package ru.crystallbloom.crystalladmin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

public class FreezeListener implements Listener {

    private final CrystallAdmin plugin;

    public FreezeListener(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crystalladmin.bypass.freeze")) return;
        if (!plugin.getFreezeManager().isFrozen(player)) return;

        // Allow head rotation but block actual movement
        if (event.getFrom().getX() != event.getTo().getX()
            || event.getFrom().getY() != event.getTo().getY()
            || event.getFrom().getZ() != event.getTo().getZ()) {

            event.setTo(event.getFrom().clone().setDirection(event.getTo().getDirection()));

            String msg = plugin.getConfig().getString("freeze.move-blocked-message",
                "&cВы заморожены! Вы не можете двигаться.");
            // Throttle the message (send only occasionally)
            if (Math.random() < 0.05) {
                player.sendMessage(ColorUtil.color(msg));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crystalladmin.bypass.freeze")) return;
        if (!plugin.getFreezeManager().isFrozen(player)) return;

        // Only block player-initiated teleports, not plugin-triggered ones
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
            || cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            event.setCancelled(true);
        }
    }
}
