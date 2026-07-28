package me.neonjava.neonorder.storage.implementation;

import me.neonjava.neonorder.utils.Log;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteConnectionManager {
    private final File dbFile;
    private Connection connection;

    public SQLiteConnectionManager(File dbFile) {
        this.dbFile = dbFile;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException ignored) {}
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=DELETE;");
                st.execute("PRAGMA synchronous=NORMAL;");
                st.execute("PRAGMA busy_timeout=10000;");
            } catch (Exception e) {
                Log.warn("Failed to set PRAGMA on SQLite connection: " + e.getMessage());
            }
        }
        return connection;
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception ignored) {}
        connection = null;
    }
}
