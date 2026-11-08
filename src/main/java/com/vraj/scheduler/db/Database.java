package com.vraj.scheduler.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hands out JDBC connections and creates the schema on startup.
 *
 * <p>H2 closes an embedded database when its last connection closes, so one connection stays open
 * for the lifetime of this object. That lets each DAO call use its own short-lived connection
 * without reopening the database every time.
 */
public class Database implements AutoCloseable {
    public static final String DEFAULT_URL = "jdbc:h2:./data/scheduler";

    private final String url;
    private final Connection keepAlive;

    public Database() throws SQLException {
        this(DEFAULT_URL);
    }

    public Database(String url) throws SQLException {
        this.url = url;
        this.keepAlive = DriverManager.getConnection(url);
        try (Statement statement = keepAlive.createStatement()) {
            statement.execute("RUNSCRIPT FROM 'classpath:/schema.sql'");
        } catch (SQLException e) {
            keepAlive.close();
            throw e;
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    public void close() throws SQLException {
        keepAlive.close();
    }
}
