
package com.tripsync.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Expense {

    private final String expenseId;
    private final String tripId;
    private String description;
    private BigDecimal amount;
    private final Traveler paidBy;
    private final LocalDate expenseDate;

    private final Map<String, BigDecimal> splitAmounts;

    public Expense(
            String tripId,
            String description,
            BigDecimal amount,
            Traveler paidBy,
            LocalDate expenseDate) {

        if (tripId == null || tripId.isBlank()) {
            throw new IllegalArgumentException("Trip ID is required.");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                "Expense description cannot be empty."
            );
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Expense amount must be positive."
            );
        }

        if (paidBy == null) {
            throw new IllegalArgumentException(
                "Payer cannot be null."
            );
        }

        if (expenseDate == null) {
            throw new IllegalArgumentException(
                "Expense date is required."
            );
        }

        this.expenseId = UUID.randomUUID().toString();
        this.tripId = tripId;
        this.description = description;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.paidBy = paidBy;
        this.expenseDate = expenseDate;
        this.splitAmounts = new LinkedHashMap<>();
    }

    // Getters

    public String getExpenseId() {
        return expenseId;
    }

    public String getTripId() {
        return tripId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Traveler getPaidBy() {
        return paidBy;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public Map<String, BigDecimal> getSplitAmounts() {
        return Collections.unmodifiableMap(splitAmounts);
    }

    // Update expense description

    public void updateDescription(String description) {

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                "Description cannot be empty."
            );
        }

        this.description = description;
    }

    // Split expense equally among travelers

    public void splitEqually(List<Traveler> travelers) {

        if (travelers == null || travelers.isEmpty()) {
            throw new IllegalArgumentException(
                "At least one traveler is required."
            );
        }

        Set<String> travelerIds = new HashSet<>();

        for (Traveler traveler : travelers) {

            if (traveler == null) {
                throw new IllegalArgumentException(
                    "Traveler cannot be null."
                );
            }

            if (!travelerIds.add(traveler.getUserId())) {
                throw new IllegalArgumentException(
                    "Duplicate traveler found."
                );
            }
        }

        splitAmounts.clear();

        BigDecimal share = amount.divide(
            BigDecimal.valueOf(travelers.size()),
            2,
            RoundingMode.DOWN
        );

        BigDecimal distributedAmount = BigDecimal.ZERO;

        for (int i = 0; i < travelers.size(); i++) {

            Traveler traveler = travelers.get(i);
            BigDecimal travelerShare;

            if (i == travelers.size() - 1) {
                // Assign remaining paise to the last traveler
                travelerShare = amount.subtract(distributedAmount);
            } else {
                travelerShare = share;
                distributedAmount = distributedAmount.add(share);
            }

            splitAmounts.put(
                traveler.getUserId(),
                travelerShare
            );
        }
    }

    // Display expense information

    public void displayExpense() {

        System.out.println("\n----- EXPENSE DETAILS -----");

        System.out.println("Expense ID: " + expenseId);
        System.out.println("Description: " + description);
        System.out.println("Trip ID: " + tripId);
        System.out.println("Amount: Rs. " + amount);
        System.out.println("Paid By: " + paidBy.getName());
        System.out.println("Date: " + expenseDate);

        System.out.println("\nExpense Split:");

        if (splitAmounts.isEmpty()) {
            System.out.println("Expense has not been split yet.");
        } else {
            for (Map.Entry<String, BigDecimal> entry
                    : splitAmounts.entrySet()) {

                System.out.println(
                    "Traveler ID: " + entry.getKey()
                    + " | Share: Rs. " + entry.getValue()
                );
            }
        }
    }
}
