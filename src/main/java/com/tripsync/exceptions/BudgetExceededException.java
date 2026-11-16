
package com.tripsync.exceptions;

import java.math.BigDecimal;

public class BudgetExceededException extends Exception {

    private static final long serialVersionUID = 1L;

    // Constructor accepting a custom error message
    public BudgetExceededException(String message) {
        super(message);
    }

    // Constructor accepting budget and requested amount
    public BudgetExceededException(
            BigDecimal budget,
            BigDecimal requestedAmount) {

        super(
            "Budget exceeded! Available budget: Rs. "
            + budget
            + ", Requested amount: Rs. "
            + requestedAmount
        );
    }
}
