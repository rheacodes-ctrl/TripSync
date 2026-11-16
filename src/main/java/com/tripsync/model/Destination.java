
package com.tripsync.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Destination {

    private final String destinationId;
    private String name;
    private String city;
    private String country;
    private String description;
    private LocalDate arrivalDate;
    private LocalDate departureDate;

    private final List<Activity> activities;

    // Constructor for creating a new destination
    public Destination(String name, String city, String country,
                       String description,
                       LocalDate arrivalDate,
                       LocalDate departureDate) {

        this(UUID.randomUUID().toString(), name, city, country,
             description, arrivalDate, departureDate);
    }

    // Constructor for loading a destination from MySQL
    public Destination(String destinationId,
                       String name, String city, String country,
                       String description,
                       LocalDate arrivalDate,
                       LocalDate departureDate) {

        if (destinationId == null || destinationId.isBlank()) {
            throw new IllegalArgumentException("Destination ID cannot be empty");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Destination name cannot be empty");
        }

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be empty");
        }

        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be empty");
        }

        if (arrivalDate == null || departureDate == null ||
                departureDate.isBefore(arrivalDate)) {
            throw new IllegalArgumentException("Invalid destination dates");
        }

        this.destinationId = destinationId;
        this.name = name;
        this.city = city;
        this.country = country;
        this.description = description;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;

        this.activities = new ArrayList<>();
    }

    // Getters
    public String getDestinationId() {
        return destinationId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public List<Activity> getActivities() {
        return new ArrayList<>(activities);
    }

    // Update description
    public void updateDescription(String description) {
        this.description = description;
    }

    // Update dates
    public void updateDates(LocalDate arrivalDate,
                            LocalDate departureDate) {

        if (arrivalDate == null || departureDate == null ||
                departureDate.isBefore(arrivalDate)) {
            throw new IllegalArgumentException("Invalid destination dates");
        }

        for (Activity activity : activities) {
            if (activity.getActivityDate().isBefore(arrivalDate) ||
                    activity.getActivityDate().isAfter(departureDate)) {
                throw new IllegalArgumentException(
                        "New dates would exclude an existing activity");
            }
        }

        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
    }

    // Add activity
    public void addActivity(Activity activity) {

        if (activity == null) {
            throw new IllegalArgumentException("Activity cannot be null");
        }

        if (activity.getActivityDate().isBefore(arrivalDate) ||
                activity.getActivityDate().isAfter(departureDate)) {
            throw new IllegalArgumentException(
                    "Activity date must fall within destination dates");
        }

        boolean alreadyExists = activities.stream()
                .anyMatch(a -> a.getActivityId().equals(
                        activity.getActivityId()));

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Activity already exists at this destination");
        }

        activities.add(activity);
    }

    // Remove activity
    public void removeActivity(String activityId) {

        boolean removed = activities.removeIf(
                activity -> activity.getActivityId().equals(activityId));

        if (!removed) {
            throw new IllegalArgumentException("Activity not found");
        }
    }

    // Display activities
    public void displayActivities() {

        System.out.println("\nActivities at " + name);

        if (activities.isEmpty()) {
            System.out.println("No activities planned yet.");
            return;
        }

        for (Activity activity : activities) {
            activity.displayActivity();
        }
    }

    // Display destination details
    public void displayDestination() {

        System.out.println("\n----- DESTINATION DETAILS -----");
        System.out.println("Destination ID: " + destinationId);
        System.out.println("Name: " + name);
        System.out.println("City: " + city);
        System.out.println("Country: " + country);
        System.out.println("Description: " + description);
        System.out.println("Arrival: " + arrivalDate);
        System.out.println("Departure: " + departureDate);
        System.out.println("Activities: " + activities.size());
    }
}
