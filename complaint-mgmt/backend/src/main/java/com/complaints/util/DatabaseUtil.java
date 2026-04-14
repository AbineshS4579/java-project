package com.complaints.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton connection pool backed by HikariCP.
 * Configuration is loaded from db.properties on the classpath.
 */
public final class DatabaseUtil {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUtil.class);
    private static volatile HikariDataSource dataSource;

    private DatabaseUtil() {}

    public static void init() {
        if (dataSource != null) return;
        synchronized (DatabaseUtil.class) {
            if (dataSource != null) return;

            Properties props = loadProperties();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Pool tuning
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30_000);
            config.setConnectionTimeout(10_000);
            config.setMaxLifetime(600_000);
            config.setPoolName("CMS-Pool");

            // Hardened defaults
            config.addDataSourceProperty("cachePrepStmts",          "true");
            config.addDataSourceProperty("prepStmtCacheSize",       "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit",   "2048");
            config.addDataSourceProperty("useServerPrepStmts",      "true");

            dataSource = new HikariDataSource(config);
            log.info("Database connection pool initialized.");
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) init();
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool closed.");
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = DatabaseUtil.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is == null) throw new RuntimeException("db.properties not found on classpath.");
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
        return props;
    }
}
