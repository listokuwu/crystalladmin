package ru.crystallbloom.crystalladmin.managers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;

import java.util.List;

public class LaggClearManager {

    private final CrystallAdmin plugin;
    private BukkitTask clearTask;
    private BukkitTask warningTask;

    public LaggClearManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("lagg-clear.enabled", true)) return;
        long intervalSeconds = plugin.getConfig().getLong("lagg-clear.interval", 300);
        if (intervalSeconds <= 0) return;

        List<Integer> warnings = plugin.getConfig().getIntegerList("lagg-clear.warnings");

        // Schedule recurring cycle
        scheduleCycle(intervalSeconds, warnings);
    }

    private void scheduleCycle(long intervalSeconds, List<Integer> warnings) {
        // Schedule warnings and the clear itself
        for (int warnSec : warnings) {
            if (warnSec >= intervalSeconds) continue;
            long delay = (intervalSeconds - warnSec) * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String msg = plugin.getConfig().getString("lagg-clear.warning-message",
                                "&8[&6LAGG CLEAR&8] &eАвтоочистка через &f%seconds%с")
                        .replace("%seconds%", String.valueOf(warnSec));
                Bukkit.broadcast(ColorUtil.color(msg));
            }, delay);
        }

        clearTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            executeClear(null);
            // Schedule next cycle
            scheduleCycle(intervalSeconds, warnings);
        }, intervalSeconds * 20L);
    }

    /**
     * Execute clear manually or via scheduler.
     * @param sender nullable — if null, the message shows as server broadcast
     */
    public void executeClear(org.bukkit.command.CommandSender sender) {
        boolean clearItems = plugin.getConfig().getBoolean("lagg-clear.clear-items", true);
        boolean clearMobs  = plugin.getConfig().getBoolean("lagg-clear.clear-mobs", false);
        boolean keepTamed  = plugin.getConfig().getBoolean("lagg-clear.keep-tamed", true);
        boolean keepNamed  = plugin.getConfig().getBoolean("lagg-clear.keep-named", true);

        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (clearItems && entity instanceof Item) {
                    entity.remove();
                    count++;
                } else if (clearMobs && entity instanceof Mob mob) {
                    if (keepTamed && mob instanceof Tameable t && t.isTamed()) continue;
                    if (keepNamed && entity.getCustomName() != null) continue;
                    entity.remove();
                    count++;
                }
            }
        }

        String msg = plugin.getConfig().getString("lagg-clear.clear-message",
                        "&8[&6LAGG CLEAR&8] &aЗемля очищена! &7Удалено: &f%count% &7объектов.")
                .replace("%count%", String.valueOf(count));
        Bukkit.broadcast(ColorUtil.color(msg));
    }

    public void stop() {
        if (clearTask != null) clearTask.cancel();
        if (warningTask != null) warningTask.cancel();
    }

    public void reload() {
        stop();
        start();
    }
}