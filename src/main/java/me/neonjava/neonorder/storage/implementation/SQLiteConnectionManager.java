package me.neonjava.neonorder.storage.implementation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.neonjava.neonorder.utils.Log;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite connections via a HikariCP pool so that concurrent async threads
 * each get their own connection, preventing the "money taken but order not placed"
 * bug caused by sharing a single non-thread-safe Connection.
 */
public class SQLiteConnectionManager {

    private final HikariDataSource pool;

    public SQLiteConnectionManager(File dbFile) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("neonorder-sqlite-pool");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(4);        // SQLite WAL supports a handful of writers safely
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000); // 10 s
        config.setIdleTimeout(60_000);
        config.setMaxLifetime(300_000);

        // Run PRAGMAs on every new connection via connectionInitSql
        config.setConnectionInitSql(
                "PRAGMA journal_mode=WAL; " +
                "PRAGMA synchronous=NORMAL; " +
                "PRAGMA busy_timeout=10000;"
        );

        this.pool = new HikariDataSource(config);
        Log.info("SQLite connection pool initialised (WAL mode).");
    }

    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    public synchronized void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}
