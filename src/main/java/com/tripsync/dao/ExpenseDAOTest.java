
package com.tripsync.dao;

import com.tripsync.model.Expense;
import com.tripsync.model.Traveler;
import com.tripsync.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class ExpenseDAOTest {

    public static void main(String[] args) {

        UserDAO userDAO = new UserDAO();
        ExpenseDAO expenseDAO = new ExpenseDAO();

        String tripId = "T001";
        String testExpenseId = null;

        try {
            System.out.println("========== EXPENSE DAO TEST ==========");

            // 1. Retrieve test travelers
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

            System.out.println("\n========== CREATE EXPENSE ==========");

            Expense expense = new Expense(
                tripId,
                "JDBC Test Dinner",
                new BigDecimal("1200.00"),
                rhea,
                LocalDate.of(2027, 1, 11)
            );

            expense.splitEqually(travelers);
            testExpenseId = expense.getExpenseId();

            boolean inserted = expenseDAO.addExpense(expense);

            System.out.println("Expense inserted: " + inserted);
            System.out.println("Expense ID: " + testExpenseId);
            System.out.println("Description: " + expense.getDescription());
            System.out.println("Amount: Rs. " + expense.getAmount());

            System.out.println("\n========== VERIFY SPLITS ==========");

            expense.getSplitAmounts().forEach((userId, share) ->
                System.out.println(
                    "Traveler: " + userId + " | Share: Rs. " + share
                )
            );

            System.out.println("\n========== RETRIEVE EXPENSE ==========");

            Expense retrieved =
                expenseDAO.getExpenseById(testExpenseId);

            if (retrieved == null) {
                throw new IllegalStateException("Expense retrieval failed.");
            }

            System.out.println("Retrieved description: "
                    + retrieved.getDescription());
            System.out.println("Retrieved amount: Rs. "
                    + retrieved.getAmount());
            System.out.println("Retrieved payer: "
                    + retrieved.getPaidBy().getName());
            System.out.println("Retrieved splits: "
                    + retrieved.getSplitAmounts());

            System.out.println("Correct ID: "
                    + retrieved.getExpenseId().equals(testExpenseId));

            System.out.println("\n========== GET EXPENSES BY TRIP ==========");

            List<Expense> tripExpenses =
                expenseDAO.getExpensesByTripId(tripId);

            System.out.println("Expenses for trip: " + tripExpenses.size());

            for (Expense item : tripExpenses) {
                System.out.println(
                    item.getDescription()
                    + " | Rs. " + item.getAmount()
                );
            }

            System.out.println("\n========== GET ALL EXPENSES ==========");

            System.out.println(
                "Total database expenses: "
                + expenseDAO.getAllExpenses().size()
            );

            System.out.println("\n========== UPDATE EXPENSE ==========");

            retrieved.updateDescription("Updated JDBC Test Dinner");

            boolean updated = expenseDAO.updateExpense(retrieved);

            System.out.println("Expense updated: " + updated);

            Expense updatedExpense =
                expenseDAO.getExpenseById(testExpenseId);

            System.out.println(
                "Updated description: "
                + updatedExpense.getDescription()
            );

            System.out.println("\n========== TOTAL EXPENSES ==========");

            System.out.println(
                "Trip total: Rs. "
                + expenseDAO.getTotalExpenses(tripId)
            );

            System.out.println(
                "Overall total: Rs. "
                + expenseDAO.getTotalExpenses()
            );

            System.out.println(
                "Expense count: " + expenseDAO.getExpenseCount()
            );

            System.out.println("\n========== DELETE TEST EXPENSE ==========");

            boolean deleted = expenseDAO.deleteExpense(testExpenseId);

            System.out.println("Expense deleted: " + deleted);

            Expense afterDelete =
                expenseDAO.getExpenseById(testExpenseId);

            System.out.println(
                "Exists after deletion: " + (afterDelete != null)
            );

            System.out.println(
                "\n========== EXPENSE DAO TEST COMPLETED =========="
            );

        } catch (SQLException e) {
            System.err.println("Database test failed:");
            e.printStackTrace();

        } catch (Exception e) {
            System.err.println("Expense DAO test failed:");
            e.printStackTrace();

        } finally {
            // Cleanup if a failure happened before normal deletion
            if (testExpenseId != null) {
                try {
                    expenseDAO.deleteExpense(testExpenseId);
                } catch (SQLException cleanupError) {
                    System.err.println(
                        "Cleanup failed: " + cleanupError.getMessage()
                    );
                }
            }
        }
    }
}
