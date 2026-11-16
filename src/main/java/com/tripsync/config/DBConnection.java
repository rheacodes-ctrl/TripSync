
package com.tripsync.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // MySQL database connection details
    private static final String URL =
            "jdbc:mysql://localhost:3306/tripsync";

    private static final String USER = "root";

    // Fetch password from environment variable
    private static final String PASSWORD =
            System.getenv("TRIPSYNC_DB_PASSWORD");

    // Private constructor prevents unnecessary object creation
    private DBConnection() {
    }

    // Establish and return database connection
    public static Connection getConnection() throws SQLException {

        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException(
                    "Database password is not configured. " +
                    "Set TRIPSYNC_DB_PASSWORD environment variable."
            );
        }

        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (SQLException e) {
            System.err.println("Unable to connect to MySQL: " + e.getMessage());
            throw e;
        }
    }

    // Test database connectivity
    public static void main(String[] args) {

        System.out.println("Connecting to TripSync database...");

        try (Connection connection = getConnection()) {

            System.out.println("MySQL connection successful!");
            System.out.println(
                    "Connected database: " + connection.getCatalog()
            );

            System.out.println(
                    "Connection status: " +
                    !connection.isClosed()
            );

        } catch (SQLException e) {

            System.err.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}
