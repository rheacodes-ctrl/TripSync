package com.tripsync.service;

import com.tripsync.dao.ExpenseDAO;
import com.tripsync.dao.PaymentDAO;
import com.tripsync.model.Expense;
import com.tripsync.model.Traveler;
import com.tripsync.model.Trip;
import com.tripsync.interfaces.Payable;
import com.tripsync.exceptions.BudgetExceededException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExpenseManager implements Payable {

    // Database access objects replacing the old in-memory maps.
    private final ExpenseDAO expenseDAO;
    private final PaymentDAO paymentDAO;

    // Outstanding balances and settlements are per-trip in the
    // database. The Payable interface methods do not carry a trip
    // id, so the manager scopes those operations to the trip of
    // the first expense added through this manager.
    private String managedTripId;

    public ExpenseManager() {
        this.expenseDAO = new ExpenseDAO();
        this.paymentDAO = new PaymentDAO();
    }

    // ==========================================
    // 1. ADD EXPENSE
    // ==========================================

    public void addExpense(Expense expense, List<Traveler> travelers) {

        if (expense == null || travelers == null || travelers.isEmpty()) {
            throw new IllegalArgumentException(
                "Expense and travelers are required."
            );
        }

        // Preserve the original "already exists" guard.
        try {
            if (expenseDAO.getExpenseById(expense.getExpenseId()) != null) {
                throw new IllegalArgumentException(
                    "Expense already exists."
                );
            }
        } catch (SQLException e) {
            throw databaseError("checking expense", e);
        }

        boolean payerIncluded = false;

        for (Traveler traveler : travelers) {

            if (traveler == null) {
                throw new IllegalArgumentException(
                    "Traveler cannot be null."
                );
            }

            if (traveler.getUserId().equals(
                    expense.getPaidBy().getUserId())) {
                payerIncluded = true;
            }
        }

        if (!payerIncluded) {
            throw new IllegalArgumentException(
                "The payer must be included in the expense split."
            );
        }

        // Populate the splits on the expense object.
        expense.splitEqually(travelers);

        // Persist the expense and its splits.
        try {
            boolean added = expenseDAO.addExpense(expense);

            if (!added) {
                throw new IllegalStateException(
                    "Expense could not be saved."
                );
            }

        } catch (SQLException e) {
            throw databaseError("saving expense", e);
        }

        // Scope Payable operations to this trip.
        if (managedTripId == null) {
            managedTripId = expense.getTripId();
        }

        System.out.println(
            "Expense added successfully: "
            + expense.getDescription()
        );
    }

    // ==========================================
    // 2. ADD EXPENSE WITH BUDGET VALIDATION
    // ==========================================

    public void addExpense(
            Trip trip,
            Expense expense,
            List<Traveler> travelers
    ) throws BudgetExceededException {

        if (trip == null || expense == null) {
            throw new IllegalArgumentException(
                "Trip and expense cannot be null."
            );
        }

        if (!trip.getTripId().equals(expense.getTripId())) {
            throw new IllegalArgumentException(
                "Expense does not belong to this trip."
            );
        }

        BigDecimal currentExpenses =
            getTotalExpenses(trip.getTripId());

        BigDecimal newTotal = currentExpenses.add(
            expense.getAmount()
        );

        if (newTotal.compareTo(trip.getBudget()) > 0) {

            BigDecimal availableBudget =
                trip.getBudget().subtract(currentExpenses);

            throw new BudgetExceededException(
                availableBudget,
                expense.getAmount()
            );
        }

        // Add only after successful budget validation.
        addExpense(expense, travelers);
    }

    // ==========================================
    // 3. GET EXPENSE BY ID
    // ==========================================

    public Expense getExpenseById(String expenseId) {

        try {
            Expense expense = expenseDAO.getExpenseById(expenseId);

            if (expense == null) {
                throw new IllegalArgumentException(
                    "Expense not found."
                );
            }

            return expense;

        } catch (SQLException e) {
            throw databaseError("retrieving expense", e);
        }
    }

    // ==========================================
    // 4. DISPLAY ALL EXPENSES
    // ==========================================

    public void displayAllExpenses() {

        List<Expense> expenses = getAllExpenses();

        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }

        System.out.println("\n========== ALL EXPENSES ==========");

        for (Expense expense : expenses) {
            expense.displayExpense();
        }
    }

    // ==========================================
    // 5. TOTAL EXPENSES
    // ==========================================

    public BigDecimal getTotalExpenses() {

        try {
            return expenseDAO.getTotalExpenses();

        } catch (SQLException e) {
            throw databaseError("calculating total expenses", e);
        }
    }

    // ==========================================
    // 6. TOTAL EXPENSES FOR A PARTICULAR TRIP
    // ==========================================

    public BigDecimal getTotalExpenses(String tripId) {

        try {
            return expenseDAO.getTotalExpenses(tripId);

        } catch (SQLException e) {
            throw databaseError("calculating trip expenses", e);
        }
    }

    // ==========================================
    // 7. REMAINING TRIP BUDGET
    // ==========================================

    public BigDecimal getRemainingBudget(Trip trip) {

        if (trip == null) {
            throw new IllegalArgumentException(
                "Trip cannot be null."
            );
        }

        return trip.getBudget().subtract(
            getTotalExpenses(trip.getTripId())
        );
    }

    // ==========================================
    // 8. GET EXPENSE COUNT
    // ==========================================

    public int getExpenseCount() {

        try {
            return expenseDAO.getExpenseCount();

        } catch (SQLException e) {
            throw databaseError("counting expenses", e);
        }
    }

    // ==========================================
    // 9. GET ALL EXPENSES
    // ==========================================

    public List<Expense> getAllExpenses() {

        try {
            return Collections.unmodifiableList(
                new ArrayList<>(expenseDAO.getAllExpenses())
            );

        } catch (SQLException e) {
            throw databaseError("retrieving expenses", e);
        }
    }

    // ==========================================
    // 10. GET OUTSTANDING BALANCE
    // ==========================================

    @Override
    public BigDecimal getOutstandingBalance(String userId) {

        if (managedTripId == null) {
            return BigDecimal.ZERO;
        }

        try {
            return paymentDAO.getOutstandingBalance(
                managedTripId, userId);

        } catch (SQLException e) {
            throw databaseError("retrieving balance", e);
        }
    }

    // ==========================================
    // 11. SETTLE PAYMENT (Payable interface)
    // ==========================================

    @Override
    public void settlePayment(
            String fromUserId,
            String toUserId,
            BigDecimal amount) {

        if (managedTripId == null) {
            throw new IllegalStateException(
                "No trip is currently managed by this ExpenseManager."
            );
        }

        settle(managedTripId, fromUserId, toUserId, amount);
    }

    // ==========================================
    // 12. SETTLE PAYMENT FOR AN EXPLICIT TRIP
    // ==========================================

    // A settlement must be stored against a trip, so this explicit
    // overload records the payment for the given trip and returns
    // the generated payment id.
    public String settlePayment(
            String tripId,
            String fromUserId,
            String toUserId,
            BigDecimal amount) {

        if (tripId == null || tripId.isBlank()) {
            throw new IllegalArgumentException(
                "Trip ID is required."
            );
        }

        return settle(tripId, fromUserId, toUserId, amount);
    }

    // ==========================================
    // 13. DISPLAY PENDING PAYMENTS
    // ==========================================

    @Override
    public void displayPendingPayments() {

        System.out.println("\n========== PENDING PAYMENTS ==========");

        Map<String, BigDecimal> balances;

        if (managedTripId != null) {

            try {
                balances =
                    paymentDAO.getOutstandingBalances(managedTripId);

            } catch (SQLException e) {
                throw databaseError("retrieving balances", e);
            }

        } else {
            balances = Collections.emptyMap();
        }

        boolean found = false;

        for (Map.Entry<String, BigDecimal> entry
                : balances.entrySet()) {

            BigDecimal balance = entry.getValue();

            if (balance.compareTo(BigDecimal.ZERO) < 0) {

                System.out.println(
                    "Traveler ID: " + entry.getKey()
                    + " owes Rs. " + balance.abs()
                );

                found = true;

            } else if (balance.compareTo(BigDecimal.ZERO) > 0) {

                System.out.println(
                    "Traveler ID: " + entry.getKey()
                    + " should receive Rs. " + balance
                );

                found = true;
            }
        }

        if (!found) {
            System.out.println("All payments are settled!");
        }
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

    // Validates and records a completed payment for the given trip.
    // Balances are read from the database so the settlement stays
    // consistent with every persisted expense and payment.
    private String settle(
            String tripId,
            String fromUserId,
            String toUserId,
            BigDecimal amount) {

        if (fromUserId == null || toUserId == null
                || fromUserId.isBlank() || toUserId.isBlank()) {

            throw new IllegalArgumentException(
                "Both traveler IDs are required."
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

        try {
            BigDecimal fromBalance =
                paymentDAO.getOutstandingBalance(tripId, fromUserId);

            BigDecimal toBalance =
                paymentDAO.getOutstandingBalance(tripId, toUserId);

            if (fromBalance.compareTo(BigDecimal.ZERO) >= 0) {
                throw new IllegalStateException(
                    "This traveler does not owe money."
                );
            }

            if (toBalance.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException(
                    "The receiving traveler is not owed money."
                );
            }

            BigDecimal maxSettlement =
                fromBalance.abs().min(toBalance);

            if (amount.compareTo(maxSettlement) > 0) {
                throw new IllegalArgumentException(
                    "Payment exceeds the outstanding amount."
                );
            }

            // A completed payment is what moves the balances in the
            // database, so no separate in-memory update is needed.
            String paymentId = paymentDAO.recordPayment(
                tripId, fromUserId, toUserId, amount
            );

            if (paymentId == null) {
                throw new IllegalStateException(
                    "Payment could not be recorded."
                );
            }

            System.out.println(
                "Payment of Rs. " + amount
                + " successfully settled."
            );

            return paymentId;

        } catch (SQLException e) {
            throw databaseError("settling payment", e);
        }
    }

    private IllegalStateException databaseError(
            String operation,
            SQLException cause) {

        return new IllegalStateException(
            "Database error while " + operation
                + ": " + cause.getMessage(),
            cause
        );
    }
}
