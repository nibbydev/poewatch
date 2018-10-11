package poe.manager.admin;

import poe.Config;
import poe.db.Database;

import java.util.ArrayList;

public class AdminSuite {
    private final ArrayList<LogMessage> log = new ArrayList<>(Config.admin_logSize);
    private String lastChangeId = null;
    private Database database;

    public AdminSuite(Database database) {
        this.database=database;
    }

    /**
     * Updates the change id entry in the database
     *
     * @param newChangeId The latest ChangeId string
     */
    public void setChangeID(String newChangeId) {
        if (lastChangeId == null || !lastChangeId.equals(newChangeId)) {
            lastChangeId = newChangeId;
            database.updateChangeID(newChangeId);
        }
    }
}
