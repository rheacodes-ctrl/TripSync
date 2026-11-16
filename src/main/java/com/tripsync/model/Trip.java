
package com.tripsync.model;

import com.tripsync.interfaces.Collaboratable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Trip implements Collaboratable {

    private final String tripId;
    private String tripName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;

    private final TripAdmin admin;
    private final List<Traveler> members;
    private final List<Destination> destinations;

    // Constructor for creating a new trip
    public Trip(String tripName, String description,
                LocalDate startDate, LocalDate endDate,
                BigDecimal budget, TripAdmin admin) {

        this(UUID.randomUUID().toString(), tripName, description,
                startDate, endDate, budget, admin);

        admin.incrementManagedTripCount();
    }

    // Constructor for loading an existing trip from database
    public Trip(String tripId, String tripName, String description,
                LocalDate startDate, LocalDate endDate,
                BigDecimal budget, TripAdmin admin) {

        if (tripId == null || tripId.isBlank()) {
            throw new IllegalArgumentException("Trip ID cannot be empty");
        }

        if (tripName == null || tripName.isBlank()) {
            throw new IllegalArgumentException("Trip name cannot be empty");
        }

        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid trip dates");
        }

        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }

        if (admin == null) {
            throw new IllegalArgumentException("Trip must have an admin");
        }

        this.tripId = tripId;
        this.tripName = tripName;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budget = budget;
        this.admin = admin;

        this.members = new ArrayList<>();
        this.destinations = new ArrayList<>();
    }

    // Getters
    public String getTripId() {
        return tripId;
    }

    public String getTripName() {
        return tripName;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public TripAdmin getAdmin() {
        return admin;
    }

    public List<Traveler> getMembers() {
        return new ArrayList<>(members);
    }

    public List<Destination> getDestinations() {
        return new ArrayList<>(destinations);
    }

    // Update trip name
    public void updateTripName(String tripName) {
        if (tripName == null || tripName.isBlank()) {
            throw new IllegalArgumentException("Trip name cannot be empty");
        }
        this.tripName = tripName;
    }

    // Update description
    public void updateDescription(String description) {
        this.description = description;
    }

    // Update dates
    public void updateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid trip dates");
        }

        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Update budget
    public void updateBudget(BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }
        this.budget = budget;
    }

    // Add traveler
    @Override
    public void addMember(Traveler traveler) {
        if (traveler == null) {
            throw new IllegalArgumentException("Traveler cannot be null");
        }

        boolean alreadyMember = members.stream()
                .anyMatch(member ->
                        member.getUserId().equals(traveler.getUserId()));

        if (alreadyMember) {
            throw new IllegalArgumentException(
                    "Traveler is already part of this trip");
        }

        members.add(traveler);
        traveler.joinTrip(tripId);
    }

    // Remove traveler
    @Override
    public void removeMember(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }

        boolean removed = members.removeIf(
                member -> member.getUserId().equals(userId));

        if (!removed) {
            throw new IllegalArgumentException(
                    "Traveler is not part of this trip");
        }
    }

    // Display collaborators
    @Override
    public void displayCollaborators() {
        System.out.println("\n----- TRIP COLLABORATORS -----");

        if (members.isEmpty()) {
            System.out.println("No collaborators added yet.");
            return;
        }

        for (Traveler traveler : members) {
            System.out.println(
                    traveler.getName() + " (" + traveler.getEmail() + ")");
        }
    }

    // Add destination
    public void addDestination(Destination destination) {
        if (destination == null) {
            throw new IllegalArgumentException(
                    "Destination cannot be null");
        }

        if (destination.getArrivalDate().isBefore(startDate)
                || destination.getDepartureDate().isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Destination dates must fall within trip dates");
        }

        destinations.add(destination);
    }

    // Display trip details
    public void displayTripDetails() {
        System.out.println("\n========== TRIP DETAILS ==========");
        System.out.println("Trip ID: " + tripId);
        System.out.println("Trip Name: " + tripName);
        System.out.println("Description: " + description);
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        System.out.println("Budget: Rs. " + budget);
        System.out.println("Admin: " + admin.getName());
        System.out.println("Members: " + members.size());
        System.out.println("Destinations: " + destinations.size());
        System.out.println("==================================");
    }
}
