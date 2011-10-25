/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass of all versions of the Postgres database.
 *
 * @author John Mazzitelli
 *
 */
public abstract class PostgresqlDatabaseType extends DatabaseType {
    /**
     * The vendor name for all PostgresQL databases.
     */
    public static final String VENDOR_NAME = "postgresql";

    /**
     * Returns {@link #VENDOR_NAME}.
     *
     * @see DatabaseType#getVendor()
     */
    public String getVendor() {
        return VENDOR_NAME;
    }

    public String getHibernateDialect() {
        return "org.hibernate.dialect.PostgreSQLDialect";
    }

    /**
     * @see DatabaseType#isTableNotFoundException(SQLException)
     */
    public boolean isTableNotFoundException(SQLException e) {
        return (e.getSQLState().toUpperCase().equals("42P01"));
    }

    /**
     * Assumes the sequence table is the value of the table argument followed by "_", the value of the key argument and
     * then "_seq".
     *
     * @see DatabaseType#getSequenceValue(Connection, String, String)
     */
    public int getSequenceValue(Connection conn, String table, String key) throws SQLException {
        String query = "SELECT last_value FROM " + table + "_" + key + "_seq";
        PreparedStatement selectPS = null;
        ResultSet rs = null;

        try {
            selectPS = conn.prepareStatement(query);
            rs = selectPS.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new RuntimeException(DbUtilsI18NFactory.getMsg()
                .getMsg(DbUtilsI18NResourceKeys.NOT_A_SEQUENCE, query));
        } finally {
            closeJDBCObjects(null, selectPS, rs);
        }
    }

    /**
     * @see DatabaseType#getNextSequenceValue(Connection, String, String)
     */
    public int getNextSequenceValue(Connection conn, String table, String key) throws SQLException {
        String query = "SELECT nextval('" + table + "_" + key + "_seq'::text)";

        PreparedStatement selectPS = null;
        ResultSet rs = null;

        try {
            selectPS = conn.prepareStatement(query);
            rs = selectPS.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new RuntimeException(DbUtilsI18NFactory.getMsg()
                .getMsg(DbUtilsI18NResourceKeys.NOT_A_SEQUENCE, query));
        } finally {
            closeJDBCObjects(null, selectPS, rs);
        }
    }

    /**
     * @see DatabaseType#alterColumn(Connection, String, String, String, String, String, Boolean, Boolean)
     */
    public void alterColumn(Connection conn, String table, String column, String generic_column_type,
        String default_value, String precision, Boolean nullable, Boolean reindex) throws SQLException {
        String db_column_type = null;
        List<String> sql_list = new ArrayList<String>();

        if (generic_column_type != null) {
            db_column_type = getDBTypeFromGenericType(generic_column_type);

            if (precision != null) {
                db_column_type += " (" + precision + ")";
            }

            sql_list.add("ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE " + db_column_type);
        }

        if (default_value != null) {
            sql_list.add("ALTER TABLE " + table + " ALTER COLUMN " + column + " SET DEFAULT '" + default_value + "'");
        }

        if (nullable != null) {
            if (nullable.booleanValue()) {
                sql_list.add("ALTER TABLE " + table + " ALTER " + column + " DROP NOT NULL");
            } else {
                sql_list.add("ALTER TABLE " + table + " ALTER " + column + " SET NOT NULL");
            }
        }

        executeSql(conn, sql_list);

        // now that we've altered the column, let's reindex if we were told to do so
        if ((reindex != null) && reindex.booleanValue()) {
            reindexTable(conn, table);
        }

        return;
    }

    /**
     * @see DatabaseType#reindexTable(Connection, String)
     */
    public void reindexTable(Connection conn, String table) throws SQLException {
        executeSql(conn, "REINDEX TABLE " + table);
    }

    /**
     * This is overridden for Postgres because by default (at least in our currently supported versions)
     * postgres treats '\' as an escape character in a string literal and the ESCAPE character for
     * LIKE syntax is specified as a string literal. Therefore, ESCAPE '\\' is required to
     * specify that the escape character is in fact a single backslash.
     * Note 1: The default behavior may change in a future version of postgres given that
     * the standard_conforming_strings setting may change from 'off' to 'on' out of the box.
     * Note 2: Also related, http://opensource.atlassian.com/projects/hibernate/browse/HHH-2674 for more.
     * 
     * @see DatabaseType#getEscapeCharacter()
     */
    public String getEscapeCharacter() {
        String result = System.getProperty("rhq.server.database.escape-character");

        return (null == result) ? "\\\\" : result;
    }

}