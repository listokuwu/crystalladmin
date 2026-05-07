package ru.crystallbloom.crystalladmin.models;

public class ReportModel {
    private final int id;
    private final String reporterUuid;
    private final String reporterName;
    private final String reporterWorld;
    private final double reporterX, reporterY, reporterZ;
    private final String message;
    private final long timestamp;
    private final String status;
    private final String claimedBy;
    private final String claimedByName;

    public ReportModel(int id, String reporterUuid, String reporterName,
                       String reporterWorld, double reporterX, double reporterY, double reporterZ,
                       String message, long timestamp, String status,
                       String claimedBy, String claimedByName) {
        this.id = id;
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.reporterWorld = reporterWorld;
        this.reporterX = reporterX;
        this.reporterY = reporterY;
        this.reporterZ = reporterZ;
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
        this.claimedBy = claimedBy;
        this.claimedByName = claimedByName;
    }

    public int getId() { return id; }
    public String getReporterUuid() { return reporterUuid; }
    public String getReporterName() { return reporterName; }
    public String getReporterWorld() { return reporterWorld; }
    public double getReporterX() { return reporterX; }
    public double getReporterY() { return reporterY; }
    public double getReporterZ() { return reporterZ; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getClaimedBy() { return claimedBy; }
    public String getClaimedByName() { return claimedByName; }
    public boolean isInProgress() { return "IN_PROGRESS".equals(status); }
    public boolean hasLocation() { return reporterWorld != null && !reporterWorld.isEmpty(); }
}