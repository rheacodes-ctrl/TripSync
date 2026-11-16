
package com.tripsync.dao;

import com.tripsync.reservation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ReservationDAOTest {

    public static void main(String[] args) {

        ReservationDAO dao = new ReservationDAO();

        // Existing seeded trip
        String tripId = "T001";

        String hotelId = null;
        String transportId = null;
        String activityId = null;

        try {

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

            System.out.println("Hotel inserted: " +
                    dao.addReservation(hotel));
            System.out.println("Hotel ID: " + hotelId);


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

            System.out.println("Transport inserted: " +
                    dao.addReservation(transport));
            System.out.println("Transport ID: " + transportId);


            System.out.println("\n========== CREATE ACTIVITY RESERVATION ==========");

            ActivityReservation activity = new ActivityReservation(
                    tripId,
                    "Scuba Diving",
                    new BigDecimal("2500.00"),
                    LocalDate.of(2027, 1, 11),
                    "Baga Beach",
                    LocalTime.of(11, 0),
                    2
            );

            activityId = activity.getReservationId();

            System.out.println("Activity inserted: " +
                    dao.addReservation(activity));
            System.out.println("Activity ID: " + activityId);


            System.out.println("\n========== RETRIEVE HOTEL ==========");

            Reservation retrievedHotel =
                    dao.getReservationById(hotelId);

            retrievedHotel.displayReservation();

            System.out.println("Correct class: " +
                    (retrievedHotel instanceof HotelReservation));


            System.out.println("\n========== RETRIEVE TRANSPORT ==========");

            Reservation retrievedTransport =
                    dao.getReservationById(transportId);

            retrievedTransport.displayReservation();

            System.out.println("Correct class: " +
                    (retrievedTransport instanceof TransportReservation));


            System.out.println("\n========== RETRIEVE ACTIVITY ==========");

            Reservation retrievedActivity =
                    dao.getReservationById(activityId);

            retrievedActivity.displayReservation();

            System.out.println("Correct class: " +
                    (retrievedActivity instanceof ActivityReservation));


            System.out.println("\n========== ALL TRIP RESERVATIONS ==========");

            List<Reservation> reservations =
                    dao.getReservationsByTripId(tripId);

            for (Reservation reservation : reservations) {
                System.out.println(
                        reservation.getReservationType()
                                + " | "
                                + reservation.getProviderName()
                                + " | Rs. "
                                + reservation.getCost()
                                + " | "
                                + reservation.getStatus()
                );
            }

            System.out.println("Total reservations: " +
                    dao.getReservationCount(tripId));


            System.out.println("\n========== UPDATE STATUS ==========");

            System.out.println("Confirmed: " +
                    dao.updateReservationStatus(
                            hotelId,
                            Reservation.Status.CONFIRMED
                    ));

            System.out.println("Updated status: " +
                    dao.getReservationById(hotelId).getStatus());


            System.out.println("\n========== UPDATE COST ==========");

            System.out.println("Cost updated: " +
                    dao.updateReservationCost(
                            hotelId,
                            new BigDecimal("9000.00")
                    ));

            System.out.println("New hotel cost: " +
                    dao.getReservationById(hotelId).getCost());


            System.out.println("\n========== TOTAL RESERVATION COST ==========");

            System.out.println("Total: Rs. " +
                    dao.getTotalReservationCost(tripId));


            System.out.println("\n========== DELETE TEST RESERVATIONS ==========");

            System.out.println("Hotel deleted: " +
                    dao.deleteReservation(hotelId));

            System.out.println("Transport deleted: " +
                    dao.deleteReservation(transportId));

            System.out.println("Activity deleted: " +
                    dao.deleteReservation(activityId));

            System.out.println("\n========== RESERVATION DAO TEST COMPLETED ==========");

        } catch (Exception e) {

            System.err.println("Reservation DAO test failed:");
            e.printStackTrace();

        } finally {

            // Clean up test data if an earlier operation failed
            try {
                if (hotelId != null) {
                    dao.deleteReservation(hotelId);
                }

                if (transportId != null) {
                    dao.deleteReservation(transportId);
                }

                if (activityId != null) {
                    dao.deleteReservation(activityId);
                }

            } catch (Exception cleanupError) {
                System.err.println("Test cleanup failed:");
                cleanupError.printStackTrace();
            }
        }
    }
}
