
package com.tripsync.dao;

import com.tripsync.model.Trip;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.Traveler;
import com.tripsync.model.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public class TripDAOTest {

    public static void main(String[] args) {

        UserDAO userDAO = new UserDAO();
        TripDAO tripDAO = new TripDAO();

        String tripId = null;

        try {

            // 1. Retrieve existing admin and traveler
            User adminUser = userDAO.getUserById("U001");
            User travelerUser = userDAO.getUserById("U002");

            if (!(adminUser instanceof TripAdmin)) {
                throw new IllegalStateException(
                        "U001 must be a Trip Administrator");
            }

            if (!(travelerUser instanceof Traveler)) {
                throw new IllegalStateException(
                        "U002 must be a Traveler");
            }

            TripAdmin admin = (TripAdmin) adminUser;
            Traveler traveler = (Traveler) travelerUser;

            // 2. CREATE TRIP
            System.out.println("\n========== CREATE TRIP ==========");

            Trip trip = new Trip(
                    "JDBC Test Trip " + UUID.randomUUID().toString()
                            .substring(0, 8),
                    "Testing TripDAO database operations",
                    LocalDate.of(2027, 2, 10),
                    LocalDate.of(2027, 2, 15),
                    new BigDecimal("40000.00"),
                    admin
            );

            tripId = trip.getTripId();

            boolean created = tripDAO.createTrip(trip);

            System.out.println("Trip created: " + created);
            System.out.println("Trip ID: " + tripId);

            // 3. ADD MEMBER
            System.out.println("\n========== ADD MEMBER ==========");

            boolean memberAdded =
                    tripDAO.addMember(tripId, traveler.getUserId());

            System.out.println("Member added: " + memberAdded);

            // 4. RETRIEVE TRIP
            System.out.println("\n========== RETRIEVE TRIP ==========");

            Trip retrievedTrip = tripDAO.getTripById(tripId);

            if (retrievedTrip != null) {
                retrievedTrip.displayTripDetails();
                retrievedTrip.displayCollaborators();
            }

            // 5. UPDATE TRIP
            System.out.println("\n========== UPDATE TRIP ==========");

            retrievedTrip.updateTripName("Updated JDBC Test Trip");
            retrievedTrip.updateBudget(new BigDecimal("45000.00"));

            boolean updated = tripDAO.updateTrip(retrievedTrip);

            System.out.println("Trip updated: " + updated);

            // 6. GET ALL TRIPS
            System.out.println("\n========== ALL TRIPS ==========");

            for (Trip currentTrip : tripDAO.getAllTrips()) {
                System.out.println(
                        currentTrip.getTripName()
                                + " | Budget: "
                                + currentTrip.getBudget()
                );
            }

            System.out.println(
                    "Total trips: " + tripDAO.getTripCount()
            );

            // 7. REMOVE MEMBER
            System.out.println("\n========== REMOVE MEMBER ==========");

            boolean memberRemoved =
                    tripDAO.removeMember(tripId, traveler.getUserId());

            System.out.println("Member removed: " + memberRemoved);

            // 8. DELETE TEST TRIP
            System.out.println("\n========== DELETE TEST TRIP ==========");

            boolean deleted = tripDAO.deleteTrip(tripId);

            System.out.println("Trip deleted: " + deleted);

            System.out.println("\n========== TRIP DAO TEST COMPLETED ==========");

        } catch (SQLException e) {

            System.err.println("Database error:");
            e.printStackTrace();

        } catch (Exception e) {

            System.err.println("Test failed:");
            e.printStackTrace();
        }
    }
}
