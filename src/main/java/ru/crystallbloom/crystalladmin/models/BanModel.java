package ru.crystallbloom.crystalladmin.models;

public class BanModel {
    private final int id;
    private final String playerUuid;
    private final String playerName;
    private final String reason;
    private final String adminName;
    private final long timestamp;
    private final long expires;

    public BanModel(int id, String playerUuid, String playerName, String reason,
                    String adminName, long timestamp, long expires) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.reason = reason;
        this.adminName = adminName;
        this.timestamp = timestamp;
        this.expires = expires;
    }

    public int getId() { return id; }
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getReason() { return reason; }
    public String getAdminName() { return adminName; }
    public long getTimestamp() { return timestamp; }
    public long getExpires() { return expires; }
    public boolean isPermanent() { return expires == -1; }
    public boolean isExpired() { return !isPermanent() && expires < System.currentTimeMillis(); }
}