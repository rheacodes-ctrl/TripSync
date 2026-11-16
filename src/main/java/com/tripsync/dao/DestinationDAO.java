
package com.tripsync.dao;

import com.tripsync.config.DBConnection;
import com.tripsync.model.Destination;
import com.tripsync.model.Trip;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DestinationDAO {

    // CREATE DESTINATION
    public boolean createDestination(String tripId, Destination destination)
            throws SQLException {

        if (tripId == null || tripId.isBlank() || destination == null) {
            throw new IllegalArgumentException("Invalid trip or destination");
        }

        // Verify that the destination dates fall within the trip dates.
        Trip trip = new TripDAO().getTripById(tripId);

        if (trip == null) {
            throw new IllegalArgumentException("Trip does not exist");
        }

        trip.addDestination(destination);

        String sql = """
                INSERT INTO destinations
                (destination_id, trip_id, name, city, country,
                 description, arrival_date, departure_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, destination.getDestinationId());
            stmt.setString(2, tripId);
            stmt.setString(3, destination.getName());
            stmt.setString(4, destination.getCity());
            stmt.setString(5, destination.getCountry());
            stmt.setString(6, destination.getDescription());
            stmt.setDate(7, Date.valueOf(destination.getArrivalDate()));
            stmt.setDate(8, Date.valueOf(destination.getDepartureDate()));

            return stmt.executeUpdate() > 0;
        }
    }

    // READ DESTINATION BY ID
    public Destination getDestinationById(String destinationId)
            throws SQLException {

        String sql = "SELECT * FROM destinations WHERE destination_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, destinationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapDestination(rs);
                }
            }
        }

        return null;
    }

    // READ ALL DESTINATIONS OF A TRIP
    public List<Destination> getDestinationsByTripId(String tripId)
            throws SQLException {

        List<Destination> destinations = new ArrayList<>();

        String sql = """
                SELECT * FROM destinations
                WHERE trip_id = ?
                ORDER BY arrival_date
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tripId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    destinations.add(mapDestination(rs));
                }
            }
        }

        return destinations;
    }

    // UPDATE DESTINATION
    public boolean updateDestination(Destination destination)
            throws SQLException {

        if (destination == null) {
            throw new IllegalArgumentException("Destination cannot be null");
        }

        String sql = """
                UPDATE destinations
                SET name = ?,
                    city = ?,
                    country = ?,
                    description = ?,
                    arrival_date = ?,
                    departure_date = ?
                WHERE destination_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, destination.getName());
            stmt.setString(2, destination.getCity());
            stmt.setString(3, destination.getCountry());
            stmt.setString(4, destination.getDescription());
            stmt.setDate(5, Date.valueOf(destination.getArrivalDate()));
            stmt.setDate(6, Date.valueOf(destination.getDepartureDate()));
            stmt.setString(7, destination.getDestinationId());

            return stmt.executeUpdate() > 0;
        }
    }

    // DELETE DESTINATION
    public boolean deleteDestination(String destinationId)
            throws SQLException {

        String sql = "DELETE FROM destinations WHERE destination_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, destinationId);

            return stmt.executeUpdate() > 0;
        }
    }

    // COUNT DESTINATIONS FOR A TRIP
    public int getDestinationCount(String tripId) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM destinations
                WHERE trip_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tripId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    // CONVERT DATABASE ROW INTO JAVA OBJECT
    private Destination mapDestination(ResultSet rs) throws SQLException {

        return new Destination(
                rs.getString("destination_id"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("description"),
                rs.getDate("arrival_date").toLocalDate(),
                rs.getDate("departure_date").toLocalDate()
        );
    }
}
