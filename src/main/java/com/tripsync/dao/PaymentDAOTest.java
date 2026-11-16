
package com.tripsync.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class PaymentDAOTest {

    public static void main(String[] args) {

        PaymentDAO paymentDAO = new PaymentDAO();

        String tripId = "T001";
        String testPaymentId = null;

        try {
            System.out.println("========== PAYMENT DAO TEST ==========");

            System.out.println("\n========== INITIAL PAYMENT HISTORY ==========");

            List<Map<String, Object>> initialPayments =
                paymentDAO.getPaymentsByTripId(tripId);

            for (Map<String, Object> payment : initialPayments) {
                System.out.println(payment);
            }

            int initialCount = paymentDAO.getPaymentCount(tripId);

            System.out.println("\nInitial payment count: " + initialCount);

            System.out.println("\n========== INITIAL BALANCES ==========");

            Map<String, BigDecimal> initialBalances =
                paymentDAO.getOutstandingBalances(tripId);

            initialBalances.forEach((userId, balance) ->
                System.out.println(userId + " : Rs. " + balance)
            );

            System.out.println("\n========== RECORD PENDING PAYMENT ==========");

            testPaymentId = paymentDAO.recordPayment(
                tripId,
                "U003",
                "U002",
                new BigDecimal("100.00"),
                "PENDING"
            );

            System.out.println("Payment inserted: " + (testPaymentId != null));
            System.out.println("Payment ID: " + testPaymentId);

            System.out.println("\n========== RETRIEVE PAYMENT ==========");

            Map<String, Object> retrieved =
                paymentDAO.getPaymentById(testPaymentId);

            System.out.println("Payment details: " + retrieved);
            System.out.println("Correct ID: "
                    + testPaymentId.equals(retrieved.get("paymentId")));

            System.out.println("\n========== VERIFY PENDING PAYMENT ==========");

            Map<String, BigDecimal> pendingBalances =
                paymentDAO.getOutstandingBalances(tripId);

            System.out.println(
                "U003 balance before completion: "
                + pendingBalances.getOrDefault("U003", BigDecimal.ZERO)
            );

            System.out.println("\n========== UPDATE PAYMENT STATUS ==========");

            boolean updated = paymentDAO.updatePaymentStatus(
                testPaymentId, "COMPLETED"
            );

            System.out.println("Payment completed: " + updated);

            Map<String, Object> completed =
                paymentDAO.getPaymentById(testPaymentId);

            System.out.println("Updated status: " + completed.get("status"));

            System.out.println("\n========== VERIFY UPDATED BALANCES ==========");

            Map<String, BigDecimal> completedBalances =
                paymentDAO.getOutstandingBalances(tripId);

            completedBalances.forEach((userId, balance) ->
                System.out.println(userId + " : Rs. " + balance)
            );

            System.out.println("\n========== TOTAL PAYMENTS ==========");

            System.out.println(
                "Completed payment total: Rs. "
                + paymentDAO.getTotalPayments(tripId)
            );

            System.out.println(
                "Payment count: " + paymentDAO.getPaymentCount(tripId)
            );

            System.out.println("\n========== DELETE TEST PAYMENT ==========");

            boolean deleted = paymentDAO.deletePayment(testPaymentId);

            System.out.println("Payment deleted: " + deleted);

            System.out.println(
                "Payment after deletion: "
                + paymentDAO.getPaymentById(testPaymentId)
            );

            System.out.println(
                "Final payment count: "
                + paymentDAO.getPaymentCount(tripId)
            );

            System.out.println(
                "\n========== PAYMENT DAO TEST COMPLETED =========="
            );

        } catch (Exception e) {
            System.err.println("Payment DAO test failed:");
            e.printStackTrace();

        } finally {
            if (testPaymentId != null) {
                try {
                    paymentDAO.deletePayment(testPaymentId);
                } catch (Exception cleanupError) {
                    System.err.println(
                        "Cleanup failed: " + cleanupError.getMessage()
                    );
                }
            }
        }
    }
}
