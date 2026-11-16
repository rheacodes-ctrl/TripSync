package com.tripsync.service;

import com.tripsync.dao.ReservationDAO;
import com.tripsync.reservation.Reservation;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ReservationManager {

    // Database access object replacing the old in-memory map.
    private final ReservationDAO reservationDAO;

    public ReservationManager() {
        this.reservationDAO = new ReservationDAO();
    }

    // Add a new reservation
    public void addReservation(Reservation reservation) {

        if (reservation == null) {
            throw new IllegalArgumentException(
                "Reservation cannot be null."
            );
        }

        String id = reservation.getReservationId();

        try {
            // Preserve the original "already exists" guard.
            if (reservationDAO.getReservationById(id) != null) {
                throw new IllegalArgumentException(
                    "Reservation already exists."
                );
            }

            boolean added = reservationDAO.addReservation(reservation);

            if (!added) {
                throw new IllegalStateException(
                    "Reservation could not be saved."
                );
            }

        } catch (SQLException e) {
            throw databaseError("adding reservation", e);
        }

        System.out.println(
            "Reservation added successfully: "
            + reservation.getReservationType()
        );
    }

    // Retrieve reservation using ID
    public Reservation getReservationById(String reservationId) {

        try {
            Reservation reservation =
                reservationDAO.getReservationById(reservationId);

            if (reservation == null) {
                throw new IllegalArgumentException(
                    "Reservation not found."
                );
            }

            return reservation;

        } catch (SQLException e) {
            throw databaseError("retrieving reservation", e);
        }
    }

    // Confirm a reservation
    public void confirmReservation(String reservationId) {

        Reservation reservation =
            getReservationById(reservationId);

        // Applies the state-transition validation (e.g. cannot
        // confirm a cancelled reservation) and updates the in-memory
        // object before persisting the new status.
        reservation.confirmReservation();

        updateStatus(reservationId, reservation.getStatus());

        System.out.println("Reservation confirmed successfully.");
    }

    // Cancel a reservation
    public void cancelReservation(String reservationId) {

        Reservation reservation =
            getReservationById(reservationId);

        reservation.cancelReservation();

        updateStatus(reservationId, reservation.getStatus());

        System.out.println("Reservation cancelled successfully.");
    }

    // Remove a reservation from the manager
    public void deleteReservation(String reservationId) {

        Reservation reservation =
            getReservationById(reservationId);

        try {
            boolean deleted =
                reservationDAO.deleteReservation(reservationId);

            if (!deleted) {
                throw new IllegalStateException(
                    "Reservation could not be deleted."
                );
            }

        } catch (SQLException e) {
            throw databaseError("deleting reservation", e);
        }

        System.out.println(
            reservation.getReservationType()
            + " reservation deleted successfully."
        );
    }

    // Display one reservation
    public void displayReservation(String reservationId) {

        Reservation reservation =
            getReservationById(reservationId);

        reservation.displayReservation();
    }

    // Display all reservations
    public void displayAllReservations() {

        Collection<Reservation> reservations = getAllReservations();

        if (reservations.isEmpty()) {
            System.out.println("No reservations found.");
            return;
        }

        System.out.println("\n========== ALL RESERVATIONS ==========");

        for (Reservation reservation : reservations) {
            reservation.displayReservation();
        }
    }

    // Display reservations belonging to a particular trip
    public void displayReservationsByTrip(String tripId) {

        List<Reservation> reservations;

        try {
            reservations =
                reservationDAO.getReservationsByTripId(tripId);

        } catch (SQLException e) {
            throw databaseError("retrieving reservations", e);
        }

        if (reservations.isEmpty()) {
            System.out.println(
                "No reservations found for this trip."
            );
            return;
        }

        for (Reservation reservation : reservations) {
            reservation.displayReservation();
        }
    }

    // Get all reservations without allowing external modification
    public Collection<Reservation> getAllReservations() {

        try {
            return Collections.unmodifiableCollection(
                reservationDAO.getAllReservations()
            );

        } catch (SQLException e) {
            throw databaseError("retrieving reservations", e);
        }
    }

    // Get total reservation count
    public int getReservationCount() {
        return getAllReservations().size();
    }

    // Get count of confirmed reservations
    public int getConfirmedReservationCount() {

        int count = 0;

        for (Reservation reservation : getAllReservations()) {
            if (reservation.getStatus() == Reservation.Status.CONFIRMED) {
                count++;
            }
        }

        return count;
    }

    // Get total cost of active reservations for a trip
    public BigDecimal getTotalReservationCost(String tripId) {

        try {
            return reservationDAO.getTotalReservationCost(tripId);

        } catch (SQLException e) {
            throw databaseError(
                "calculating reservation cost", e
            );
        }
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

    private void updateStatus(
            String reservationId, Reservation.Status status) {

        try {
            boolean updated =
                reservationDAO.updateReservationStatus(
                    reservationId, status
                );

            if (!updated) {
                throw new IllegalStateException(
                    "Reservation status could not be updated."
                );
            }

        } catch (SQLException e) {
            throw databaseError("updating reservation status", e);
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
