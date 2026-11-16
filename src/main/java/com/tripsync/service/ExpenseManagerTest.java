package com.tripsync.service;

import com.tripsync.dao.ExpenseDAO;
import com.tripsync.dao.PaymentDAO;
import com.tripsync.dao.TripDAO;
import com.tripsync.dao.UserDAO;

import com.tripsync.exceptions.BudgetExceededException;
import com.tripsync.model.Expense;
import com.tripsync.model.Traveler;
import com.tripsync.model.Trip;
import com.tripsync.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class ExpenseManagerTest {

    public static void main(String[] args) throws Exception {

        ExpenseDAO expenseDAO = new ExpenseDAO();
        PaymentDAO paymentDAO = new PaymentDAO();
        TripDAO tripDAO = new TripDAO();
        UserDAO userDAO = new UserDAO();

        ExpenseManager expenseManager = new ExpenseManager();

        // Seeded records used as context. Never modified or deleted.
        String tripId = "T001";

        // Seeded expense E001 (Rs. 3000) is the only expense for T001.
        // Seeded balances (after expense E001 and payment P001):
        //   U001 = -1000 (owes), U002 = +1000 (owed), U003 = 0
        BigDecimal seededTripTotal = new BigDecimal("3000.00");
        BigDecimal seededU001 = new BigDecimal("-1000.00");
        BigDecimal seededU002 = new BigDecimal("1000.00");
        BigDecimal seededU003 = BigDecimal.ZERO;

        String testExpenseId = null;
        String testPaymentId = null;
        String expensiveExpenseId = null;

        System.out.println("========== EXPENSE MANAGER INTEGRATION TEST ==========");

        try {

            // ==========================================
            // 1. VERIFY SEEDED CONTEXT
            // ==========================================

            System.out.println("\n========== VERIFY SEEDED CONTEXT ==========");

            Trip trip = tripDAO.getTripById(tripId);

            if (trip == null) {
                throw new IllegalStateException("Seeded trip T001 was not found.");
            }

            User rheaUser = userDAO.getUserById("U002");
            User shreyaUser = userDAO.getUserById("U003");

            if (!(rheaUser instanceof Traveler)
                    || !(shreyaUser instanceof Traveler)) {
                throw new IllegalStateException(
                    "Seeded travelers U002 and U003 were not found."
                );
            }

            Traveler rhea = (Traveler) rheaUser;
            Traveler shreya = (Traveler) shreyaUser;
            List<Traveler> travelers = Arrays.asList(rhea, shreya);

            System.out.println("Trip: " + trip.getTripName());
            System.out.println("Budget: Rs. " + trip.getBudget());
            System.out.println("Travelers: " + rhea.getName() + ", " + shreya.getName());

            System.out.println("\nSeeded outstanding balances:");
            assertBalance(paymentDAO.getOutstandingBalance(tripId, "U001"), seededU001, "seeded U001");
            assertBalance(paymentDAO.getOutstandingBalance(tripId, "U002"), seededU002, "seeded U002");
            assertBalance(paymentDAO.getOutstandingBalance(tripId, "U003"), seededU003, "seeded U003");

            int initialExpenseCount = expenseDAO.getExpenseCount();
            System.out.println("Initial expense count (seeded): " + initialExpenseCount);

            if (expenseManager.getTotalExpenses(tripId).compareTo(seededTripTotal) != 0) {
                throw new IllegalStateException(
                    "Seeded trip total was not Rs. " + seededTripTotal
                );
            }

            // ==========================================
            // 2. ADD EXPENSE WITH BUDGET VALIDATION (PERSIST)
            // ==========================================

            System.out.println("\n========== ADD EXPENSE (BUDGET VALIDATED) ==========");

            Expense testExpense = new Expense(
                tripId,
                "JDBC Test Lunch",
                new BigDecimal("600.00"),
                rhea,
                LocalDate.of(2027, 1, 11)
            );

            testExpenseId = testExpense.getExpenseId();

            // Splits 300/300 between U002 and U003, paid by U002.
            expenseManager.addExpense(trip, testExpense, travelers);

            System.out.println("Expense ID: " + testExpenseId);

            System.out.println(
                "Expense exists in database: "
                + (expenseDAO.getExpenseById(testExpenseId) != null)
            );

            if (expenseDAO.getExpenseById(testExpenseId) == null) {
                throw new IllegalStateException("Expense was not persisted.");
            }

            // ==========================================
            // 3. VERIFY CONSISTENT BALANCES (SEED + TEST EXPENSE)
            // ==========================================

            System.out.println("\n========== BALANCES AFTER EXPENSE ==========");

            // The first addExpense scopes the manager to T001, so the
            // Payable interface methods now read T001 balances.
            assertBalance(expenseManager.getOutstandingBalance("U001"), new BigDecimal("-1000.00"), "U001");
            assertBalance(expenseManager.getOutstandingBalance("U002"), new BigDecimal("1300.00"), "U002");
            assertBalance(expenseManager.getOutstandingBalance("U003"), new BigDecimal("-300.00"), "U003");

            System.out.println(
                "Trip total after expense: Rs. "
                + expenseManager.getTotalExpenses(tripId)
            );

            if (expenseManager.getTotalExpenses(tripId).compareTo(new BigDecimal("3600.00")) != 0) {
                throw new IllegalStateException("Trip total was not Rs. 3600.00.");
            }

            System.out.println(
                "Remaining budget: Rs. "
                + expenseManager.getRemainingBudget(trip)
            );

            if (expenseManager.getRemainingBudget(trip).compareTo(new BigDecimal("56400.00")) != 0) {
                throw new IllegalStateException("Remaining budget was not Rs. 56400.00.");
            }

            if (expenseManager.getExpenseCount() != initialExpenseCount + 1) {
                throw new IllegalStateException("Expense count did not increase.");
            }

            // ==========================================
            // 4. BUDGET EXCEEDED (MUST NOT PERSIST)
            // ==========================================

            System.out.println("\n========== BUDGET EXCEEDED GUARD ==========");

            Expense expensiveExpense = new Expense(
                tripId,
                "Luxury Yacht",
                new BigDecimal("70000.00"),
                rhea,
                LocalDate.of(2027, 1, 12)
            );

            expensiveExpenseId = expensiveExpense.getExpenseId();

            boolean budgetExceeded = false;

            try {
                expenseManager.addExpense(trip, expensiveExpense, travelers);
            } catch (BudgetExceededException expected) {
                budgetExceeded = true;
                System.out.println("Budget exception: " + expected.getMessage());
            }

            if (!budgetExceeded) {
                throw new IllegalStateException("BudgetExceededException was not thrown.");
            }

            System.out.println(
                "Oversized expense persisted (should be false): "
                + (expenseDAO.getExpenseById(expensiveExpenseId) != null)
            );

            if (expenseDAO.getExpenseById(expensiveExpenseId) != null) {
                throw new IllegalStateException(
                    "Oversized expense was persisted despite the budget breach."
                );
            }

            expensiveExpenseId = null; // never persisted, no cleanup needed

            // ==========================================
            // 5. SETTLE PAYMENT (PERSIST A COMPLETED PAYMENT)
            // ==========================================

            System.out.println("\n========== SETTLE PAYMENT ==========");

            // U003 owes 300, U002 is owed 1300. Settle 200.
            testPaymentId = expenseManager.settlePayment(
                tripId, "U003", "U002", new BigDecimal("200.00")
            );

            System.out.println("Recorded payment ID: " + testPaymentId);

            if (testPaymentId == null
                    || paymentDAO.getPaymentById(testPaymentId) == null) {
                throw new IllegalStateException("Payment was not persisted.");
            }

            System.out.println("\nBalances after settlement:");
            assertBalance(expenseManager.getOutstandingBalance("U001"), new BigDecimal("-1000.00"), "U001");
            assertBalance(expenseManager.getOutstandingBalance("U002"), new BigDecimal("1100.00"), "U002");
            assertBalance(expenseManager.getOutstandingBalance("U003"), new BigDecimal("-100.00"), "U003");

            // ==========================================
            // 6. SETTLEMENT GUARDS (NO PAYMENT RECORDED)
            // ==========================================

            System.out.println("\n========== SETTLEMENT GUARDS ==========");

            expectFailure("exceeds outstanding", () ->
                expenseManager.settlePayment(
                    tripId, "U003", "U002", new BigDecimal("500.00")
                )
            );

            expectFailure("does not owe", () ->
                expenseManager.settlePayment(
                    tripId, "U002", "U003", new BigDecimal("100.00")
                )
            );

            expectFailure("cannot pay themselves", () ->
                expenseManager.settlePayment(
                    tripId, "U003", "U003", new BigDecimal("100.00")
                )
            );

            // The Payable interface overload must use the managed trip,
            // reaching the balance checks rather than the "no trip" guard.
            try {
                expenseManager.settlePayment(
                    "U002", "U003", new BigDecimal("100.00")
                );

                throw new IllegalStateException(
                    "Expected the interface settlement to be rejected."
                );

            } catch (RuntimeException expected) {

                if (expected.getMessage().contains("No trip is currently managed")) {
                    throw new IllegalStateException(
                        "Interface settlement did not use the managed trip.",
                        expected
                    );
                }

                System.out.println(
                    "  interface does not owe rejected: "
                    + expected.getMessage()
                );
            }

            // No extra payments should have been recorded by the guards.
            if (paymentDAO.getPaymentCount(tripId) != 2) {
                throw new IllegalStateException(
                    "Settlement guards recorded unexpected payments. Count: "
                    + paymentDAO.getPaymentCount(tripId)
                );
            }

            // ==========================================
            // 7. RETRIEVE AND DISPLAY
            // ==========================================

            System.out.println("\n========== RETRIEVE AND DISPLAY ==========");

            Expense retrieved = expenseManager.getExpenseById(testExpenseId);

            System.out.println(
                "Retrieved expense: " + retrieved.getDescription()
            );

            System.out.println(
                "Retrieved splits: " + retrieved.getSplitAmounts()
            );

            expenseManager.displayAllExpenses();
            expenseManager.displayPendingPayments();

            System.out.println(
                "All-expenses count: " + expenseManager.getAllExpenses().size()
            );

            System.out.println(
                "Total expenses (all trips): Rs. "
                + expenseManager.getTotalExpenses()
            );

            // ==========================================
            // 8. CLEANUP AND VERIFY SEED RESTORED
            // ==========================================

            System.out.println("\n========== CLEANUP ==========");

            if (testPaymentId != null) {
                System.out.println(
                    "Test payment deleted: "
                    + paymentDAO.deletePayment(testPaymentId)
                );
                testPaymentId = null;
            }

            if (testExpenseId != null) {
                System.out.println(
                    "Test expense deleted: "
                    + expenseDAO.deleteExpense(testExpenseId)
                );
                testExpenseId = null;
            }

            System.out.println("\nBalances after cleanup (should match seed):");
            assertBalance(paymentDAO.getOutstandingBalance(tripId, "U001"), seededU001, "restored U001");
            assertBalance(paymentDAO.getOutstandingBalance(tripId, "U002"), seededU002, "restored U002");
            assertBalance(paymentDAO.getOutstandingBalance(tripId, "U003"), seededU003, "restored U003");

            if (expenseManager.getExpenseCount() != initialExpenseCount) {
                throw new IllegalStateException(
                    "Expense count did not return to its seeded value."
                );
            }

            System.out.println(
                "\n========== EXPENSE MANAGER INTEGRATION TEST COMPLETED =========="
            );

        } catch (Exception e) {

            System.out.println("\nEXPENSE MANAGER TEST FAILED!");
            e.printStackTrace();

            throw e;

        } finally {

            // Best-effort cleanup using the DAOs directly.
            try {
                if (testPaymentId != null) {
                    paymentDAO.deletePayment(testPaymentId);
                }

                if (testExpenseId != null) {
                    expenseDAO.deleteExpense(testExpenseId);
                }

                // The oversized expense is never persisted, but guard
                // against it just in case a future change does persist.
                if (expensiveExpenseId != null) {
                    expenseDAO.deleteExpense(expensiveExpenseId);
                }

                System.out.println("Temporary expense/payment cleanup attempted.");

            } catch (Exception cleanupError) {
                System.err.println("Test cleanup failed:");
                cleanupError.printStackTrace();
            }
        }
    }

    // ==========================================
    // TEST HELPERS
    // ==========================================

    private static void assertBalance(
            BigDecimal actual, BigDecimal expected, String label) {

        System.out.println("  " + label + " = Rs. " + actual);

        if (actual == null || actual.compareTo(expected) != 0) {
            throw new IllegalStateException(
                "Balance mismatch for " + label
                    + ": expected Rs. " + expected
                    + ", got Rs. " + actual
            );
        }
    }

    @FunctionalInterface
    private interface SettlementAction {
        void run();
    }

    private static void expectFailure(String label, SettlementAction action) {

        try {
            action.run();
        } catch (RuntimeException expected) {
            System.out.println("  " + label + " rejected: " + expected.getMessage());
            return;
        }

        throw new IllegalStateException(
            "Expected failure for: " + label
        );
    }
}
