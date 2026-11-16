
package com.tripsync.service;

import com.tripsync.model.Trip;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.Traveler;
import com.tripsync.model.Destination;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TripManager {

    // Stores all trips using their unique IDs
    private final Map<String, Trip> trips;

    // Constructor
    public TripManager() {
        this.trips = new LinkedHashMap<>();
    }

    // Create a new trip
    public Trip createTrip(String tripName, String description,
                           LocalDate startDate, LocalDate endDate,
                           BigDecimal budget, TripAdmin admin) {

        Trip trip = new Trip(
                tripName, description,
                startDate, endDate, budget, admin
        );

        trips.put(trip.getTripId(), trip);

        System.out.println("Trip created successfully!");
        System.out.println("Trip ID: " + trip.getTripId());

        return trip;
    }

    // Find a trip using its ID
    public Trip getTripById(String tripId) {

        Trip trip = trips.get(tripId);

        if (trip == null) {
            throw new IllegalArgumentException(
                    "Trip not found with ID: " + tripId);
        }

        return trip;
    }

    // Update trip name
    public void updateTripName(String tripId, String newName) {

        Trip trip = getTripById(tripId);
        trip.updateTripName(newName);

        System.out.println("Trip name updated successfully!");
    }

    // Update trip description
    public void updateTripDescription(
            String tripId, String description) {

        Trip trip = getTripById(tripId);
        trip.updateDescription(description);

        System.out.println("Trip description updated successfully!");
    }

    // Update trip dates
    public void updateTripDates(
            String tripId, LocalDate startDate, LocalDate endDate) {

        Trip trip = getTripById(tripId);
        trip.updateDates(startDate, endDate);

        System.out.println("Trip dates updated successfully!");
    }

    // Update trip budget
    public void updateTripBudget(
            String tripId, BigDecimal newBudget) {

        Trip trip = getTripById(tripId);
        trip.updateBudget(newBudget);

        System.out.println("Trip budget updated successfully!");
    }

    // Delete a trip
    public void deleteTrip(String tripId) {

        Trip trip = getTripById(tripId);

        trips.remove(tripId);

        System.out.println(
                "Trip '" + trip.getTripName()
                        + "' deleted successfully!");
    }

    // Add traveler to a trip
    public void addTraveler(String tripId, Traveler traveler) {

        Trip trip = getTripById(tripId);
        trip.addMember(traveler);

        System.out.println(
                traveler.getName() + " added to the trip!");
    }

    // Remove traveler from a trip
    public void removeTraveler(String tripId, String userId) {

        Trip trip = getTripById(tripId);
        trip.removeMember(userId);

        System.out.println("Traveler removed successfully!");
    }

    // Display one trip
    public void displayTrip(String tripId) {

        Trip trip = getTripById(tripId);
        trip.displayTripDetails();
    }

    // Display all trips
    public void displayAllTrips() {

        if (trips.isEmpty()) {
            System.out.println("No trips available.");
            return;
        }

        System.out.println("\n========== ALL TRIPS ==========");

        for (Trip trip : trips.values()) {
            trip.displayTripDetails();
        }
    }

    // Get all trips
    public Collection<Trip> getAllTrips() {
        return java.util.Collections.unmodifiableCollection(
                trips.values());
    }

    // Get total number of trips
    public int getTripCount() {
        return trips.size();
    }
    // Add destination to a trip
public void addDestination(String tripId, Destination destination) {

    Trip trip = getTripById(tripId);
    trip.addDestination(destination);

    System.out.println(
            destination.getName() + " added to the trip!");
}
// Display all destinations of a trip
public void displayDestinations(String tripId) {

    Trip trip = getTripById(tripId);

    if (trip.getDestinations().isEmpty()) {
        System.out.println("No destinations added yet.");
        return;
    }

    System.out.println("\n========== ITINERARY ==========");

    for (Destination destination : trip.getDestinations()) {
        destination.displayDestination();
    }
}
}
