package com.tripsync.service;

import com.tripsync.dao.ReservationDAO;

import com.tripsync.reservation.ActivityReservation;
import com.tripsync.reservation.HotelReservation;
import com.tripsync.reservation.Reservation;
import com.tripsync.reservation.TransportReservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationManagerTest {

    public static void main(String[] args) throws Exception {

        ReservationDAO reservationDAO = new ReservationDAO();
        ReservationManager reservationManager =
            new ReservationManager();

        // Seeded trip used as context. Never modified or deleted.
        String tripId = "T001";

        // Seeded reservation count for trip T001 is 3 (R001..R003).
        int seededCount = 3;
        BigDecimal seededNonCancelledCost =
            new BigDecimal("22000.00"); // 12000 + 8500 + 1500

        String hotelId = null;
        String transportId = null;
        String activityId = null;

        System.out.println("========== RESERVATION MANAGER INTEGRATION TEST ==========");

        try {

            // ==========================================
            // 1. CREATE RESERVATIONS (PERSIST IN MYSQL)
            // ==========================================

            System.out.println("\n========== CREATE HOTEL RESERVATION ==========");

            HotelReservation hotel = new HotelReservation(
                tripId,
                "JDBC Test Hotel",
                new BigDecimal("8000.00"),
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 12),
                2
            );

            hotelId = hotel.getReservationId();

            reservationManager.addReservation(hotel);

            System.out.println(
                "Hotel exists in database: "
                + (reservationDAO.getReservationById(hotelId) != null)
            );

            System.out.println("\n========== CREATE TRANSPORT RESERVATION ==========");

            TransportReservation transport = new TransportReservation(
                tripId,
                "IndiGo",
                new BigDecimal("5500.00"),
                LocalDate.of(2027, 1, 10),
                TransportReservation.TransportType.FLIGHT,
                "Delhi",
                "Goa",
                LocalTime.of(9, 30),
                "JDBC-FLIGHT-001"
            );

            transportId = transport.getReservationId();

            reservationManager.addReservation(transport);

            System.out.println(
                "Transport exists in database: "
                + (reservationDAO.getReservationById(transportId) != null)
            );

            System.out.println("\n========== CREATE ACTIVITY RESERVATION ==========");

            ActivityReservation activityBooking = new ActivityReservation(
                tripId,
                "Scuba Diving",
                new BigDecimal("2500.00"),
                LocalDate.of(2027, 1, 11),
                "Baga Beach",
                LocalTime.of(11, 0),
                2
            );

            activityId = activityBooking.getReservationId();

            reservationManager.addReservation(activityBooking);

            System.out.println(
                "Activity reservation exists in database: "
                + (reservationDAO.getReservationById(activityId) != null)
            );

            System.out.println(
                "Reservation count after inserts: "
                + reservationManager.getReservationCount()
            );

            if (reservationManager.getReservationCount()
                    != seededCount + 3) {
                throw new IllegalStateException(
                    "Reservation count did not increase by 3."
                );
            }

            // ==========================================
            // 2. DUPLICATE GUARD
            // ==========================================

            System.out.println("\n========== DUPLICATE GUARD ==========");

            boolean duplicateRejected = false;

            try {
                reservationManager.addReservation(hotel);
            } catch (IllegalArgumentException expected) {
                duplicateRejected = true;
                System.out.println(
                    "Duplicate rejected: " + expected.getMessage()
                );
            }

            if (!duplicateRejected) {
                throw new IllegalStateException(
                    "Duplicate reservation was not rejected."
                );
            }

            // ==========================================
            // 3. RETRIEVE AND VERIFY POLYMORPHIC TYPE
            // ==========================================

            System.out.println("\n========== RETRIEVE RESERVATIONS ==========");

            Reservation retrievedHotel =
                reservationManager.getReservationById(hotelId);

            if (!(retrievedHotel instanceof HotelReservation)) {
                throw new IllegalStateException(
                    "Hotel reservation type was not preserved."
                );
            }

            System.out.println("Hotel class: " + retrievedHotel.getClass().getSimpleName());

            Reservation retrievedTransport =
                reservationManager.getReservationById(transportId);

            if (!(retrievedTransport instanceof TransportReservation)) {
                throw new IllegalStateException(
                    "Transport reservation type was not preserved."
                );
            }

            System.out.println(
                "Transport class: "
                + retrievedTransport.getClass().getSimpleName()
            );

            Reservation retrievedActivity =
                reservationManager.getReservationById(activityId);

            if (!(retrievedActivity instanceof ActivityReservation)) {
                throw new IllegalStateException(
                    "Activity reservation type was not preserved."
                );
            }

            System.out.println(
                "Activity class: "
                + retrievedActivity.getClass().getSimpleName()
            );

            // ==========================================
            // 4. CONFIRM AND CANCEL (PERSIST STATUS)
            // ==========================================

            System.out.println("\n========== CONFIRM RESERVATION ==========");

            // New reservations start as PENDING.
            if (retrievedHotel.getStatus() != Reservation.Status.PENDING) {
                throw new IllegalStateException(
                    "New reservation was expected to be PENDING."
                );
            }

            reservationManager.confirmReservation(hotelId);

            Reservation confirmedHotel =
                reservationManager.getReservationById(hotelId);

            System.out.println(
                "Hotel status after confirm: "
                + confirmedHotel.getStatus()
            );

            if (confirmedHotel.getStatus() != Reservation.Status.CONFIRMED) {
                throw new IllegalStateException(
                    "Hotel confirmation was not persisted."
                );
            }

            System.out.println("\n========== CANCEL RESERVATION ==========");

            reservationManager.cancelReservation(transportId);

            Reservation cancelledTransport =
                reservationManager.getReservationById(transportId);

            System.out.println(
                "Transport status after cancel: "
                + cancelledTransport.getStatus()
            );

            if (cancelledTransport.getStatus() != Reservation.Status.CANCELLED) {
                throw new IllegalStateException(
                    "Transport cancellation was not persisted."
                );
            }

            System.out.println("\n========== CANCEL GUARD ==========");

            boolean cancelTwiceRejected = false;

            try {
                reservationManager.cancelReservation(transportId);
            } catch (IllegalStateException expected) {
                cancelTwiceRejected = true;
                System.out.println(
                    "Double cancel rejected: " + expected.getMessage()
                );
            }

            if (!cancelTwiceRejected) {
                throw new IllegalStateException(
                    "Cancelling an already-cancelled reservation was not rejected."
                );
            }

            System.out.println("\n========== CONFIRM CANCELLED GUARD ==========");

            boolean confirmCancelledRejected = false;

            try {
                reservationManager.confirmReservation(transportId);
            } catch (IllegalStateException expected) {
                confirmCancelledRejected = true;
                System.out.println(
                    "Confirming a cancelled reservation rejected: "
                    + expected.getMessage()
                );
            }

            if (!confirmCancelledRejected) {
                throw new IllegalStateException(
                    "Confirming a cancelled reservation was not rejected."
                );
            }

            // ==========================================
            // 5. STATISTICS
            // ==========================================

            System.out.println("\n========== STATISTICS ==========");

            int reservationCount = reservationManager.getReservationCount();
            int confirmedCount = reservationManager.getConfirmedReservationCount();
            BigDecimal totalCost = reservationManager.getTotalReservationCost(tripId);

            System.out.println("Total reservations: " + reservationCount);
            System.out.println("Confirmed reservations: " + confirmedCount);
            System.out.println("Active reservation cost for T001: Rs. " + totalCost);

            // Seeded (3 confirmed) + hotel (confirmed) = 4 confirmed.
            if (confirmedCount != 4) {
                throw new IllegalStateException(
                    "Expected 4 confirmed reservations, got " + confirmedCount
                );
            }

            // Seeded 22000 + hotel 8000 + activity 2500 (transport cancelled excluded).
            BigDecimal expectedCost =
                seededNonCancelledCost
                    .add(new BigDecimal("8000.00"))
                    .add(new BigDecimal("2500.00"));

            if (totalCost.compareTo(expectedCost) != 0) {
                throw new IllegalStateException(
                    "Expected active cost Rs. " + expectedCost
                        + ", got Rs. " + totalCost
                );
            }

            // ==========================================
            // 6. DISPLAY
            // ==========================================

            System.out.println("\n========== DISPLAY ==========");

            reservationManager.displayReservation(hotelId);
            reservationManager.displayAllReservations();
            reservationManager.displayReservationsByTrip(tripId);

            // ==========================================
            // 7. DELETE (PERSIST IN MYSQL)
            // ==========================================

            System.out.println("\n========== DELETE RESERVATION ==========");

            reservationManager.deleteReservation(activityId);

            boolean activityStillExists =
                reservationDAO.getReservationById(activityId) != null;

            System.out.println(
                "Activity reservation exists after deletion: "
                + activityStillExists
            );

            if (activityStillExists) {
                throw new IllegalStateException(
                    "Activity reservation was not deleted."
                );
            }

            boolean notFoundRejected = false;

            try {
                reservationManager.getReservationById(activityId);
            } catch (IllegalArgumentException expected) {
                notFoundRejected = true;
                System.out.println(
                    "Retrieve after delete rejected: " + expected.getMessage()
                );
            }

            if (!notFoundRejected) {
                throw new IllegalStateException(
                    "Retrieving a deleted reservation did not fail."
                );
            }

            activityId = null;

            System.out.println(
                "\n========== RESERVATION MANAGER INTEGRATION TEST COMPLETED =========="
            );

        } catch (Exception e) {

            System.out.println("\nRESERVATION MANAGER TEST FAILED!");
            e.printStackTrace();

            throw e;

        } finally {

            // Best-effort cleanup using the DAO directly so a
            // missing row does not throw during cleanup.
            try {
                if (hotelId != null) {
                    reservationDAO.deleteReservation(hotelId);
                }

                if (transportId != null) {
                    reservationDAO.deleteReservation(transportId);
                }

                if (activityId != null) {
                    reservationDAO.deleteReservation(activityId);
                }

                System.out.println(
                    "Temporary reservations cleanup attempted."
                );

            } catch (Exception cleanupError) {
                System.err.println("Test cleanup failed:");
                cleanupError.printStackTrace();
            }
        }
    }
}
