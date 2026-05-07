package ru.crystallbloom.crystalladmin.managers;

import ru.crystallbloom.crystalladmin.CrystallAdmin;

import java.util.List;
import java.util.Map;

public class StaffNotesManager {

    private final CrystallAdmin plugin;

    public StaffNotesManager(CrystallAdmin plugin) {
        this.plugin = plugin;
    }

    public void addNote(String targetUuid, String targetName, String authorName, String note) {
        plugin.getDatabaseManager().addNote(targetUuid, targetName, authorName, note);
    }

    public List<Map<String, Object>> getNotes(String targetUuid) {
        return plugin.getDatabaseManager().getNotes(targetUuid);
    }

    public boolean deleteNote(int id) {
        return plugin.getDatabaseManager().deleteNote(id);
    }
}