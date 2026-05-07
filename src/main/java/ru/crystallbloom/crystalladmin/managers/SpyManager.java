package ru.crystallbloom.crystalladmin.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpyManager {

    private final CrystallAdmin plugin;
    // admin UUID -> set of player UUIDs they are spying on (null = spy on all)
    private final Map<UUID, Set<UUID>> spyTargets = new ConcurrentHashMap<>();

    public SpyManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    /**
     * Toggle spy on a specific player, or pass null to spy on all.
     */
    public void toggleSpy(Player admin, Player target) {
        UUID adminUuid = admin.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        spyTargets.computeIfAbsent(adminUuid, k -> new HashSet<>());
        Set<UUID> targets = spyTargets.get(adminUuid);

        if (targets.contains(targetUuid)) {
            targets.remove(targetUuid);
            if (targets.isEmpty()) spyTargets.remove(adminUuid);
            admin.sendMessage(ColorUtil.color("&8[&4SPY&8] &7Вы перестали следить за &c" + target.getName()));
        } else {
            targets.add(targetUuid);
            admin.sendMessage(ColorUtil.color("&8[&4SPY&8] &7Вы начали следить за &c" + target.getName()));
        }
    }

    public boolean isSpyingOn(Player admin, Player target) {
        Set<UUID> targets = spyTargets.get(admin.getUniqueId());
        if (targets == null) return false;
        return targets.contains(target.getUniqueId());
    }

    /**
     * Notify all admins spying on this player about a command they used.
     */
    public void notifyCommandSpy(Player player, String command) {
        UUID playerUuid = player.getUniqueId();
        Component msg = ColorUtil.color(
                "&8[&4SPY&8] &7" + player.getName() + " &8>> &f" + command
        );
        for (Map.Entry<UUID, Set<UUID>> entry : spyTargets.entrySet()) {
            if (entry.getValue().contains(playerUuid)) {
                Player admin = Bukkit.getPlayer(entry.getKey());
                if (admin != null && admin.isOnline()) {
                    admin.sendMessage(msg);
                }
            }
        }
    }

    public void removeAdmin(UUID adminUuid) {
        spyTargets.remove(adminUuid);
    }
}