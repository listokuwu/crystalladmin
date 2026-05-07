package ru.crystallbloom.crystalladmin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

import java.util.List;

public class MuteListener implements Listener {

    private final CrystallAdmin plugin;

    public MuteListener(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crystalladmin.bypass.mute")) return;
        if (!plugin.getPunishmentManager().isMuted(player)) return;

        String rawCommand = event.getMessage().toLowerCase().substring(1); // remove leading /
        String baseCommand = rawCommand.split(" ")[0];

        // Check always-allowed list first
        List<String> allowed = plugin.getConfig().getStringList("mute.allowed-commands");
        for (String allow : allowed) {
            if (baseCommand.equalsIgnoreCase(allow.toLowerCase())) return;
        }

        // Check blocked list
        List<String> blocked = plugin.getConfig().getStringList("mute.blocked-commands");
        for (String block : blocked) {
            if (baseCommand.equalsIgnoreCase(block.toLowerCase())) {
                event.setCancelled(true);
                String muteMsg = plugin.getPunishmentManager().getMuteMessage(player);
                if (muteMsg != null) player.sendMessage(ColorUtil.color(muteMsg));
                return;
            }
        }

        // Spy notification for non-blocked commands
        plugin.getSpyManager().notifyCommandSpy(player, event.getMessage());
    }
}