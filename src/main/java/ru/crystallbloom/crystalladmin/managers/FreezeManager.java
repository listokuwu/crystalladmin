package ru.crystallbloom.crystalladmin.managers;

import org.bukkit.entity.Player;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeManager {

    private final CrystallAdmin plugin;
    private final Set<UUID> frozenPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public FreezeManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    public void freeze(Player player) {
        frozenPlayers.add(player.getUniqueId());
        player.sendMessage(ColorUtil.color(
                plugin.getConfig().getString("freeze.message-on-freeze",
                        "&cВы были заморожены администрацией сервера.")));
    }

    public void unfreeze(Player player) {
        frozenPlayers.remove(player.getUniqueId());
        player.sendMessage(ColorUtil.color(
                plugin.getConfig().getString("freeze.message-on-unfreeze", "&aВы были разморожены.")));
    }

    /** Silent freeze — no message (used by SS) */
    public void freezeSilent(UUID uuid) {
        frozenPlayers.add(uuid);
    }

    /** Silent unfreeze — no message (used by SS) */
    public void unfreezeSilent(UUID uuid) {
        frozenPlayers.remove(uuid);
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public void removeOnQuit(UUID uuid) {
        frozenPlayers.remove(uuid);
    }
}