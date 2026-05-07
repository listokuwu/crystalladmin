package ru.crystallbloom.crystalladmin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import ru.crystallbloom.crystalladmin.CrystallAdmin;

public class BrandListener implements Listener, PluginMessageListener {

    private final CrystallAdmin plugin;
    private static final String BRAND_CHANNEL = "minecraft:brand";

    public BrandListener(CrystallAdmin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BRAND_CHANNEL, this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!BRAND_CHANNEL.equals(channel) || message.length == 0) return;

        // Delegate parsing to ClientDetectionManager (single source of truth)
        String brand = plugin.getClientDetectionManager().parseBrand(message);
        plugin.getDatabaseManager().updateClientBrand(player.getUniqueId().toString(), brand);
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        Player player = event.getPlayer();

        // Store protocol version as version string
        String version = protocolToVersion(player.getProtocolVersion());
        plugin.getDatabaseManager().updateClientVersion(player.getUniqueId().toString(), version);
    }

    private String protocolToVersion(int protocol) {
        return switch (protocol) {
            case 770 -> "1.21.4";
            case 769 -> "1.21.3";
            case 768 -> "1.21.1";
            case 767 -> "1.21";
            case 766 -> "1.20.6";
            case 765 -> "1.20.4";
            case 764 -> "1.20.2";
            case 763 -> "1.20.1";
            case 762 -> "1.19.4";
            case 761 -> "1.19.3";
            case 760 -> "1.19.2";
            case 759 -> "1.19";
            default  -> "proto:" + protocol;
        };
    }
}