
package com.tripsync.interfaces;

import java.math.BigDecimal;

public interface Payable {

    // Record a payment from one traveler to another
    void settlePayment(
        String fromUserId,
        String toUserId,
        BigDecimal amount
    );

    // Get the outstanding balance of a traveler
    BigDecimal getOutstandingBalance(String userId);

    // Display all pending payments
    void displayPendingPayments();
}
