
package com.tripsync.dao;

import com.tripsync.config.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PaymentDAO {

    // ==========================================
    // 1. RECORD PAYMENT
    // ==========================================

    public String recordPayment(
            String tripId,
            String fromUserId,
            String toUserId,
            BigDecimal amount,
            String status) throws SQLException {

        if (tripId == null || tripId.isBlank()
                || fromUserId == null || fromUserId.isBlank()
                || toUserId == null || toUserId.isBlank()) {
            throw new IllegalArgumentException(
                "Trip ID and both traveler IDs are required."
            );
        }

        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException(
                "A traveler cannot pay themselves."
            );
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Payment amount must be positive."
            );
        }

        String normalizedStatus = normalizeStatus(status);
        String paymentId = UUID.randomUUID().toString();

        String sql = """
                INSERT INTO payments
                (payment_id, trip_id, from_user_id, to_user_id,
                 amount, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, paymentId);
            statement.setString(2, tripId);
            statement.setString(3, fromUserId);
            statement.setString(4, toUserId);
            statement.setBigDecimal(5, amount);
            statement.setString(6, normalizedStatus);

            statement.executeUpdate();
            return paymentId;
        }
    }

    // Convenience method: create a completed payment
    public String recordPayment(
            String tripId,
            String fromUserId,
            String toUserId,
            BigDecimal amount) throws SQLException {

        return recordPayment(
            tripId, fromUserId, toUserId, amount, "COMPLETED"
        );
    }

    // ==========================================
    // 2. GET PAYMENT BY ID
    // ==========================================

    public Map<String, Object> getPaymentById(String paymentId)
            throws SQLException {

        String sql = """
                SELECT payment_id, trip_id, from_user_id,
                       to_user_id, amount, payment_date, status
                FROM payments
                WHERE payment_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, paymentId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapPayment(result);
                }
            }
        }

        return null;
    }

    // ==========================================
    // 3. GET PAYMENTS BY TRIP
    // ==========================================

    public List<Map<String, Object>> getPaymentsByTripId(String tripId)
            throws SQLException {

        List<Map<String, Object>> payments = new ArrayList<>();

        String sql = """
                SELECT payment_id, trip_id, from_user_id,
                       to_user_id, amount, payment_date, status
                FROM payments
                WHERE trip_id = ?
                ORDER BY payment_date, payment_id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    payments.add(mapPayment(result));
                }
            }
        }

        return payments;
    }

    // ==========================================
    // 4. GET ALL PAYMENTS
    // ==========================================

    public List<Map<String, Object>> getAllPayments()
            throws SQLException {

        List<Map<String, Object>> payments = new ArrayList<>();

        String sql = """
                SELECT payment_id, trip_id, from_user_id,
                       to_user_id, amount, payment_date, status
                FROM payments
                ORDER BY payment_date, payment_id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                payments.add(mapPayment(result));
            }
        }

        return payments;
    }

    // ==========================================
    // 5. UPDATE PAYMENT STATUS
    // ==========================================

    public boolean updatePaymentStatus(
            String paymentId,
            String status) throws SQLException {

        String normalizedStatus = normalizeStatus(status);

        String sql = """
                UPDATE payments
                SET status = ?
                WHERE payment_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, normalizedStatus);
            statement.setString(2, paymentId);

            return statement.executeUpdate() == 1;
        }
    }

    // ==========================================
    // 6. DELETE PAYMENT
    // ==========================================

    public boolean deletePayment(String paymentId) throws SQLException {

        String sql = """
                DELETE FROM payments
                WHERE payment_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, paymentId);
            return statement.executeUpdate() == 1;
        }
    }

    // ==========================================
    // 7. TOTAL COMPLETED PAYMENTS FOR A TRIP
    // ==========================================

    public BigDecimal getTotalPayments(String tripId)
            throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                WHERE trip_id = ?
                AND status = 'COMPLETED'
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBigDecimal(1);
            }
        }
    }

    // ==========================================
    // 8. CALCULATE OUTSTANDING BALANCES
    // Positive = should receive
    // Negative = owes money
    // ==========================================

    public Map<String, BigDecimal> getOutstandingBalances(String tripId)
            throws SQLException {

        Map<String, BigDecimal> balances = new LinkedHashMap<>();

        String sql = """
                SELECT user_id, SUM(net_amount) AS balance
                FROM (
                    SELECT paid_by AS user_id, amount AS net_amount
                    FROM expenses
                    WHERE trip_id = ?

                    UNION ALL

                    SELECT es.user_id, -es.share_amount AS net_amount
                    FROM expense_splits es
                    INNER JOIN expenses e
                        ON es.expense_id = e.expense_id
                    WHERE e.trip_id = ?

                    UNION ALL

                    SELECT from_user_id AS user_id, amount AS net_amount
                    FROM payments
                    WHERE trip_id = ?
                    AND status = 'COMPLETED'

                    UNION ALL

                    SELECT to_user_id AS user_id, -amount AS net_amount
                    FROM payments
                    WHERE trip_id = ?
                    AND status = 'COMPLETED'
                ) AS transactions
                GROUP BY user_id
                ORDER BY user_id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tripId);
            statement.setString(2, tripId);
            statement.setString(3, tripId);
            statement.setString(4, tripId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    balances.put(
                        result.getString("user_id"),
                        result.getBigDecimal("balance")
                    );
                }
            }
        }

        return balances;
    }

    // ==========================================
    // 9. BALANCE FOR ONE TRAVELER
    // ==========================================

    public BigDecimal getOutstandingBalance(
            String tripId,
            String userId) throws SQLException {

        return getOutstandingBalances(tripId).getOrDefault(
            userId, BigDecimal.ZERO
        );
    }

    // ==========================================
    // 10. PAYMENT COUNT
    // ==========================================

    public int getPaymentCount(String tripId) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM payments
                WHERE trip_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    // ==========================================
    // 11. MAP DATABASE RECORD
    // ==========================================

    private Map<String, Object> mapPayment(ResultSet result)
            throws SQLException {

        Map<String, Object> payment = new LinkedHashMap<>();

        payment.put("paymentId", result.getString("payment_id"));
        payment.put("tripId", result.getString("trip_id"));
        payment.put("fromUserId", result.getString("from_user_id"));
        payment.put("toUserId", result.getString("to_user_id"));
        payment.put("amount", result.getBigDecimal("amount"));

        Timestamp paymentDate = result.getTimestamp("payment_date");
        payment.put("paymentDate", paymentDate);

        payment.put("status", result.getString("status"));

        return payment;
    }

    // ==========================================
    // 12. VALIDATE PAYMENT STATUS
    // ==========================================

    private String normalizeStatus(String status) {

        if (status == null) {
            throw new IllegalArgumentException(
                "Payment status is required."
            );
        }

        String normalized = status.trim().toUpperCase();

        if (!normalized.equals("PENDING")
                && !normalized.equals("COMPLETED")
                && !normalized.equals("CANCELLED")) {
            throw new IllegalArgumentException(
                "Invalid payment status: " + status
            );
        }

        return normalized;
    }
}
