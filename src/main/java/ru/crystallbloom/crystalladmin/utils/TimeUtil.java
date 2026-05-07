package ru.crystallbloom.crystalladmin.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    /**
     * Parse duration string like 30m, 2h, 7d, 1mo into milliseconds.
     * Returns -1 for permanent.
     */
    public static long parseDuration(String input) {
        if (input == null || input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent")) {
            return -1L;
        }
        try {
            long multiplier;
            String lower = input.toLowerCase();
            if (lower.endsWith("mo")) {
                multiplier = 30L * 24 * 60 * 60 * 1000;
                return Long.parseLong(lower.replace("mo", "")) * multiplier;
            } else if (lower.endsWith("d")) {
                multiplier = 24L * 60 * 60 * 1000;
                return Long.parseLong(lower.replace("d", "")) * multiplier;
            } else if (lower.endsWith("h")) {
                multiplier = 60L * 60 * 1000;
                return Long.parseLong(lower.replace("h", "")) * multiplier;
            } else if (lower.endsWith("m")) {
                multiplier = 60L * 1000;
                return Long.parseLong(lower.replace("m", "")) * multiplier;
            } else if (lower.endsWith("s")) {
                multiplier = 1000L;
                return Long.parseLong(lower.replace("s", "")) * multiplier;
            }
        } catch (NumberFormatException ignored) {}
        return -2L; // invalid
    }

    /**
     * Format a timestamp to readable date string.
     */
    public static String formatDate(long timestamp) {
        if (timestamp <= 0) return "Никогда";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format duration in ms to readable string.
     */
    public static String formatDuration(long ms) {
        if (ms <= 0) return "0с";
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "д " + (hours % 24) + "ч " + (minutes % 60) + "м";
        if (hours > 0) return hours + "ч " + (minutes % 60) + "м";
        if (minutes > 0) return minutes + "м " + (seconds % 60) + "с";
        return seconds + "с";
    }

    /**
     * Format absolute expiry timestamp to readable string.
     */
    public static String formatExpiry(long expires) {
        if (expires == -1) return "Навсегда";
        return formatDate(expires);
    }

    /**
     * Format remaining time from now until expires.
     */
    public static String formatRemaining(long expires) {
        if (expires == -1) return "Навсегда";
        long remaining = expires - System.currentTimeMillis();
        if (remaining <= 0) return "Истёк";
        return formatDuration(remaining);
    }

    /**
     * Format total playtime milliseconds to human readable
     */
    public static String formatPlaytime(long ms) {
        if (ms <= 0) return "0м";
        long minutes = ms / 60000;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) return days + "д " + (hours % 24) + "ч";
        if (hours > 0) return hours + "ч " + (minutes % 60) + "м";
        return minutes + "м";
    }
}