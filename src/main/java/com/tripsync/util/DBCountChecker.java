package com.tripsync.util;

import com.tripsync.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Read-only helper that prints the row count of every TripSync table.
 *
 * It uses the existing DBConnection (and the TRIPSYNC_DB_PASSWORD
 * environment variable), so it needs no credentials of its own.
 * Run it before and after Main to confirm the demo does not modify
 * the database.
 */
public class DBCountChecker {

    // The 12 TripSync tables. These names are fixed by the schema,
    // not user input, so they can be interpolated safely.
    private static final List<String> TABLES = List.of(
        "users",
        "trips",
        "trip_members",
        "destinations",
        "activities",
        "activity_participants",
        "activity_votes",
        "reservations",
        "expenses",
        "expense_splits",
        "payments",
        "documents"
    );

    public static void main(String[] args) {

        System.out.println("========== DATABASE ROW COUNTS ==========");

        try (Connection connection = DBConnection.getConnection()) {

            for (String table : TABLES) {

                String sql = "SELECT COUNT(*) FROM " + table;

                try (PreparedStatement statement =
                            connection.prepareStatement(sql);
                     ResultSet result = statement.executeQuery()) {

                    result.next();
                    System.out.println(
                        String.format("%-22s %d", table, result.getInt(1))
                    );
                }
            }

            System.out.println("========================================");

        } catch (SQLException e) {
            System.err.println("Could not read row counts: " + e.getMessage());
            System.err.println(
                "Ensure MySQL is running and the "
                + "TRIPSYNC_DB_PASSWORD environment variable is set."
            );
        }
    }
}
