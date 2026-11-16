
package com.tripsync.dao;

import com.tripsync.config.DBConnection;
import com.tripsync.model.Trip;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.Traveler;
import com.tripsync.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class TripDAO {

    private final UserDAO userDAO = new UserDAO();

    // CREATE: Save a trip and its existing members
    public boolean createTrip(Trip trip) throws SQLException {

        if (trip == null) {
            throw new IllegalArgumentException("Trip cannot be null");
        }

        String sql = """
                INSERT INTO trips
                (trip_id, trip_name, description, start_date,
                 end_date, budget, admin_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, trip.getTripId());
                    statement.setString(2, trip.getTripName());
                    statement.setString(3, trip.getDescription());
                    statement.setDate(4,
                            java.sql.Date.valueOf(trip.getStartDate()));
                    statement.setDate(5,
                            java.sql.Date.valueOf(trip.getEndDate()));
                    statement.setBigDecimal(6, trip.getBudget());
                    statement.setString(7, trip.getAdmin().getUserId());

                    statement.executeUpdate();
                }

                String memberSql = """
                        INSERT INTO trip_members (trip_id, user_id)
                        VALUES (?, ?)
                        """;

                try (PreparedStatement memberStatement =
                             connection.prepareStatement(memberSql)) {

                    for (Traveler traveler : trip.getMembers()) {
                        memberStatement.setString(1, trip.getTripId());
                        memberStatement.setString(2, traveler.getUserId());
                        memberStatement.addBatch();
                    }

                    memberStatement.executeBatch();
                }

                connection.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // READ: Retrieve one trip by ID
    public Trip getTripById(String tripId) throws SQLException {

        String sql = """
                SELECT trip_id, trip_name, description, start_date,
                       end_date, budget, admin_id
                FROM trips
                WHERE trip_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                TripAdmin admin = getAdmin(result.getString("admin_id"));

                if (admin == null) {
                    throw new SQLException("Trip administrator not found");
                }

                Trip trip = new Trip(
                        result.getString("trip_id"),
                        result.getString("trip_name"),
                        result.getString("description"),
                        result.getDate("start_date").toLocalDate(),
                        result.getDate("end_date").toLocalDate(),
                        result.getBigDecimal("budget"),
                        admin
                );

                for (Traveler traveler : getTripMembers(tripId)) {
                    trip.addMember(traveler);
                }

                return trip;
            }
        }
    }

    // READ: Retrieve all trips
    public List<Trip> getAllTrips() throws SQLException {

        List<String> tripIds = new ArrayList<>();

        String sql = "SELECT trip_id FROM trips ORDER BY created_at";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                tripIds.add(result.getString("trip_id"));
            }
        }

        List<Trip> trips = new ArrayList<>();

        for (String id : tripIds) {
            Trip trip = getTripById(id);

            if (trip != null) {
                trips.add(trip);
            }
        }

        return trips;
    }

    // UPDATE: Update trip details
    public boolean updateTrip(Trip trip) throws SQLException {

        if (trip == null) {
            throw new IllegalArgumentException("Trip cannot be null");
        }

        String sql = """
                UPDATE trips
                SET trip_name = ?, description = ?,
                    start_date = ?, end_date = ?, budget = ?
                WHERE trip_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, trip.getTripName());
            statement.setString(2, trip.getDescription());
            statement.setDate(3,
                    java.sql.Date.valueOf(trip.getStartDate()));
            statement.setDate(4,
                    java.sql.Date.valueOf(trip.getEndDate()));
            statement.setBigDecimal(5, trip.getBudget());
            statement.setString(6, trip.getTripId());

            return statement.executeUpdate() == 1;
        }
    }

    // DELETE: Delete a trip and its member relationships
    public boolean deleteTrip(String tripId) throws SQLException {

        String deleteMembers =
                "DELETE FROM trip_members WHERE trip_id = ?";

        String deleteTrip =
                "DELETE FROM trips WHERE trip_id = ?";

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement =
                             connection.prepareStatement(deleteMembers)) {

                    statement.setString(1, tripId);
                    statement.executeUpdate();
                }

                int rowsDeleted;

                try (PreparedStatement statement =
                             connection.prepareStatement(deleteTrip)) {

                    statement.setString(1, tripId);
                    rowsDeleted = statement.executeUpdate();
                }

                connection.commit();
                return rowsDeleted == 1;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // ADD MEMBER: Add a traveler to a trip
    public boolean addMember(String tripId, String userId)
            throws SQLException {

        Trip trip = getTripById(tripId);

        if (trip == null) {
            throw new IllegalArgumentException("Trip not found");
        }

        User user = userDAO.getUserById(userId);

        if (!(user instanceof Traveler)) {
            throw new IllegalArgumentException(
                    "Only travelers can join a trip");
        }

        String sql = """
                INSERT INTO trip_members (trip_id, user_id)
                VALUES (?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);
            statement.setString(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    // ADD MEMBER on a shared connection. Added for the invitation
    // system: accepting an invitation must insert the trip member and
    // update the invitation status in one transaction. This overload
    // performs only the insert on the supplied connection and does not
    // touch autoCommit, so callers control the transaction. It does
    // not repeat the "traveler only" validation here; the caller is
    // responsible for that. Backward-compatible addition; the existing
    // addMember(String, String) is unchanged.
    public boolean addMember(Connection connection, String tripId,
            String userId) throws SQLException {

        if (connection == null) {
            throw new IllegalArgumentException("Connection is required.");
        }
        if (tripId == null || tripId.isBlank() || userId == null
                || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "Trip id and user id are required.");
        }

        String sql = """
                INSERT INTO trip_members (trip_id, user_id)
                VALUES (?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);
            statement.setString(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    // REMOVE MEMBER
    public boolean removeMember(String tripId, String userId)
            throws SQLException {

        String sql = """
                DELETE FROM trip_members
                WHERE trip_id = ? AND user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);
            statement.setString(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    // READ: Retrieve all members of a trip
    public List<Traveler> getTripMembers(String tripId)
            throws SQLException {

        List<Traveler> members = new ArrayList<>();

        String sql = """
                SELECT user_id
                FROM trip_members
                WHERE trip_id = ?
                ORDER BY joined_at
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    String userId = result.getString("user_id");

                    User user = userDAO.getUserById(userId);

                    if (user instanceof Traveler) {
                        Traveler traveler = (Traveler) user;
                        traveler.joinTrip(tripId);
                        members.add(traveler);
                    }
                }
            }
        }

        return members;
    }

    // Helper: Retrieve administrator
    private TripAdmin getAdmin(String adminId) throws SQLException {

        User user = userDAO.getUserById(adminId);

        if (user instanceof TripAdmin) {
            return (TripAdmin) user;
        }

        return null;
    }

    // COUNT: Get total trips
    public int getTripCount() throws SQLException {

        String sql = "SELECT COUNT(*) FROM trips";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                return result.getInt(1);
            }
        }

        return 0;
    }
}
