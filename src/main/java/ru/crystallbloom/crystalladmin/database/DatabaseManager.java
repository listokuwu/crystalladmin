package ru.crystallbloom.crystalladmin.database;

import ru.crystallbloom.crystalladmin.CrystallAdmin;
import ru.crystallbloom.crystalladmin.models.*;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final CrystallAdmin plugin;
    private volatile Connection connection;

    // After maven-shade relocation: org.sqlite → ru.crystallbloom.crystalladmin.shaded.sqlite
    private static final String JDBC_CLASS = "ru.crystallbloom.crystalladmin.shaded.sqlite.JDBC";
    // Fallback if running without shading (dev environment)
    private static final String JDBC_CLASS_FALLBACK = "org.sqlite.JDBC";

    public DatabaseManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            if (!dbFile.exists()) {
                dbFile.getParentFile().mkdirs();
            }

            // Загружаем стандартный класс драйвера.
            // Больше никаких "ru.crystallbloom...shaded"!
            Class.forName("org.sqlite.JDBC");

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            this.connection = DriverManager.getConnection(url);

            // Настройки оптимизации (твои PRAGMA)
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
                st.execute("PRAGMA foreign_keys=ON");
            }

            createTables(); // Вся логика таблиц Клауда остается ТУТ и она будет работать
            plugin.getLogger().info("[DB] SQLite initialized successfully.");

        } catch (Exception e) {
            plugin.getLogger().severe("[DB] FATAL: Failed to initialize database!");
            e.printStackTrace();
            // Выключаем плагин, чтобы не было ошибок в консоли дальше
            org.bukkit.Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    /** Guard — every method that touches DB calls this first */
    private boolean isReady() {
        if (connection == null) {
            plugin.getLogger().warning("[DB] Query skipped: connection is null (DB not initialized).");
            return false;
        }
        try {
            if (connection.isClosed()) {
                plugin.getLogger().warning("[DB] Query skipped: connection is closed.");
                return false;
            }
        } catch (SQLException ignored) {}
        return true;
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    last_ip TEXT,
                    client_brand TEXT DEFAULT 'Unknown',
                    client_version TEXT DEFAULT 'Unknown',
                    client_flags TEXT DEFAULT '',
                    last_seen LONG DEFAULT 0,
                    total_playtime LONG DEFAULT 0,
                    session_start LONG DEFAULT 0,
                    spawn_world TEXT,
                    spawn_x DOUBLE DEFAULT 0,
                    spawn_y DOUBLE DEFAULT 0,
                    spawn_z DOUBLE DEFAULT 0
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS ip_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    ip TEXT NOT NULL,
                    timestamp LONG NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reporter_uuid TEXT NOT NULL,
                    reporter_name TEXT NOT NULL,
                    reporter_world TEXT DEFAULT '',
                    reporter_x DOUBLE DEFAULT 0,
                    reporter_y DOUBLE DEFAULT 0,
                    reporter_z DOUBLE DEFAULT 0,
                    message TEXT NOT NULL,
                    timestamp LONG NOT NULL,
                    status TEXT DEFAULT 'OPEN',
                    claimed_by TEXT DEFAULT NULL,
                    claimed_by_name TEXT DEFAULT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS bans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    admin_name TEXT NOT NULL,
                    timestamp LONG NOT NULL,
                    expires LONG DEFAULT -1,
                    active INTEGER DEFAULT 1
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS mutes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    admin_name TEXT NOT NULL,
                    timestamp LONG NOT NULL,
                    expires LONG DEFAULT -1,
                    active INTEGER DEFAULT 1
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS warns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    admin_name TEXT NOT NULL,
                    timestamp LONG NOT NULL,
                    expires LONG DEFAULT -1,
                    active INTEGER DEFAULT 1
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS staff_notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_uuid TEXT NOT NULL,
                    target_name TEXT NOT NULL,
                    author_name TEXT NOT NULL,
                    note TEXT NOT NULL,
                    timestamp LONG NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS ore_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    ore_type TEXT NOT NULL,
                    count INTEGER DEFAULT 0,
                    cobblestone_count INTEGER DEFAULT 0,
                    window_start LONG NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    join_time LONG NOT NULL,
                    leave_time LONG DEFAULT 0,
                    duration LONG DEFAULT 0
                )
            """);
        }
    }

    // ═══════════════════════ PLAYER DATA ═══════════════════════

    public void savePlayerData(String uuid, String name, String ip, long sessionStart) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO player_data (uuid, name, last_ip, last_seen, session_start)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name=excluded.name, last_ip=excluded.last_ip,
                last_seen=excluded.last_seen, session_start=excluded.session_start
        """)) {
            ps.setString(1, uuid); ps.setString(2, name); ps.setString(3, ip);
            ps.setLong(4, System.currentTimeMillis()); ps.setLong(5, sessionStart);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updatePlayerLeave(String uuid, long playtime) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET last_seen=?, total_playtime=total_playtime+?, session_start=0 WHERE uuid=?")) {
            ps.setLong(1, System.currentTimeMillis()); ps.setLong(2, playtime); ps.setString(3, uuid);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateClientBrand(String uuid, String brand) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET client_brand=? WHERE uuid=?")) {
            ps.setString(1, brand); ps.setString(2, uuid); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateClientVersion(String uuid, String version) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET client_version=? WHERE uuid=?")) {
            ps.setString(1, version); ps.setString(2, uuid); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void addClientFlag(String uuid, String flag) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT client_flags FROM player_data WHERE uuid=?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String existing = rs.getString("client_flags");
                if (existing != null && existing.contains(flag)) return;
                String updated = (existing == null || existing.isEmpty()) ? flag : existing + "," + flag;
                try (PreparedStatement upd = connection.prepareStatement(
                        "UPDATE player_data SET client_flags=? WHERE uuid=?")) {
                    upd.setString(1, updated); upd.setString(2, uuid); upd.executeUpdate();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void clearClientFlags(String uuid) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET client_flags='' WHERE uuid=?")) {
            ps.setString(1, uuid); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateSpawnPoint(String uuid, String world, double x, double y, double z) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET spawn_world=?, spawn_x=?, spawn_y=?, spawn_z=? WHERE uuid=?")) {
            ps.setString(1, world); ps.setDouble(2, x); ps.setDouble(3, y);
            ps.setDouble(4, z); ps.setString(5, uuid); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public Map<String, Object> getPlayerData(String uuid) {
        if (!isReady()) return null;
        return queryPlayerData("SELECT * FROM player_data WHERE uuid=?", uuid);
    }

    public Map<String, Object> getPlayerDataByName(String name) {
        if (!isReady()) return null;
        return queryPlayerData("SELECT * FROM player_data WHERE LOWER(name)=LOWER(?)", name);
    }

    private Map<String, Object> queryPlayerData(String sql, String param) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", rs.getString("uuid"));
                data.put("name", rs.getString("name"));
                data.put("last_ip", rs.getString("last_ip"));
                data.put("client_brand", rs.getString("client_brand"));
                data.put("client_version", rs.getString("client_version"));
                data.put("client_flags", rs.getString("client_flags"));
                data.put("last_seen", rs.getLong("last_seen"));
                data.put("total_playtime", rs.getLong("total_playtime"));
                data.put("spawn_world", rs.getString("spawn_world"));
                data.put("spawn_x", rs.getDouble("spawn_x"));
                data.put("spawn_y", rs.getDouble("spawn_y"));
                data.put("spawn_z", rs.getDouble("spawn_z"));
                return data;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ═══════════════════════ IP HISTORY ═══════════════════════

    public void logIp(String uuid, String name, String ip) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM ip_history WHERE uuid=? AND ip=? LIMIT 1")) {
            ps.setString(1, uuid); ps.setString(2, ip);
            if (ps.executeQuery().next()) return;
        } catch (SQLException e) { e.printStackTrace(); }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ip_history (uuid, name, ip, timestamp) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid); ps.setString(2, name);
            ps.setString(3, ip); ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Map<String, String>> getAltsForIp(String ip, String excludeUuid) {
        List<Map<String, String>> alts = new ArrayList<>();
        if (!isReady()) return alts;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DISTINCT uuid, name FROM ip_history WHERE ip=? AND uuid!=? ORDER BY timestamp DESC")) {
            ps.setString(1, ip); ps.setString(2, excludeUuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> alt = new HashMap<>();
                alt.put("uuid", rs.getString("uuid")); alt.put("name", rs.getString("name"));
                alts.add(alt);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return alts;
    }

    // ═══════════════════════ REPORTS ═══════════════════════

    public void addReport(String reporterUuid, String reporterName, String world,
                          double x, double y, double z, String message) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO reports (reporter_uuid, reporter_name, reporter_world,
                reporter_x, reporter_y, reporter_z, message, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """)) {
            ps.setString(1, reporterUuid); ps.setString(2, reporterName);
            ps.setString(3, world); ps.setDouble(4, x); ps.setDouble(5, y);
            ps.setDouble(6, z); ps.setString(7, message);
            ps.setLong(8, System.currentTimeMillis()); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<ReportModel> getOpenReports() {
        List<ReportModel> list = new ArrayList<>();
        if (!isReady()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM reports WHERE status='OPEN' OR status='IN_PROGRESS' ORDER BY timestamp ASC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new ReportModel(
                        rs.getInt("id"), rs.getString("reporter_uuid"), rs.getString("reporter_name"),
                        rs.getString("reporter_world"), rs.getDouble("reporter_x"),
                        rs.getDouble("reporter_y"), rs.getDouble("reporter_z"),
                        rs.getString("message"), rs.getLong("timestamp"),
                        rs.getString("status"), rs.getString("claimed_by"), rs.getString("claimed_by_name")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void claimReport(int id, String adminUuid, String adminName) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE reports SET status='IN_PROGRESS', claimed_by=?, claimed_by_name=? WHERE id=?")) {
            ps.setString(1, adminUuid); ps.setString(2, adminName); ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void closeReport(int id) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE reports SET status='CLOSED' WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int getOpenReportCount() {
        if (!isReady()) return 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM reports WHERE status='OPEN'")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ═══════════════════════ BANS ═══════════════════════

    public void addBan(String uuid, String name, String reason, String admin, long expires) {
        if (!isReady()) return;
        deactivateBans(uuid);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO bans (player_uuid, player_name, reason, admin_name, timestamp, expires) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, uuid); ps.setString(2, name); ps.setString(3, reason);
            ps.setString(4, admin); ps.setLong(5, System.currentTimeMillis()); ps.setLong(6, expires);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deactivateBans(String uuid) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE bans SET active=0 WHERE player_uuid=?")) {
            ps.setString(1, uuid); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public BanModel getActiveBan(String uuid) {
        if (!isReady()) return null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM bans WHERE player_uuid=? AND active=1 ORDER BY timestamp DESC LIMIT 1")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long exp = rs.getLong("expires");
                if (exp != -1 && exp < System.currentTimeMillis()) { deactivateBans(uuid); return null; }
                return new BanModel(rs.getInt("id"), rs.getString("player_uuid"),
                        rs.getString("player_name"), rs.getString("reason"),
                        rs.getString("admin_name"), rs.getLong("timestamp"), exp);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<BanModel> getBanHistory(String uuid) {
        List<BanModel> list = new ArrayList<>();
        if (!isReady()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM bans WHERE player_uuid=? ORDER BY timestamp DESC LIMIT 10")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new BanModel(rs.getInt("id"), rs.getString("player_uuid"),
                    rs.getString("player_name"), rs.getString("reason"),
                    rs.getString("admin_name"), rs.getLong("timestamp"), rs.getLong("expires")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ═══════════════════════ MUTES ═══════════════════════

    public void addMute(String uuid, String name, String reason, String admin, long expires) {
        if (!isReady()) return;
        deactivateMutes(uuid);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO mutes (player_uuid, player_name, reason, admin_name, timestamp, expires) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, uuid); ps.setString(2, name); ps.setString(3, reason);
            ps.setString(4, admin); ps.setLong(5, System.currentTimeMillis()); ps.setLong(6, expires);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deactivateMutes(String uuid) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE mutes SET active=0 WHERE player_uuid=?")) {
            ps.setString(1, uuid); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public MuteModel getActiveMute(String uuid) {
        if (!isReady()) return null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM mutes WHERE player_uuid=? AND active=1 ORDER BY timestamp DESC LIMIT 1")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long exp = rs.getLong("expires");
                if (exp != -1 && exp < System.currentTimeMillis()) { deactivateMutes(uuid); return null; }
                return new MuteModel(rs.getInt("id"), rs.getString("player_uuid"),
                        rs.getString("player_name"), rs.getString("reason"),
                        rs.getString("admin_name"), rs.getLong("timestamp"), exp);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<MuteModel> getMuteHistory(String uuid) {
        List<MuteModel> list = new ArrayList<>();
        if (!isReady()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM mutes WHERE player_uuid=? ORDER BY timestamp DESC LIMIT 10")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new MuteModel(rs.getInt("id"), rs.getString("player_uuid"),
                    rs.getString("player_name"), rs.getString("reason"),
                    rs.getString("admin_name"), rs.getLong("timestamp"), rs.getLong("expires")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ═══════════════════════ WARNS ═══════════════════════

    public void addWarn(String uuid, String name, String reason, String admin, long expires) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO warns (player_uuid, player_name, reason, admin_name, timestamp, expires) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, uuid); ps.setString(2, name); ps.setString(3, reason);
            ps.setString(4, admin); ps.setLong(5, System.currentTimeMillis()); ps.setLong(6, expires);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void removeWarnById(int id) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE warns SET active=0 WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<WarnModel> getActiveWarns(String uuid) {
        List<WarnModel> list = new ArrayList<>();
        if (!isReady()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM warns WHERE player_uuid=? AND active=1 ORDER BY timestamp DESC")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long exp = rs.getLong("expires");
                if (exp != -1 && exp < System.currentTimeMillis()) {
                    removeWarnById(rs.getInt("id")); continue;
                }
                list.add(new WarnModel(rs.getInt("id"), rs.getString("player_uuid"),
                        rs.getString("player_name"), rs.getString("reason"),
                        rs.getString("admin_name"), rs.getLong("timestamp"), exp));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<WarnModel> getAllWarns(String uuid) {
        List<WarnModel> list = new ArrayList<>();
        if (!isReady()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM warns WHERE player_uuid=? ORDER BY timestamp DESC LIMIT 10")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new WarnModel(rs.getInt("id"), rs.getString("player_uuid"),
                    rs.getString("player_name"), rs.getString("reason"),
                    rs.getString("admin_name"), rs.getLong("timestamp"), rs.getLong("expires")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ═══════════════════════ STAFF NOTES ═══════════════════════

    public void addNote(String targetUuid, String targetName, String authorName, String note) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO staff_notes (target_uuid, target_name, author_name, note, timestamp) VALUES (?,?,?,?,?)")) {
            ps.setString(1, targetUuid); ps.setString(2, targetName);
            ps.setString(3, authorName); ps.setString(4, note);
            ps.setLong(5, System.currentTimeMillis()); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Map<String, Object>> getNotes(String targetUuid) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!isReady()) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM staff_notes WHERE target_uuid=? ORDER BY timestamp DESC LIMIT 10")) {
            ps.setString(1, targetUuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> n = new HashMap<>();
                n.put("id", rs.getInt("id"));
                n.put("author", rs.getString("author_name"));
                n.put("note", rs.getString("note"));
                n.put("timestamp", rs.getLong("timestamp"));
                list.add(n);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean deleteNote(int id) {
        if (!isReady()) return false;
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM staff_notes WHERE id=?")) {
            ps.setInt(1, id); return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ═══════════════════════ ORE STATS ═══════════════════════

    private long getWindowStart() {
        return (System.currentTimeMillis() / 3_600_000L) * 3_600_000L;
    }

    public void incrementOre(String uuid, String name, String oreType) {
        if (!isReady()) return;
        long window = getWindowStart();
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE ore_stats SET count=count+1 WHERE uuid=? AND ore_type=? AND window_start=?")) {
                ps.setString(1, uuid); ps.setString(2, oreType); ps.setLong(3, window);
                if (ps.executeUpdate() == 0) {
                    try (PreparedStatement ins = connection.prepareStatement(
                            "INSERT INTO ore_stats (uuid, name, ore_type, count, window_start) VALUES (?,?,?,1,?)")) {
                        ins.setString(1, uuid); ins.setString(2, name);
                        ins.setString(3, oreType); ins.setLong(4, window); ins.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void incrementBackground(String uuid, String name) {
        if (!isReady()) return;
        long window = getWindowStart();
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE ore_stats SET cobblestone_count=cobblestone_count+1 WHERE uuid=? AND ore_type='__bg__' AND window_start=?")) {
                ps.setString(1, uuid); ps.setLong(2, window);
                if (ps.executeUpdate() == 0) {
                    try (PreparedStatement ins = connection.prepareStatement(
                            "INSERT INTO ore_stats (uuid, name, ore_type, count, cobblestone_count, window_start) VALUES (?,?,'__bg__',0,1,?)")) {
                        ins.setString(1, uuid); ins.setString(2, name); ins.setLong(3, window); ins.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int getOreCountLastHour(String uuid, String oreType) {
        if (!isReady()) return 0;
        long window = getWindowStart();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count FROM ore_stats WHERE uuid=? AND ore_type=? AND window_start=?")) {
            ps.setString(1, uuid); ps.setString(2, oreType); ps.setLong(3, window);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("count");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public Map<String, Integer> getAllOreStats(String uuid) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        if (!isReady()) return stats;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ore_type, SUM(count) as total, SUM(cobblestone_count) as bg FROM ore_stats WHERE uuid=? GROUP BY ore_type")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("ore_type");
                if (type.equals("__bg__")) {
                    stats.put("__bg__", rs.getInt("bg"));
                } else {
                    stats.put(type, rs.getInt("total"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    public long getBackgroundCountLastHour(String uuid) {
        if (!isReady()) return 0;
        long window = getWindowStart();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT cobblestone_count FROM ore_stats WHERE uuid=? AND ore_type='__bg__' AND window_start=?")) {
            ps.setString(1, uuid); ps.setLong(2, window);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("cobblestone_count");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ═══════════════════════ SESSIONS ═══════════════════════

    public void logSession(String uuid, String name, long joinTime, long leaveTime) {
        if (!isReady()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO sessions (uuid, name, join_time, leave_time, duration) VALUES (?,?,?,?,?)")) {
            ps.setString(1, uuid); ps.setString(2, name);
            ps.setLong(3, joinTime); ps.setLong(4, leaveTime);
            ps.setLong(5, Math.max(0, leaveTime - joinTime)); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public boolean isReady2() { return isReady(); }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("[DB] Connection closed.");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}