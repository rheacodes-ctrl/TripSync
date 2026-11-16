
package com.tripsync.dao;

import com.tripsync.config.DBConnection;
import com.tripsync.model.User;
import com.tripsync.model.Traveler;
import com.tripsync.model.TripAdmin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // CREATE
    public boolean addUser(User user) throws SQLException {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        String sql = """
                INSERT INTO users (user_id, name, email, role)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, user.getUserId());
            statement.setString(2, user.getName());
            statement.setString(3, user.getEmail());

            String role = user instanceof TripAdmin
                    ? "TRIP_ADMIN"
                    : "TRAVELER";

            statement.setString(4, role);

            return statement.executeUpdate() == 1;
        }
    }

    // READ BY ID
    public User getUserById(String userId) throws SQLException {

        String sql = """
                SELECT user_id, name, email, role
                FROM users
                WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, userId);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return mapUser(result);
                }
            }
        }

        return null;
    }

    // READ BY EMAIL
    // Added for the invitation system so an invitee can be linked to a
    // registered user when one exists. Backward-compatible addition.
    // MySQL's default VARCHAR collation is case-insensitive, so this
    // matches an email regardless of stored case.
    public User getUserByEmail(String email) throws SQLException {

        if (email == null || email.isBlank()) {
            return null;
        }

        String sql = """
                SELECT user_id, name, email, role
                FROM users
                WHERE email = ?
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapUser(result);
                }
            }
        }

        return null;
    }

    // READ ALL
    public List<User> getAllUsers() throws SQLException {

        List<User> users = new ArrayList<>();

        String sql = """
                SELECT user_id, name, email, role
                FROM users
                ORDER BY name
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(mapUser(result));
            }
        }

        return users;
    }

    // UPDATE
    public boolean updateUser(User user) throws SQLException {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        String sql = """
                UPDATE users
                SET name = ?, email = ?
                WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getUserId());

            return statement.executeUpdate() == 1;
        }
    }

    // DELETE
    public boolean deleteUser(String userId) throws SQLException {

        String sql = """
                DELETE FROM users
                WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, userId);

            return statement.executeUpdate() == 1;
        }
    }

    // DATABASE RECORD TO JAVA OBJECT
    private User mapUser(ResultSet result) throws SQLException {

        String userId = result.getString("user_id");
        String name = result.getString("name");
        String email = result.getString("email");
        String role = result.getString("role");

        if ("TRIP_ADMIN".equalsIgnoreCase(role)) {
            return new TripAdmin(userId, name, email, true);
        }

        if ("TRAVELER".equalsIgnoreCase(role)) {
            return new Traveler(userId, name, email, true);
        }

        throw new SQLException("Unknown user role: " + role);
    }
}
