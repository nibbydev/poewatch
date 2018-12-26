package poe.db.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;

import java.sql.*;

public class Flag {
    private static Logger logger = LoggerFactory.getLogger(Flag.class);
    private Database database;

    public Flag(Database database) {
        this.database = database;
    }

    /**
     * Updates totals for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean updateCounters() {
        String query1 = "UPDATE league_items AS i " +
                        "JOIN ( " +
                        "  SELECT id_l, id_d, COUNT(*) AS total " +
                        "  FROM league_entries " +
                        "  WHERE time > date_sub(now(), interval 60 second) " +
                        "  GROUP BY id_l, id_d" +
                        ") AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                        "SET i.total = i.total + e.total, i.inc = i.inc + e.total ";

        return database.executeUpdateQueries(query1);
    }

    public boolean updateOutliers() {
        String query  =
                "update league_entries as e4 " +
                "join ( " +
                "  select e1.id_l, e1.id_d, " +
                "    median(e1.price) / 1.5 as minMed, " +
                "    median(e1.price) * 1.2 as maxMed " +
                "  from league_entries as e1 " +
                "  join ( " +
                "    select distinct id_l, id_d " +
                "    from league_entries " +
                "    where time > date_sub(now(), interval 60 second) " +
                "  ) as e2 on e2.id_l = e1.id_l and e2.id_d = e1.id_d " +
                "  group by e1.id_l, e1.id_d " +
                "  having median(e1.price) > 0 " +
                ") as e3 on e4.id_l = e3.id_l and e4.id_d = e3.id_d " +
                "set e4.outlier = IF(e4.price between e3.minMed and e3.maxMed, 0, 1); ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Resets counters for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean resetCounters() {
        String query = "UPDATE league_items SET inc = 0; ";

        return database.executeUpdateQueries(query);
    }
}
