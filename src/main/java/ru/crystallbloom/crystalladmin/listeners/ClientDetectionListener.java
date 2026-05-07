package ru.crystallbloom.crystalladmin.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.crystallbloom.crystalladmin.CrystallAdmin;

public class ClientDetectionListener implements Listener {

    private final CrystallAdmin plugin;

    public ClientDetectionListener(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    /**
     * CRITICAL FIX: Only check reach on direct melee hits BY player.
     * Ignore:
     * - Projectiles (arrows, snowballs, etc.)
     * - Damage TO player (dragon hits, mob attacks)
     * - Admin bypass
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        // IGNORE projectiles entirely (arrows, snowballs, tridents, etc.)
        if (damager instanceof Projectile) return;

        // ONLY check if damager is a PLAYER
        if (!(damager instanceof Player attacker)) return;

        // Bypass for admins
        if (attacker.hasPermission("crystalladmin.bypass.anticheat")) return;

        // Get target entity
        Entity target = event.getEntity();

        // Calculate distance
        double distance = attacker.getLocation().distance(target.getLocation());

        // Send to detection manager
        plugin.getClientDetectionManager().onReach(attacker, distance);
    }
}