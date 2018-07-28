package com.poestats.account;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountRelation {
    public String oldAccountName, newAccountName;
    public long oldAccountId, newAccountId;
    public int matches;

    public int statusCode, moved;

    public void load (ResultSet resultSet) throws SQLException {
        oldAccountName = resultSet.getString("oldAccountName");
        newAccountName = resultSet.getString("newAccountName");
        oldAccountId = resultSet.getLong("oldAccountId");
        newAccountId = resultSet.getLong("newAccountId");
        matches = resultSet.getInt("matches");
    }
}
