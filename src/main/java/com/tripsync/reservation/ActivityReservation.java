
package com.tripsync.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class ActivityReservation extends Reservation {

    private String activityName;
    private String location;
    private LocalTime activityTime;
    private int numberOfParticipants;

    public ActivityReservation(
            String tripId,
            String activityName,
            BigDecimal cost,
            LocalDate reservationDate,
            String location,
            LocalTime activityTime,
            int numberOfParticipants) {

        super(tripId, activityName, cost, reservationDate);

        validate(activityName, location,
                activityTime, numberOfParticipants);

        this.activityName = activityName;
        this.location = location;
        this.activityTime = activityTime;
        this.numberOfParticipants = numberOfParticipants;
    }

    // Database-loading constructor
    public ActivityReservation(
            String reservationId,
            String tripId,
            String activityName,
            BigDecimal cost,
            LocalDate reservationDate,
            Status status,
            String location,
            LocalTime activityTime,
            int numberOfParticipants) {

        super(reservationId, tripId, activityName,
                cost, reservationDate, status);

        validate(activityName, location,
                activityTime, numberOfParticipants);

        this.activityName = activityName;
        this.location = location;
        this.activityTime = activityTime;
        this.numberOfParticipants = numberOfParticipants;
    }

    private void validate(String name, String location,
                          LocalTime time, int participants) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Activity name cannot be empty.");
        }

        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException(
                    "Location cannot be empty.");
        }

        if (time == null || participants <= 0) {
            throw new IllegalArgumentException(
                    "Valid activity time and participant count are required.");
        }
    }

    public String getActivityName() {
        return activityName;
    }

    public String getLocation() {
        return location;
    }

    public LocalTime getActivityTime() {
        return activityTime;
    }

    public int getNumberOfParticipants() {
        return numberOfParticipants;
    }

    @Override
    public String getReservationType() {
        return "Activity";
    }

    @Override
    public void displayReservation() {
        System.out.println("\n----- ACTIVITY RESERVATION -----");
        System.out.println("Reservation ID: " + getReservationId());
        System.out.println("Activity: " + activityName);
        System.out.println("Location: " + location);
        System.out.println("Date: " + getReservationDate());
        System.out.println("Time: " + activityTime);
        System.out.println("Participants: " + numberOfParticipants);
        System.out.println("Cost: Rs. " + getCost());
        System.out.println("Status: " + getStatus());
    }
}
