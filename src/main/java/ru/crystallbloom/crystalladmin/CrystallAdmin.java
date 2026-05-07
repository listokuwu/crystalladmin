package ru.crystallbloom.crystalladmin;

import org.bukkit.plugin.java.JavaPlugin;
import ru.crystallbloom.crystalladmin.commands.*;
import ru.crystallbloom.crystalladmin.database.DatabaseManager;
import ru.crystallbloom.crystalladmin.listeners.*;
import ru.crystallbloom.crystalladmin.managers.*;

public class CrystallAdmin extends JavaPlugin {

    private static CrystallAdmin instance;

    private DatabaseManager databaseManager;
    private LocaleManager localeManager;
    private VanishManager vanishManager;
    private FreezeManager freezeManager;
    private AdminChatManager adminChatManager;
    private PunishmentManager punishmentManager;
    private SpyManager spyManager;
    private DiscordManager discordManager;
    private PlayerDataManager playerDataManager;
    private OreMonitorManager oreMonitorManager;
    private LaggClearManager laggClearManager;
    private ClientDetectionManager clientDetectionManager;
    private SSManager ssManager;
    private StaffNotesManager staffNotesManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Save language files if they don't exist
        saveResource("messages_ru.yml", false);
        saveResource("messages_en.yml", false);

        // Init database first
        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        // Init locale manager (depends on config)
        localeManager = new LocaleManager(this);

        // Init other managers
        discordManager      = new DiscordManager(this);
        playerDataManager   = new PlayerDataManager(this);
        vanishManager       = new VanishManager(this);
        freezeManager       = new FreezeManager(this);
        adminChatManager    = new AdminChatManager(this);
        punishmentManager   = new PunishmentManager(this);
        spyManager          = new SpyManager(this);
        oreMonitorManager   = new OreMonitorManager(this);
        laggClearManager    = new LaggClearManager(this);
        clientDetectionManager = new ClientDetectionManager(this);
        ssManager           = new SSManager(this);
        staffNotesManager   = new StaffNotesManager(this);

        // Register commands
        AdmCommand admCmd = new AdmCommand(this);
        getCommand("adm").setExecutor(admCmd);
        getCommand("adm").setTabCompleter(admCmd);
        getCommand("report").setExecutor(new ReportCommand(this));

        // Standalone commands
        StandaloneCommands standalone = new StandaloneCommands(this);
        getCommand("checkplayer").setExecutor(standalone);
        getCommand("checkore").setExecutor(standalone);
        getCommand("screenshare").setExecutor(standalone);
        getCommand("staffnote").setExecutor(standalone);
        getCommand("invsee").setExecutor(standalone);
        getCommand("enderchest").setExecutor(standalone);
        getCommand("adminchat").setExecutor(standalone);

        // Punishment commands
        PunishmentCommands punCmd = new PunishmentCommands(this);
        for (String cmd : new String[]{"ban", "tempban", "unban", "mute", "tempmute", "unmute", "warn", "unwarn", "kick"}) {
            getCommand(cmd).setExecutor(punCmd);
            getCommand(cmd).setTabCompleter(punCmd);
        }

        // Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new FreezeListener(this), this);
        pm.registerEvents(new MuteListener(this), this);
        pm.registerEvents(new VanishListener(this), this);
        pm.registerEvents(new BrandListener(this), this);
        pm.registerEvents(new OreListener(this), this);
        pm.registerEvents(new ClientDetectionListener(this), this);

        laggClearManager.start();

        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║   CrystallAdmin v2.0 Enabled    ║");
        getLogger().info("║   Language: " + localeManager.getCurrentLanguage().toUpperCase() + "                     ║");
        getLogger().info("╚══════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        ssManager.cancelAll();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("CrystallAdmin disabled.");
    }

    public static CrystallAdmin getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public VanishManager getVanishManager() { return vanishManager; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public AdminChatManager getAdminChatManager() { return adminChatManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public SpyManager getSpyManager() { return spyManager; }
    public DiscordManager getDiscordManager() { return discordManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public OreMonitorManager getOreMonitorManager() { return oreMonitorManager; }
    public LaggClearManager getLaggClearManager() { return laggClearManager; }
    public ClientDetectionManager getClientDetectionManager() { return clientDetectionManager; }
    public SSManager getSsManager() { return ssManager; }
    public StaffNotesManager getStaffNotesManager() { return staffNotesManager; }
}