package ru.crystallbloom.crystalladmin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;

public class OreListener implements Listener {

    private final CrystallAdmin plugin;

    public OreListener(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crystalladmin.admin")) return; // don't track admins

        var material = event.getBlock().getType();

        // Check if it's a tracked ore
        if (plugin.getOreMonitorManager().isTrackedOre(material)) {
            plugin.getOreMonitorManager().onOreMined(player, material);
        }

        // Track background blocks for ratio
        if (plugin.getOreMonitorManager().isBackgroundBlock(material)) {
            plugin.getOreMonitorManager().onBackgroundMined(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("crystalladmin.bypass.anticheat")) return;

        // Pass to client detection for FastPlace check
        plugin.getClientDetectionManager().onBlockPlace(player);
    }
}