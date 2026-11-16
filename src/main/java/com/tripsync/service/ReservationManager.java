
package com.tripsync.service;

import com.tripsync.reservation.Reservation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReservationManager {

    private final Map<String, Reservation> reservations;

    public ReservationManager() {
        reservations = new LinkedHashMap<>();
    }

    // Add a new reservation
    public void addReservation(Reservation reservation) {

        if (reservation == null) {
            throw new IllegalArgumentException(
                "Reservation cannot be null."
            );
        }

        String id = reservation.getReservationId();

        if (reservations.containsKey(id)) {
            throw new IllegalArgumentException(
                "Reservation already exists."
            );
        }

        reservations.put(id, reservation);

        System.out.println(
            "Reservation added successfully: "
            + reservation.getReservationType()
        );
    }

    // Retrieve reservation using ID
    public Reservation getReservationById(String reservationId) {

        Reservation reservation = reservations.get(reservationId);

        if (reservation == null) {
            throw new IllegalArgumentException(
                "Reservation not found."
            );
        }

        return reservation;
    }

    // Confirm a reservation
    public void confirmReservation(String reservationId) {

        Reservation reservation =
            getReservationById(reservationId);

        reservation.confirmReservation();

        System.out.println("Reservation confirmed successfully.");
    }

    // Cancel a reservation
    public void cancelReservation(String reservationId) {

        Reservation reservation =
            getReservationById(reservationId);

        reservation.cancelReservation();

        System.out.println("Reservation cancelled successfully.");
    }

    // Remove a reservation from the manager
    public void deleteReservation(String reservationId) {

        Reservation reservation =
            getReservationById(reservationId);

        reservations.remove(reservationId);

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

        if (reservations.isEmpty()) {
            System.out.println("No reservations found.");
            return;
        }

        System.out.println("\n========== ALL RESERVATIONS ==========");

        for (Reservation reservation : reservations.values()) {
            reservation.displayReservation();
        }
    }

    // Display reservations belonging to a particular trip
    public void displayReservationsByTrip(String tripId) {

        boolean found = false;

        for (Reservation reservation : reservations.values()) {

            if (reservation.getTripId().equals(tripId)) {
                reservation.displayReservation();
                found = true;
            }
        }

        if (!found) {
            System.out.println(
                "No reservations found for this trip."
            );
        }
    }

    // Get all reservations without allowing external modification
    public Collection<Reservation> getAllReservations() {
        return Collections.unmodifiableCollection(
            reservations.values()
        );
    }

    // Get total reservation count
    public int getReservationCount() {
        return reservations.size();
    }

    // Get count of confirmed reservations
    public int getConfirmedReservationCount() {

        int count = 0;

        for (Reservation reservation : reservations.values()) {
            if (reservation.getStatus() == Reservation.Status.CONFIRMED) {
                count++;
            }
        }

        return count;
    }

    // Get total cost of active reservations for a trip
    public java.math.BigDecimal getTotalReservationCost(String tripId) {

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;

        for (Reservation reservation : reservations.values()) {

            if (reservation.getTripId().equals(tripId)
                    && reservation.getStatus() != Reservation.Status.CANCELLED) {

                total = total.add(reservation.getCost());
            }
        }

        return total;
    }
}
