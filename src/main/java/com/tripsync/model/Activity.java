
package com.tripsync.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Activity {

    public enum Status {
        PLANNED,
        COMPLETED,
        CANCELLED
    }

    private final String activityId;
    private String name;
    private String description;
    private LocalDate activityDate;
    private LocalTime activityTime;
    private BigDecimal estimatedCost;
    private Status status;

    private final List<Traveler> participants;
    private final Set<String> voterIds;

    // Constructor for creating a new activity
    public Activity(String name, String description,
                    LocalDate activityDate, LocalTime activityTime,
                    BigDecimal estimatedCost) {

        this(UUID.randomUUID().toString(), name, description,
             activityDate, activityTime, estimatedCost, Status.PLANNED);
    }

    // Constructor for loading an existing activity from MySQL
    public Activity(String activityId, String name, String description,
                    LocalDate activityDate, LocalTime activityTime,
                    BigDecimal estimatedCost, Status status) {

        if (activityId == null || activityId.isBlank()) {
            throw new IllegalArgumentException("Activity ID cannot be empty");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Activity name cannot be empty");
        }

        if (activityDate == null || activityTime == null) {
            throw new IllegalArgumentException(
                    "Activity date and time are required");
        }

        if (estimatedCost == null ||
                estimatedCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Activity cost cannot be negative");
        }

        if (status == null) {
            throw new IllegalArgumentException("Activity status cannot be null");
        }

        this.activityId = activityId;
        this.name = name;
        this.description = description;
        this.activityDate = activityDate;
        this.activityTime = activityTime;
        this.estimatedCost = estimatedCost;
        this.status = status;

        this.participants = new ArrayList<>();
        this.voterIds = new HashSet<>();
    }

    // Getters
    public String getActivityId() {
        return activityId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public LocalTime getActivityTime() {
        return activityTime;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public Status getStatus() {
        return status;
    }

    public List<Traveler> getParticipants() {
        return new ArrayList<>(participants);
    }

    public int getVoteCount() {
        return voterIds.size();
    }

    // Update activity details
    public void updateDetails(String name, String description) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Activity name cannot be empty");
        }

        this.name = name;
        this.description = description;
    }

    // Update activity schedule
    public void updateSchedule(LocalDate date, LocalTime time) {

        if (date == null || time == null) {
            throw new IllegalArgumentException(
                    "Date and time are required");
        }

        this.activityDate = date;
        this.activityTime = time;
    }

    // Update estimated cost
    public void updateEstimatedCost(BigDecimal cost) {

        if (cost == null ||
                cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }

        this.estimatedCost = cost;
    }

    // Assign traveler to activity
    public void addParticipant(Traveler traveler) {

        if (traveler == null) {
            throw new IllegalArgumentException("Traveler cannot be null");
        }

        boolean alreadyAdded = participants.stream()
                .anyMatch(p -> p.getUserId().equals(traveler.getUserId()));

        if (alreadyAdded) {
            throw new IllegalArgumentException(
                    "Traveler is already assigned");
        }

        participants.add(traveler);
    }

    // Remove participant
    public void removeParticipant(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        boolean removed = participants.removeIf(
                p -> p.getUserId().equals(userId));

        if (!removed) {
            throw new IllegalArgumentException("Participant not found");
        }

        voterIds.remove(userId);
    }

    // Vote for activity
    public void vote(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Invalid voter ID");
        }

        boolean isParticipant = participants.stream()
                .anyMatch(p -> p.getUserId().equals(userId));

        if (!isParticipant) {
            throw new IllegalArgumentException(
                    "Only assigned participants can vote");
        }

        if (!voterIds.add(userId)) {
            throw new IllegalArgumentException(
                    "You have already voted for this activity");
        }
    }

    // Update activity status
    public void updateStatus(Status status) {

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        this.status = status;
    }

    // Display activity
    public void displayActivity() {

        System.out.println("\n----- ACTIVITY DETAILS -----");
        System.out.println("Activity ID: " + activityId);
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Date: " + activityDate);
        System.out.println("Time: " + activityTime);
        System.out.println("Estimated Cost: Rs. " + estimatedCost);
        System.out.println("Status: " + status);
        System.out.println("Participants: " + participants.size());
        System.out.println("Votes: " + voterIds.size());
    }
}
