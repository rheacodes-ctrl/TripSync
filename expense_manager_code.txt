
package com.tripsync.service;

import com.tripsync.model.Expense;
import com.tripsync.model.Traveler;
import com.tripsync.model.Trip;
import com.tripsync.interfaces.Payable;
import com.tripsync.exceptions.BudgetExceededException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpenseManager implements Payable {

    private final Map<String, Expense> expenses;
    private final Map<String, BigDecimal> balances;

    public ExpenseManager() {
        expenses = new LinkedHashMap<>();
        balances = new LinkedHashMap<>();
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

        if (expenses.containsKey(expense.getExpenseId())) {
            throw new IllegalArgumentException(
                "Expense already exists."
            );
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

        expense.splitEqually(travelers);

        expenses.put(expense.getExpenseId(), expense);

        // Initialize traveler balances
        for (Traveler traveler : travelers) {
            balances.putIfAbsent(
                traveler.getUserId(),
                BigDecimal.ZERO
            );
        }

        // Credit the person who paid
        String payerId = expense.getPaidBy().getUserId();

        balances.put(
            payerId,
            balances.get(payerId).add(expense.getAmount())
        );

        // Deduct each traveler's share
        for (Map.Entry<String, BigDecimal> entry
                : expense.getSplitAmounts().entrySet()) {

            String userId = entry.getKey();
            BigDecimal share = entry.getValue();

            balances.put(
                userId,
                balances.get(userId).subtract(share)
            );
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

        // Add only after successful budget validation
        addExpense(expense, travelers);
    }

    // ==========================================
    // 3. GET EXPENSE BY ID
    // ==========================================

    public Expense getExpenseById(String expenseId) {

        Expense expense = expenses.get(expenseId);

        if (expense == null) {
            throw new IllegalArgumentException(
                "Expense not found."
            );
        }

        return expense;
    }

    // ==========================================
    // 4. DISPLAY ALL EXPENSES
    // ==========================================

    public void displayAllExpenses() {

        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }

        System.out.println("\n========== ALL EXPENSES ==========");

        for (Expense expense : expenses.values()) {
            expense.displayExpense();
        }
    }

    // ==========================================
    // 5. TOTAL EXPENSES
    // ==========================================

    public BigDecimal getTotalExpenses() {

        BigDecimal total = BigDecimal.ZERO;

        for (Expense expense : expenses.values()) {
            total = total.add(expense.getAmount());
        }

        return total;
    }

    // ==========================================
    // 6. TOTAL EXPENSES FOR A PARTICULAR TRIP
    // ==========================================

    public BigDecimal getTotalExpenses(String tripId) {

        BigDecimal total = BigDecimal.ZERO;

        for (Expense expense : expenses.values()) {

            if (expense.getTripId().equals(tripId)) {
                total = total.add(expense.getAmount());
            }
        }

        return total;
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
        return expenses.size();
    }

    // ==========================================
    // 9. GET ALL EXPENSES
    // ==========================================

    public List<Expense> getAllExpenses() {

        return Collections.unmodifiableList(
            new ArrayList<>(expenses.values())
        );
    }

    // ==========================================
    // 10. GET OUTSTANDING BALANCE
    // ==========================================

    @Override
    public BigDecimal getOutstandingBalance(String userId) {

        return balances.getOrDefault(
            userId,
            BigDecimal.ZERO
        );
    }

    // ==========================================
    // 11. SETTLE PAYMENT
    // ==========================================

    @Override
    public void settlePayment(
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

        BigDecimal fromBalance =
            getOutstandingBalance(fromUserId);

        BigDecimal toBalance =
            getOutstandingBalance(toUserId);

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

        // Update balances
        balances.put(
            fromUserId,
            fromBalance.add(amount)
        );

        balances.put(
            toUserId,
            toBalance.subtract(amount)
        );

        System.out.println(
            "Payment of Rs. " + amount
            + " successfully settled."
        );
    }

    // ==========================================
    // 12. DISPLAY PENDING PAYMENTS
    // ==========================================

    @Override
    public void displayPendingPayments() {

        System.out.println("\n========== PENDING PAYMENTS ==========");

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
}
