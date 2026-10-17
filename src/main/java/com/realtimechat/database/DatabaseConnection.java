package com.realtimechat.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages JDBC connections to MySQL.
 * Reads database credentials securely from 'db.properties' (ignored by Git).
 */
public class DatabaseConnection {

    private static final String PROPERTIES_FILE = "db.properties";
    private static String url;
    private static String user;
    private static String password;
    private static String driver;

    static {
        loadProperties();
    }

    /**
     * Loads database configuration properties from classpath or project directory.
     */
    private static void loadProperties() {
        Properties props = new Properties();
        InputStream inStream = null;

        try {
            // First attempt: Load from classpath (src/main/resources)
            inStream = DatabaseConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);

            // Second attempt fallback: Load directly from file system
            if (inStream == null) {
                java.io.File localFile = new java.io.File("src/main/resources/" + PROPERTIES_FILE);
                if (localFile.exists()) {
                    inStream = new FileInputStream(localFile);
                }
            }

            if (inStream != null) {
                props.load(inStream);
                driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
                url = props.getProperty("db.url");
                user = props.getProperty("db.user");
                password = props.getProperty("db.password");
            } else {
                driver = "com.mysql.cj.jdbc.Driver";
            }

            // Environment variable overrides for Cloud/Docker deployment
            String envUrl = System.getenv("DB_URL");
            if (envUrl != null && !envUrl.isBlank()) {
                url = envUrl;
            }
            String envUser = System.getenv("DB_USER");
            if (envUser != null && !envUser.isBlank()) {
                user = envUser;
            }
            String envPass = System.getenv("DB_PASSWORD");
            if (envPass != null) {
                password = envPass;
            }

            // Explicitly load the MySQL driver class
            Class.forName(driver);
            System.out.println("[DatabaseConnection] JDBC Driver registered: " + driver);

        } catch (IOException e) {
            System.err.println("[DatabaseConnection] Warning reading " + PROPERTIES_FILE + ": " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] CRITICAL: MySQL Driver class not found: " + driver);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Obtains a new JDBC Connection to the MySQL database.
     *
     * @return active java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (url == null) {
            throw new SQLException("Database URL is not configured. Check your db.properties file.");
        }
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Utility method to verify MySQL database connectivity.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean valid = conn != null && !conn.isClosed();
            if (valid) {
                System.out.println("[DatabaseConnection] Connection test successful! Database is online.");
            }
            return valid;
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
