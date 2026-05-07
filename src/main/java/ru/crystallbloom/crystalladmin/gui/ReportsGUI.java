package ru.crystallbloom.crystalladmin.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.models.ReportModel;
import ru.crystallbloom.crystalladmin.utils.ColorUtil;
import ru.crystallbloom.crystalladmin.utils.TimeUtil;

import java.util.*;

public class ReportsGUI implements Listener {

    private final CrystallAdmin plugin;
    private static final String GUI_TITLE_PLAIN = "Репорты » CrystallBloom";

    // admin UUID -> { slot -> reportId }
    private final Map<UUID, Map<Integer, Integer>> openGuis = new HashMap<>();

    public ReportsGUI(CrystallAdmin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player admin) {
        List<ReportModel> reports = plugin.getDatabaseManager().getOpenReports();

        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&4&lРепорты &8» &7CrystallBloom"));

        // Bottom bar decoration
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        // Info slot (centre of bar)
        inv.setItem(49, makeItem(Material.PAPER,
                "&7Открытых репортов: &c" + reports.size()));

        Map<Integer, Integer> slotMap = new HashMap<>();
        int slot = 0;
        for (ReportModel r : reports) {
            if (slot >= 45) break;
            inv.setItem(slot, buildReportItem(r));
            slotMap.put(slot, r.getId());
            slot++;
        }

        openGuis.put(admin.getUniqueId(), slotMap);
        admin.openInventory(inv);
    }

    private ItemStack buildReportItem(ReportModel r) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        // Head texture by reporter name
        var profile = Bukkit.createPlayerProfile(r.getReporterName());
        meta.setOwnerProfile(profile);

        // Title colour by status
        if (r.isInProgress()) {
            meta.displayName(ColorUtil.color("&e[В РАБОТЕ] &f" + r.getReporterName()));
        } else {
            meta.displayName(ColorUtil.color("&c[ОТКРЫТЫЙ] &f" + r.getReporterName()));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.color("&8ID: &7#" + r.getId()));
        lore.add(ColorUtil.color("&8Время: &7" + TimeUtil.formatDate(r.getTimestamp())));
        lore.add(Component.empty());
        lore.add(ColorUtil.color("&8Сообщение:"));
        for (String line : wrapText(r.getMessage(), 36)) {
            lore.add(ColorUtil.color("&f" + line));
        }
        lore.add(Component.empty());

        // Location line
        if (r.hasLocation()) {
            lore.add(ColorUtil.color("&8Локация: &7" + r.getReporterWorld()
                    + " &8[&7" + (int) r.getReporterX() + ", "
                    + (int) r.getReporterY() + ", "
                    + (int) r.getReporterZ() + "&8]"));
            lore.add(Component.empty());
        }

        if (r.isInProgress() && r.getClaimedByName() != null) {
            lore.add(ColorUtil.color("&8В работе у: &e" + r.getClaimedByName()));
            lore.add(Component.empty());
            lore.add(ColorUtil.color("&7[ЛКМ] &fЗакрыть репорт"));
        } else {
            lore.add(ColorUtil.color("&7[ЛКМ] &fВзять в работу"));
            lore.add(ColorUtil.color("&7[ПКМ] &fЗакрыть репорт"));
        }

        if (r.hasLocation()) {
            lore.add(ColorUtil.color("&7[SHIFT+ЛКМ] &fТелепортироваться к месту репорта"));
        }

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;

        // Identify our GUI by plain title
        String plainTitle = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        if (!plainTitle.contains("Репорты") || !plainTitle.contains("CrystallBloom")) return;

        event.setCancelled(true);

        Map<Integer, Integer> slotMap = openGuis.get(admin.getUniqueId());
        if (slotMap == null) return;

        Integer reportId = slotMap.get(event.getRawSlot());
        if (reportId == null) return;

        // Find the report for TP
        ReportModel report = plugin.getDatabaseManager().getOpenReports().stream()
                .filter(r -> r.getId() == reportId).findFirst().orElse(null);

        // SHIFT+LeftClick → teleport
        if (event.isShiftClick() && event.isLeftClick()) {
            if (report != null && report.hasLocation()) {
                World world = Bukkit.getWorld(report.getReporterWorld());
                if (world != null) {
                    Location loc = new Location(world,
                            report.getReporterX(), report.getReporterY(), report.getReporterZ());
                    admin.closeInventory();
                    admin.teleport(loc);
                    admin.sendMessage(ColorUtil.color(
                            "&8[&2РЕПОРТЫ&8] &7Телепорт к репорту &c#" + reportId
                    ));
                    return;
                }
            }
            admin.sendMessage(ColorUtil.color("&cЛокация недоступна."));
            return;
        }

        // LeftClick → claim or close
        if (event.isLeftClick()) {
            boolean inProgress = report != null && report.isInProgress();
            if (!inProgress) {
                plugin.getDatabaseManager().claimReport(reportId,
                        admin.getUniqueId().toString(), admin.getName());
                admin.sendMessage(ColorUtil.color(
                        "&8[&2РЕПОРТЫ&8] &7Репорт &c#" + reportId + " &7взят в работу."));
            } else {
                plugin.getDatabaseManager().closeReport(reportId);
                admin.sendMessage(ColorUtil.color(
                        "&8[&2РЕПОРТЫ&8] &7Репорт &c#" + reportId + " &7закрыт."));
            }
        }

        // RightClick → close
        if (event.isRightClick()) {
            plugin.getDatabaseManager().closeReport(reportId);
            admin.sendMessage(ColorUtil.color(
                    "&8[&2РЕПОРТЫ&8] &7Репорт &c#" + reportId + " &7закрыт."));
        }

        // Refresh
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin), 1L);
    }

    private ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ColorUtil.color(name));
        item.setItemMeta(meta);
        return item;
    }

    private List<String> wrapText(String text, int maxLen) {
        List<String> lines = new ArrayList<>();
        while (text.length() > maxLen) {
            int cut = text.lastIndexOf(' ', maxLen);
            if (cut == -1) cut = maxLen;
            lines.add(text.substring(0, cut));
            text = text.substring(cut).trim();
        }
        if (!text.isEmpty()) lines.add(text);
        return lines;
    }

    public void cleanup(UUID adminUuid) { openGuis.remove(adminUuid); }
}