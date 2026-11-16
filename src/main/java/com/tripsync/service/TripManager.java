
package com.tripsync.service;

import com.tripsync.dao.TripDAO;
import com.tripsync.dao.DestinationDAO;

import com.tripsync.model.Trip;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.Traveler;
import com.tripsync.model.Destination;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TripManager {

    private final TripDAO tripDAO;
    private final DestinationDAO destinationDAO;

    // Constructor
    public TripManager() {
        this.tripDAO = new TripDAO();
        this.destinationDAO = new DestinationDAO();
    }

    // ==========================================
    // 1. CREATE A NEW TRIP
    // ==========================================

    public Trip createTrip(
            String tripName,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal budget,
            TripAdmin admin
    ) {

        Trip trip = new Trip(
                tripName,
                description,
                startDate,
                endDate,
                budget,
                admin
        );

        try {
            boolean created = tripDAO.createTrip(trip);

            if (!created) {
                throw new IllegalStateException(
                        "Trip could not be saved."
                );
            }

            System.out.println("Trip created successfully!");
            System.out.println("Trip ID: " + trip.getTripId());

            return trip;

        } catch (SQLException e) {
            throw databaseError("creating trip", e);
        }
    }

    // ==========================================
    // 2. GET TRIP BY ID
    // ==========================================

    public Trip getTripById(String tripId) {

        try {
            Trip trip = tripDAO.getTripById(tripId);

            if (trip == null) {
                throw new IllegalArgumentException(
                        "Trip not found with ID: " + tripId
                );
            }

            // Load destinations from MySQL.
            List<Destination> destinations =
                    destinationDAO.getDestinationsByTripId(tripId);

            for (Destination destination : destinations) {
                trip.addDestination(destination);
            }

            return trip;

        } catch (SQLException e) {
            throw databaseError("retrieving trip", e);
        }
    }

    // ==========================================
    // 3. UPDATE TRIP NAME
    // ==========================================

    public void updateTripName(
            String tripId,
            String newName
    ) {

        Trip trip = getTripById(tripId);
        trip.updateTripName(newName);

        saveTrip(trip);

        System.out.println("Trip name updated successfully!");
    }

    // ==========================================
    // 4. UPDATE TRIP DESCRIPTION
    // ==========================================

    public void updateTripDescription(
            String tripId,
            String description
    ) {

        Trip trip = getTripById(tripId);
        trip.updateDescription(description);

        saveTrip(trip);

        System.out.println("Trip description updated successfully!");
    }

    // ==========================================
    // 5. UPDATE TRIP DATES
    // ==========================================

    public void updateTripDates(
            String tripId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        Trip trip = getTripById(tripId);
        trip.updateDates(startDate, endDate);

        saveTrip(trip);

        System.out.println("Trip dates updated successfully!");
    }

    // ==========================================
    // 6. UPDATE TRIP BUDGET
    // ==========================================

    public void updateTripBudget(
            String tripId,
            BigDecimal newBudget
    ) {

        Trip trip = getTripById(tripId);
        trip.updateBudget(newBudget);

        saveTrip(trip);

        System.out.println("Trip budget updated successfully!");
    }

    // ==========================================
    // 7. SAVE UPDATED TRIP
    // ==========================================

    private void saveTrip(Trip trip) {

        try {
            boolean updated = tripDAO.updateTrip(trip);

            if (!updated) {
                throw new IllegalStateException(
                        "Trip update failed."
                );
            }

        } catch (SQLException e) {
            throw databaseError("updating trip", e);
        }
    }

    // ==========================================
    // 8. DELETE TRIP
    // ==========================================

    public void deleteTrip(String tripId) {

        Trip trip = getTripById(tripId);

        try {
            boolean deleted = tripDAO.deleteTrip(tripId);

            if (!deleted) {
                throw new IllegalStateException(
                        "Trip could not be deleted."
                );
            }

            System.out.println(
                    "Trip '" + trip.getTripName()
                            + "' deleted successfully!"
            );

        } catch (SQLException e) {
            throw databaseError("deleting trip", e);
        }
    }

    // ==========================================
    // 9. ADD TRAVELER TO TRIP
    // ==========================================

    public void addTraveler(
            String tripId,
            Traveler traveler
    ) {

        if (traveler == null) {
            throw new IllegalArgumentException(
                    "Traveler cannot be null."
            );
        }

        Trip trip = getTripById(tripId);

        try {
            boolean added = tripDAO.addMember(
                    tripId,
                    traveler.getUserId()
            );

            if (!added) {
                throw new IllegalStateException(
                        "Traveler could not be added."
                );
            }

            // Keep the current Java object synchronized.
            trip.addMember(traveler);

            System.out.println(
                    traveler.getName() + " added to the trip!"
            );

        } catch (SQLException e) {
            throw databaseError("adding traveler", e);
        }
    }

    // ==========================================
    // 10. REMOVE TRAVELER FROM TRIP
    // ==========================================

    public void removeTraveler(
            String tripId,
            String userId
    ) {

        Trip trip = getTripById(tripId);

        try {
            boolean removed = tripDAO.removeMember(
                    tripId,
                    userId
            );

            if (!removed) {
                throw new IllegalArgumentException(
                        "Traveler is not a member of this trip."
                );
            }

            trip.removeMember(userId);

            System.out.println(
                    "Traveler removed successfully!"
            );

        } catch (SQLException e) {
            throw databaseError("removing traveler", e);
        }
    }

    // ==========================================
    // 11. DISPLAY ONE TRIP
    // ==========================================

    public void displayTrip(String tripId) {

        Trip trip = getTripById(tripId);
        trip.displayTripDetails();
    }

    // ==========================================
    // 12. DISPLAY ALL TRIPS
    // ==========================================

    public void displayAllTrips() {

        Collection<Trip> trips = getAllTrips();

        if (trips.isEmpty()) {
            System.out.println("No trips available.");
            return;
        }

        System.out.println("\n========== ALL TRIPS ==========");

        for (Trip trip : trips) {
            trip.displayTripDetails();
        }
    }

    // ==========================================
    // 13. GET ALL TRIPS FROM DATABASE
    // ==========================================

    public Collection<Trip> getAllTrips() {

        try {
            List<Trip> trips = tripDAO.getAllTrips();

            // Load destinations for every trip.
            for (Trip trip : trips) {

                List<Destination> destinations =
                        destinationDAO.getDestinationsByTripId(
                                trip.getTripId()
                        );

                for (Destination destination : destinations) {
                    trip.addDestination(destination);
                }
            }

            return Collections.unmodifiableList(trips);

        } catch (SQLException e) {
            throw databaseError("retrieving all trips", e);
        }
    }

    // ==========================================
    // 14. GET TOTAL NUMBER OF TRIPS
    // ==========================================

    public int getTripCount() {

        try {
            return tripDAO.getTripCount();

        } catch (SQLException e) {
            throw databaseError("counting trips", e);
        }
    }

    // ==========================================
    // 15. ADD DESTINATION TO A TRIP
    // ==========================================

    public void addDestination(
            String tripId,
            Destination destination
    ) {

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Destination cannot be null."
            );
        }

        Trip trip = getTripById(tripId);

        try {
            boolean added = destinationDAO.createDestination(
                    tripId,
                    destination
            );

            if (!added) {
                throw new IllegalStateException(
                        "Destination could not be saved."
                );
            }

            // Synchronize the existing Trip object.
            trip.addDestination(destination);

            System.out.println(
                    destination.getName() + " added to the trip!"
            );

        } catch (SQLException e) {
            throw databaseError("adding destination", e);
        }
    }

    // ==========================================
    // 16. DISPLAY DESTINATIONS OF A TRIP
    // ==========================================

    public void displayDestinations(String tripId) {

        try {
            List<Destination> destinations =
                    destinationDAO.getDestinationsByTripId(tripId);

            if (destinations.isEmpty()) {
                System.out.println("No destinations added yet.");
                return;
            }

            System.out.println("\n========== ITINERARY ==========");

            for (Destination destination : destinations) {
                destination.displayDestination();
            }

        } catch (SQLException e) {
            throw databaseError("retrieving destinations", e);
        }
    }

    // ==========================================
    // DATABASE ERROR HANDLING
    // ==========================================

    private IllegalStateException databaseError(
            String operation,
            SQLException cause
    ) {

        return new IllegalStateException(
                "Database error while " + operation
                        + ": " + cause.getMessage(),
                cause
        );
    }
}
