
package com.tripsync.dao;
import com.tripsync.model.User;
import com.tripsync.config.DBConnection;
import com.tripsync.model.Expense;
import com.tripsync.model.Traveler;
import com.tripsync.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExpenseDAO {

    private final UserDAO userDAO;

    public ExpenseDAO() {
        this.userDAO = new UserDAO();
    }

    // ==========================================
    // 1. CREATE EXPENSE AND SAVE ITS SPLITS
    // ==========================================

    public boolean addExpense(Expense expense) throws SQLException {

        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null.");
        }

        Map<String, BigDecimal> splits = expense.getSplitAmounts();

        if (splits == null || splits.isEmpty()) {
            throw new IllegalArgumentException(
                "Expense must be split before saving."
            );
        }

        BigDecimal splitTotal = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : splits.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null
                    || entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Invalid expense split.");
            }

            splitTotal = splitTotal.add(entry.getValue());
        }

        if (splitTotal.compareTo(expense.getAmount()) != 0) {
            throw new IllegalArgumentException(
                "The sum of splits must equal the expense amount."
            );
        }

        if (!splits.containsKey(expense.getPaidBy().getUserId())) {
            throw new IllegalArgumentException(
                "The payer must be included in the expense splits."
            );
        }

        String expenseSQL = """
                INSERT INTO expenses
                (expense_id, trip_id, description, amount, paid_by, expense_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        String splitSQL = """
                INSERT INTO expense_splits
                (split_id, expense_id, user_id, share_amount, is_settled)
                VALUES (?, ?, ?, ?, 0)
                """;

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Insert the main expense
                try (PreparedStatement statement =
                        connection.prepareStatement(expenseSQL)) {

                    statement.setString(1, expense.getExpenseId());
                    statement.setString(2, expense.getTripId());
                    statement.setString(3, expense.getDescription());
                    statement.setBigDecimal(4, expense.getAmount());
                    statement.setString(
                        5, expense.getPaidBy().getUserId()
                    );
                    statement.setDate(
                        6, Date.valueOf(expense.getExpenseDate())
                    );

                    statement.executeUpdate();
                }

                // Insert all traveler splits
                try (PreparedStatement statement =
                        connection.prepareStatement(splitSQL)) {

                    for (Map.Entry<String, BigDecimal> entry
                            : splits.entrySet()) {

                        statement.setString(
                            1, UUID.randomUUID().toString()
                        );
                        statement.setString(2, expense.getExpenseId());
                        statement.setString(3, entry.getKey());
                        statement.setBigDecimal(4, entry.getValue());

                        statement.addBatch();
                    }

                    statement.executeBatch();
                }

                connection.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    // ==========================================
    // 2. GET EXPENSE BY ID
    // ==========================================

    public Expense getExpenseById(String expenseId) throws SQLException {

        String sql = """
                SELECT expense_id, trip_id, description,
                       amount, paid_by, expense_date
                FROM expenses
                WHERE expense_id = ?
                """;

        String tripId;
        String description;
        BigDecimal amount;
        String payerId;
        java.time.LocalDate expenseDate;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, expenseId);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                tripId = result.getString("trip_id");
                description = result.getString("description");
                amount = result.getBigDecimal("amount");
                payerId = result.getString("paid_by");
                expenseDate = result.getDate("expense_date").toLocalDate();
            }
        }

        return buildExpense(
            expenseId, tripId, description,
            amount, payerId, expenseDate
        );
    }

    // ==========================================
    // 3. GET ALL EXPENSES FOR A TRIP
    // ==========================================

    public List<Expense> getExpensesByTripId(String tripId)
            throws SQLException {

        List<String> expenseIds = new ArrayList<>();

        String sql = """
                SELECT expense_id
                FROM expenses
                WHERE trip_id = ?
                ORDER BY expense_date, expense_id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tripId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    expenseIds.add(result.getString("expense_id"));
                }
            }
        }

        List<Expense> expenses = new ArrayList<>();

        for (String id : expenseIds) {
            Expense expense = getExpenseById(id);
            if (expense != null) {
                expenses.add(expense);
            }
        }

        return expenses;
    }

    // ==========================================
    // 4. GET ALL EXPENSES
    // ==========================================

    public List<Expense> getAllExpenses() throws SQLException {

        List<String> expenseIds = new ArrayList<>();

        String sql = """
                SELECT expense_id
                FROM expenses
                ORDER BY expense_date, expense_id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                expenseIds.add(result.getString("expense_id"));
            }
        }

        List<Expense> expenses = new ArrayList<>();

        for (String id : expenseIds) {
            Expense expense = getExpenseById(id);
            if (expense != null) {
                expenses.add(expense);
            }
        }

        return expenses;
    }

    // ==========================================
    // 5. UPDATE EXPENSE DESCRIPTION
    // ==========================================

    public boolean updateExpense(Expense expense) throws SQLException {

        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null.");
        }

        String sql = """
                UPDATE expenses
                SET description = ?
                WHERE expense_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, expense.getDescription());
            statement.setString(2, expense.getExpenseId());

            return statement.executeUpdate() == 1;
        }
    }

    // ==========================================
    // 6. DELETE EXPENSE AND ITS SPLITS
    // ==========================================

    public boolean deleteExpense(String expenseId) throws SQLException {

        String deleteSplitsSQL = """
                DELETE FROM expense_splits
                WHERE expense_id = ?
                """;

        String deleteExpenseSQL = """
                DELETE FROM expenses
                WHERE expense_id = ?
                """;

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Delete child split records first
                try (PreparedStatement statement =
                        connection.prepareStatement(deleteSplitsSQL)) {

                    statement.setString(1, expenseId);
                    statement.executeUpdate();
                }

                // Delete the expense
                int rowsDeleted;

                try (PreparedStatement statement =
                        connection.prepareStatement(deleteExpenseSQL)) {

                    statement.setString(1, expenseId);
                    rowsDeleted = statement.executeUpdate();
                }

                connection.commit();
                return rowsDeleted == 1;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    // ==========================================
    // 7. GET TOTAL EXPENSES
    // ==========================================

    public BigDecimal getTotalExpenses() throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM expenses
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            result.next();
            return result.getBigDecimal(1);
        }
    }

    // ==========================================
    // 8. GET TOTAL EXPENSES FOR A TRIP
    // ==========================================

    public BigDecimal getTotalExpenses(String tripId)
            throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM expenses
                WHERE trip_id = ?
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
    // 9. GET EXPENSE COUNT
    // ==========================================

    public int getExpenseCount() throws SQLException {

        String sql = "SELECT COUNT(*) FROM expenses";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            result.next();
            return result.getInt(1);
        }
    }

    // ==========================================
    // 10. GET ALL SPLITS FOR AN EXPENSE
    // ==========================================

    private Map<String, BigDecimal> getExpenseSplits(String expenseId)
            throws SQLException {

        Map<String, BigDecimal> splits = new LinkedHashMap<>();

        String sql = """
                SELECT user_id, share_amount
                FROM expense_splits
                WHERE expense_id = ?
                ORDER BY split_id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, expenseId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    splits.put(
                        result.getString("user_id"),
                        result.getBigDecimal("share_amount")
                    );
                }
            }
        }

        return splits;
    }

    // ==========================================
    // 11. MAP DATABASE RECORD TO EXPENSE OBJECT
    // ==========================================

    private Expense buildExpense(
            String expenseId,
            String tripId,
            String description,
            BigDecimal amount,
            String payerId,
            java.time.LocalDate expenseDate) throws SQLException {

        User user = userDAO.getUserById(payerId);

        if (!(user instanceof Traveler)) {
            throw new SQLException(
                "Expense payer is missing or is not a traveler: " + payerId
            );
        }

        Map<String, BigDecimal> splits =
            getExpenseSplits(expenseId);

        return new Expense(
            expenseId,
            tripId,
            description,
            amount,
            (Traveler) user,
            expenseDate,
            splits
        );
    }
}
