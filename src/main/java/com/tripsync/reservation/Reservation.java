
package com.tripsync.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public abstract class Reservation {

    public enum Status {
        PENDING, CONFIRMED, CANCELLED
    }

    private final String reservationId;
    private final String tripId;
    private String providerName;
    private BigDecimal cost;
    private LocalDate reservationDate;
    private Status status;

    // Constructor for creating a new reservation
    public Reservation(String tripId, String providerName,
                       BigDecimal cost, LocalDate reservationDate) {
        this(UUID.randomUUID().toString(), tripId, providerName,
                cost, reservationDate, Status.PENDING);
    }

    // Constructor for loading an existing reservation from MySQL
    protected Reservation(String reservationId, String tripId,
                          String providerName, BigDecimal cost,
                          LocalDate reservationDate, Status status) {

        if (reservationId == null || reservationId.isBlank()
                || tripId == null || tripId.isBlank()) {
            throw new IllegalArgumentException("Invalid reservation or trip ID.");
        }

        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be empty.");
        }

        if (cost == null || cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost cannot be negative.");
        }

        if (reservationDate == null || status == null) {
            throw new IllegalArgumentException("Date and status are required.");
        }

        this.reservationId = reservationId;
        this.tripId = tripId;
        this.providerName = providerName;
        this.cost = cost;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getTripId() {
        return tripId;
    }

    public String getProviderName() {
        return providerName;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public Status getStatus() {
        return status;
    }

    public void confirmReservation() {
        if (status == Status.CANCELLED) {
            throw new IllegalStateException(
                    "A cancelled reservation cannot be confirmed.");
        }
        status = Status.CONFIRMED;
    }

    public void cancelReservation() {
        if (status == Status.CANCELLED) {
            throw new IllegalStateException(
                    "Reservation is already cancelled.");
        }
        status = Status.CANCELLED;
    }

    public void updateCost(BigDecimal newCost) {
        if (newCost == null ||
                newCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid reservation cost.");
        }
        this.cost = newCost;
    }

    public abstract String getReservationType();

    public abstract void displayReservation();
}
