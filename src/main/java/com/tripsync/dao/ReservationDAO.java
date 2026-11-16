
package com.tripsync.dao;

import com.tripsync.config.DBConnection;
import com.tripsync.reservation.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReservationDAO {

    public boolean addReservation(Reservation reservation)
            throws SQLException {

        String sql = """
                INSERT INTO reservations
                (reservation_id, trip_id, reservation_type,
                 provider_name, cost, reservation_date, status, details)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reservation.getReservationId());
            ps.setString(2, reservation.getTripId());
            ps.setString(3, getDatabaseType(reservation));
            ps.setString(4, reservation.getProviderName());
            ps.setBigDecimal(5, reservation.getCost());
            ps.setDate(6, Date.valueOf(reservation.getReservationDate()));
            ps.setString(7, reservation.getStatus().name());
            ps.setString(8, buildDetails(reservation));

            return ps.executeUpdate() == 1;
        }
    }

    public Reservation getReservationById(String reservationId)
            throws SQLException {

        String sql =
                "SELECT * FROM reservations WHERE reservation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }

        return null;
    }

    public List<Reservation> getReservationsByTripId(String tripId)
            throws SQLException {

        List<Reservation> reservations = new ArrayList<>();

        String sql = """
                SELECT * FROM reservations
                WHERE trip_id = ?
                ORDER BY reservation_date
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tripId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> getAllReservations()
            throws SQLException {

        List<Reservation> reservations = new ArrayList<>();

        String sql =
                "SELECT * FROM reservations ORDER BY reservation_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }

        return reservations;
    }

    public boolean updateReservationStatus(
            String reservationId, Reservation.Status status)
            throws SQLException {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Status cannot be null.");
        }

        String sql = """
                UPDATE reservations SET status = ?
                WHERE reservation_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, reservationId);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateReservationCost(
            String reservationId, BigDecimal newCost)
            throws SQLException {

        if (newCost == null ||
                newCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Invalid reservation cost.");
        }

        String sql = """
                UPDATE reservations SET cost = ?
                WHERE reservation_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, newCost);
            ps.setString(2, reservationId);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteReservation(String reservationId)
            throws SQLException {

        String sql =
                "DELETE FROM reservations WHERE reservation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reservationId);
            return ps.executeUpdate() == 1;
        }
    }

    public int getReservationCount(String tripId)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM reservations WHERE trip_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tripId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public BigDecimal getTotalReservationCost(String tripId)
            throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(cost), 0)
                FROM reservations
                WHERE trip_id = ?
                  AND (status IS NULL OR status <> 'CANCELLED')
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tripId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    private String getDatabaseType(Reservation reservation) {

        if (reservation instanceof HotelReservation) {
            return "HOTEL";
        }

        if (reservation instanceof TransportReservation) {
            return "TRANSPORT";
        }

        if (reservation instanceof ActivityReservation) {
            return "ACTIVITY";
        }

        throw new IllegalArgumentException(
                "Unsupported reservation type.");
    }

    private String buildDetails(Reservation reservation) {

        if (reservation instanceof HotelReservation hotel) {
            return "{"
                    + "\"checkInDate\":\"" + hotel.getCheckInDate() + "\","
                    + "\"checkOutDate\":\"" + hotel.getCheckOutDate() + "\","
                    + "\"numberOfRooms\":" + hotel.getNumberOfRooms()
                    + "}";
        }

        if (reservation instanceof TransportReservation transport) {
            return "{"
                    + "\"transportType\":\"" + transport.getTransportType() + "\","
                    + "\"departureLocation\":\""
                    + jsonEscape(transport.getDepartureLocation()) + "\","
                    + "\"arrivalLocation\":\""
                    + jsonEscape(transport.getArrivalLocation()) + "\","
                    + "\"departureTime\":\"" + transport.getDepartureTime() + "\","
                    + "\"bookingReference\":\""
                    + jsonEscape(transport.getBookingReference() == null
                    ? "" : transport.getBookingReference()) + "\""
                    + "}";
        }

        if (reservation instanceof ActivityReservation activity) {
            return "{"
                    + "\"location\":\""
                    + jsonEscape(activity.getLocation()) + "\","
                    + "\"activityTime\":\"" + activity.getActivityTime() + "\","
                    + "\"numberOfParticipants\":"
                    + activity.getNumberOfParticipants()
                    + "}";
        }

        throw new IllegalArgumentException(
                "Unsupported reservation type.");
    }

    private Reservation mapReservation(ResultSet rs)
            throws SQLException {

        String id = rs.getString("reservation_id");
        String tripId = rs.getString("trip_id");
        String type = rs.getString("reservation_type");
        String provider = rs.getString("provider_name");
        BigDecimal cost = rs.getBigDecimal("cost");

        LocalDate date = rs.getDate("reservation_date").toLocalDate();

        String statusValue = rs.getString("status");

        Reservation.Status status =
                statusValue == null
                        ? Reservation.Status.PENDING
                        : Reservation.Status.valueOf(statusValue);

        String details = rs.getString("details");

        // Older seeded records may have NULL details.
        // Treat them as records with unavailable optional information.
        if (details == null || details.isBlank()) {
            details = "{}";
        }

        switch (type) {

            case "HOTEL": {
                LocalDate checkIn = parseDate(
                        jsonStringOrDefault(
                                details, "checkInDate", date.toString()),
                        date);

                LocalDate checkOut = parseDate(
                        jsonStringOrDefault(
                                details, "checkOutDate",
                                date.plusDays(1).toString()),
                        date.plusDays(1));

                int rooms = jsonIntOrDefault(
                        details, "numberOfRooms", 1);

                return new HotelReservation(
                        id, tripId, provider, cost, date, status,
                        checkIn, checkOut, rooms);
            }

            case "TRANSPORT": {
                String transportValue = jsonStringOrDefault(
                        details, "transportType", "FLIGHT");

                TransportReservation.TransportType transportType;

                try {
                    transportType =
                            TransportReservation.TransportType.valueOf(
                                    transportValue);
                } catch (IllegalArgumentException e) {
                    transportType =
                            TransportReservation.TransportType.FLIGHT;
                }

                String departure = jsonStringOrDefault(
                        details, "departureLocation", "Not available");

                String arrival = jsonStringOrDefault(
                        details, "arrivalLocation", "Not available");

                LocalTime departureTime = parseTime(
                        jsonStringOrDefault(
                                details, "departureTime", "00:00"),
                        LocalTime.MIDNIGHT);

                String bookingReference = emptyToNull(
                        jsonStringOrDefault(
                                details, "bookingReference", ""));

                return new TransportReservation(
                        id, tripId, provider, cost, date, status,
                        transportType, departure, arrival,
                        departureTime, bookingReference);
            }

            case "ACTIVITY": {
                String location = jsonStringOrDefault(
                        details, "location", "Not available");

                LocalTime activityTime = parseTime(
                        jsonStringOrDefault(
                                details, "activityTime", "00:00"),
                        LocalTime.MIDNIGHT);

                int participants = jsonIntOrDefault(
                        details, "numberOfParticipants", 1);

                return new ActivityReservation(
                        id, tripId, provider, cost, date, status,
                        location, activityTime, participants);
            }

            default:
                throw new SQLException(
                        "Unknown reservation type: " + type);
        }
    }

    private String jsonEscape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String jsonStringOrDefault(
            String json, String key, String defaultValue) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key)
                        + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            return defaultValue;
        }

        return jsonUnescape(matcher.group(1));
    }

    private int jsonIntOrDefault(
            String json, String key, int defaultValue) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {

        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalTime parseTime(String value, LocalTime fallback) {

        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String jsonUnescape(String value) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {

            char ch = value.charAt(i);

            if (ch == '\\' && i + 1 < value.length()) {

                char next = value.charAt(++i);

                switch (next) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'u':
                        if (i + 4 < value.length()) {
                            String hex = value.substring(i + 1, i + 5);
                            result.append((char)
                                    Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default:
                        result.append(next);
                }

            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
