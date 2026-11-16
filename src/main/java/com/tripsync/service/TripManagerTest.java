
package com.tripsync.service;

import com.tripsync.dao.UserDAO;
import com.tripsync.dao.TripDAO;
import com.tripsync.dao.DestinationDAO;

import com.tripsync.model.User;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.Traveler;
import com.tripsync.model.Trip;
import com.tripsync.model.Destination;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TripManagerTest {

    public static void main(String[] args) throws Exception {

        UserDAO userDAO = new UserDAO();
        TripDAO tripDAO = new TripDAO();
        DestinationDAO destinationDAO = new DestinationDAO();

        TripManager tripManager = new TripManager();

        String tripId = null;
        String destinationId = null;

        System.out.println("========== TRIP MANAGER INTEGRATION TEST ==========");

        try {

            // ==========================================
            // 1. RETRIEVE EXISTING DATABASE USERS
            // ==========================================

            System.out.println("\n========== RETRIEVE USERS ==========");

            User adminUser = userDAO.getUserById("U001");
            User travelerUser = userDAO.getUserById("U002");

            if (!(adminUser instanceof TripAdmin)
                    || !(travelerUser instanceof Traveler)) {
                throw new IllegalStateException(
                    "Seed users U001 and U002 were not found "
                    + "with the expected roles."
                );
            }

            TripAdmin admin = (TripAdmin) adminUser;
            Traveler traveler = (Traveler) travelerUser;

            System.out.println("Admin: " + admin.getName());
            System.out.println("Traveler: " + traveler.getName());

            // ==========================================
            // 2. CREATE TRIP IN MYSQL
            // ==========================================

            System.out.println("\n========== CREATE TRIP ==========");

            Trip trip = tripManager.createTrip(
                "JDBC Integration Test Trip",
                "Testing database-backed TripManager",
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 2, 5),
                new BigDecimal("25000"),
                admin
            );

            tripId = trip.getTripId();

            System.out.println(
                "Trip exists in database: "
                + (tripDAO.getTripById(tripId) != null)
            );

            // ==========================================
            // 3. ADD TRAVELER
            // ==========================================

            System.out.println("\n========== ADD TRAVELER ==========");

            tripManager.addTraveler(tripId, traveler);

            Trip tripAfterMemberAddition =
                    tripManager.getTripById(tripId);

            System.out.println(
                "Member count after addition: "
                + tripAfterMemberAddition.getMembers().size()
            );

            // ==========================================
            // 4. ADD DESTINATION
            // ==========================================

            System.out.println("\n========== ADD DESTINATION ==========");

            Destination destination = new Destination(
                "Test Jaipur",
                "Jaipur",
                "India",
                "Destination created by integration test",
                LocalDate.of(2027, 2, 2),
                LocalDate.of(2027, 2, 3)
            );

            tripManager.addDestination(tripId, destination);

            destinationId = destination.getDestinationId();

            System.out.println(
                "Destination exists in database: "
                + (destinationDAO.getDestinationById(destinationId) != null)
            );

            // ==========================================
            // 5. UPDATE TRIP
            // ==========================================

            System.out.println("\n========== UPDATE TRIP ==========");

            tripManager.updateTripName(
                tripId,
                "Updated JDBC Integration Trip"
            );

            tripManager.updateTripBudget(
                tripId,
                new BigDecimal("30000")
            );

            Trip updatedTrip = tripManager.getTripById(tripId);

            System.out.println(
                "Updated name: " + updatedTrip.getTripName()
            );

            System.out.println(
                "Updated budget: Rs. " + updatedTrip.getBudget()
            );

            // ==========================================
            // 6. VERIFY PERSISTENCE
            // ==========================================

            System.out.println("\n========== VERIFY DATABASE PERSISTENCE ==========");

            Trip retrievedTrip = tripDAO.getTripById(tripId);

            System.out.println(
                "Retrieved trip: " + retrievedTrip.getTripName()
            );

            System.out.println(
                "Retrieved members: "
                + retrievedTrip.getMembers().size()
            );

            System.out.println(
                "Retrieved destinations: "
                + destinationDAO.getDestinationCount(tripId)
            );

            System.out.println(
                "Trip count from database: "
                + tripManager.getTripCount()
            );

            // ==========================================
            // 7. DISPLAY TRIP
            // ==========================================

            System.out.println("\n========== DISPLAY TRIP ==========");

            tripManager.displayTrip(tripId);
            tripManager.displayDestinations(tripId);

            // ==========================================
            // 8. DELETE TEST RECORDS
            // ==========================================

            System.out.println("\n========== CLEANUP TEST DATA ==========");

            if (destinationId != null) {
                System.out.println(
                    "Destination deleted: "
                    + destinationDAO.deleteDestination(destinationId)
                );
                destinationId = null;
            }

            if (tripId != null) {
                tripManager.deleteTrip(tripId);
                System.out.println(
                    "Trip exists after deletion: "
                    + (tripDAO.getTripById(tripId) != null)
                );
                tripId = null;
            }

            System.out.println("\nTRIP MANAGER INTEGRATION TEST COMPLETED");

        } finally {

            // Best-effort cleanup if a test fails midway.
            if (destinationId != null) {
                destinationDAO.deleteDestination(destinationId);
            }

            if (tripId != null) {
                tripDAO.deleteTrip(tripId);
            }
        }
    }
}
