package com.realtimechat.database;

/**
 * Diagnostic runner to verify MySQL connectivity and database readiness.
 * Run this class directly in IntelliJ to test your database configuration.
 */
public class DatabaseTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   Testing MySQL Database Connectivity");
        System.out.println("==================================================");

        boolean isConnected = DatabaseConnection.testConnection();

        if (isConnected) {
            System.out.println("\nSUCCESS: Successfully connected to the MySQL database!");
            System.out.println("The JDBC driver, connection string, and credentials are valid.");
        } else {
            System.out.println("\nFAILED: Could not connect to MySQL.");
            System.out.println("Please check:");
            System.out.println("1. Is your MySQL server running? (e.g., MySQL Workbench, XAMPP, or Windows Services)");
            System.out.println("2. Did you run 'src/main/resources/schema.sql' to create the 'realtime_chat' database?");
            System.out.println("3. Are the credentials in 'src/main/resources/db.properties' correct?");
        }
    }
}
