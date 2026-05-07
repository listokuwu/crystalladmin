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

public class VanishManager {

    private final CrystallAdmin plugin;
    // Players currently in vanish
    private final Set<UUID> vanishedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // Players who entered vanish before this session (persist across rejoins)
    private final Set<UUID> persistentVanish = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public VanishManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    public void vanish(Player player) {
        vanishedPlayers.add(player.getUniqueId());
        persistentVanish.add(player.getUniqueId());

        // Hide from all non-admin players
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.hasPermission("crystalladmin.admin") && !other.equals(player)) {
                other.hidePlayer(plugin, player);
            }
        }

        // Send fake leave message
        if (plugin.getConfig().getBoolean("moderation.vanish.fake-messages", true)) {
            String fakeLeave = plugin.getConfig().getString("moderation.vanish.fake-leave",
                    "&e{player} &fпокинул игру");
            Component msg = ColorUtil.color(
                    fakeLeave.replace("{player}", player.getName()).replace("%player%", player.getName()));
            Bukkit.broadcast(msg);
        }

        player.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("vanish.enabled")));
    }

    public void unvanish(Player player, boolean fakeJoin) {
        vanishedPlayers.remove(player.getUniqueId());
        persistentVanish.remove(player.getUniqueId());

        // Show to all players
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(plugin, player);
        }

        if (fakeJoin && plugin.getConfig().getBoolean("moderation.vanish.fake-messages", true)) {
            String fakeJoinMsg = plugin.getConfig().getString("moderation.vanish.fake-join",
                    "&e{player} &fзашёл на сервер");
            Component msg = ColorUtil.color(
                    fakeJoinMsg.replace("{player}", player.getName()).replace("%player%", player.getName()));
            Bukkit.broadcast(msg);
        }

        player.sendMessage(ColorUtil.color(plugin.getLocaleManager().msg("vanish.disabled")));
    }

    public void toggle(Player player) {
        if (isVanished(player)) {
            unvanish(player, true);
        } else {
            vanish(player);
        }
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public boolean isPersistentVanish(UUID uuid) {
        return persistentVanish.contains(uuid);
    }

    /**
     * Called when a vanished admin re-joins — re-apply vanish effects silently.
     */
    public void applyVanishOnJoin(Player player) {
        vanishedPlayers.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.hasPermission("crystalladmin.admin") && !other.equals(player)) {
                    other.hidePlayer(plugin, player);
                }
            }
        }, 1L);
    }

    /**
     * Called when a vanished admin disconnects — no real leave message should appear.
     */
    public void handleVanishedQuit(Player player) {
        // Just mark them as offline from vanish — no broadcast
        vanishedPlayers.remove(player.getUniqueId());
        // Keep in persistentVanish so next join re-applies
    }

    /**
     * Apply visibility rules for a newly joined player —
     * hide all currently vanished admins from them.
     */
    public void applyHiddenToNewPlayer(Player newPlayer) {
        if (newPlayer.hasPermission("crystalladmin.admin")) return;
        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && vanished.isOnline()) {
                newPlayer.hidePlayer(plugin, vanished);
            }
        }
    }

    public Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanishedPlayers);
    }
}