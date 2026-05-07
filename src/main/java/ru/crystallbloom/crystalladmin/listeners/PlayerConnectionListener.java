package ru.crystallbloom.crystalladmin.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.models.BanModel;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

public class PlayerConnectionListener implements Listener {

    private final CrystallAdmin plugin;

    public PlayerConnectionListener(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress() != null
                ? player.getAddress().getAddress().getHostAddress() : "unknown";
        long now = System.currentTimeMillis();

        // ── Ban check ────────────────────────────────────────────
        BanModel ban = plugin.getDatabaseManager().getActiveBan(uuid);
        if (ban != null && !ban.isExpired()) {
            event.setJoinMessage(null);
            Component banScreen = plugin.getPunishmentManager()
                    .buildBanScreen(ban.getReason(), ban.getAdminName(), ban.getExpires());
            Bukkit.getScheduler().runTask(plugin, () -> player.kick(banScreen));
            return;
        }

        // ── Session start ────────────────────────────────────────
        plugin.getPlayerDataManager().startSession(player.getUniqueId());
        plugin.getDatabaseManager().savePlayerData(uuid, player.getName(), ip, now);
        plugin.getDatabaseManager().logIp(uuid, player.getName(), ip);
        plugin.getDatabaseManager().logSession(uuid, player.getName(), now, 0);

        // ── Vanish re-apply (admin returning in vanish) ──────────
        if (plugin.getVanishManager().isPersistentVanish(player.getUniqueId())) {
            event.setJoinMessage(null);
            plugin.getVanishManager().applyVanishOnJoin(player);
            return;
        }

        // ── Hide vanished admins from this new player ────────────
        plugin.getVanishManager().applyHiddenToNewPlayer(player);

        // ── Admin pending reports notification ───────────────────
        if (player.hasPermission("crystalladmin.admin")) {
            int pending = plugin.getDatabaseManager().getOpenReportCount();
            if (pending > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        player.sendMessage(ColorUtil.color(
                                "&8[&2РЕПОРТЫ&8] &7Ожидают рассмотрения: &c" + pending
                                        + " &7репортов. &8(/adm reports)"
                        )), 20L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // ── SS auto-ban on disconnect ────────────────────────────
        if (plugin.getSsManager().isUnderSS(player.getUniqueId())) {
            event.setQuitMessage(null);
            plugin.getSsManager().handleQuit(player.getUniqueId(), player.getName());
            cleanup(player, uuid);
            return;
        }

        // ── Vanish: suppress quit message ────────────────────────
        if (plugin.getVanishManager().isVanished(player)) {
            event.setQuitMessage(null);
            plugin.getVanishManager().handleVanishedQuit(player);
            cleanup(player, uuid);
            return;
        }

        cleanup(player, uuid);
    }

    private void cleanup(Player player, String uuid) {
        long sessionDuration = plugin.getPlayerDataManager().endSession(player.getUniqueId());
        plugin.getDatabaseManager().updatePlayerLeave(uuid, sessionDuration);
        plugin.getDatabaseManager().logSession(uuid, player.getName(),
                plugin.getPlayerDataManager().getSessionStart(player.getUniqueId()),
                System.currentTimeMillis());
        plugin.getFreezeManager().removeOnQuit(player.getUniqueId());
        plugin.getSpyManager().removeAdmin(player.getUniqueId());
        plugin.getClientDetectionManager().cleanup(player.getUniqueId());
        plugin.getOreMonitorManager().clearCache(player.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location bed = player.getBedSpawnLocation();
        if (bed != null) {
            plugin.getDatabaseManager().updateSpawnPoint(
                    player.getUniqueId().toString(),
                    bed.getWorld().getName(), bed.getX(), bed.getY(), bed.getZ()
            );
        }
    }
}