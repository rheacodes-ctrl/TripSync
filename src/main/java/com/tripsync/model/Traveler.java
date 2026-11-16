
package com.tripsync.model;

import java.util.ArrayList;
import java.util.List;

public class Traveler extends User {

    private final List<String> preferences;
    private final List<String> joinedTripIds;

    // Normal constructor
    public Traveler(String name, String email, String password) {
        super(name, email, password);

        this.preferences = new ArrayList<>();
        this.joinedTripIds = new ArrayList<>();
    }

    // Database constructor
    public Traveler(
            String userId,
            String name,
            String email,
            boolean fromDatabase
    ) {
        super(userId, name, email, fromDatabase);

        this.preferences = new ArrayList<>();
        this.joinedTripIds = new ArrayList<>();
    }

    public void addPreference(String preference) {
        if (preference != null && !preference.isBlank()) {
            preferences.add(preference);
        }
    }

    public void joinTrip(String tripId) {
        if (tripId != null && !tripId.isBlank()
                && !joinedTripIds.contains(tripId)) {
            joinedTripIds.add(tripId);
        }
    }

    public List<String> getPreferences() {
        return new ArrayList<>(preferences);
    }

    public List<String> getJoinedTripIds() {
        return new ArrayList<>(joinedTripIds);
    }

    @Override
    public String getRole() {
        return "Traveler";
    }

    @Override
    public void displayProfile() {
        System.out.println("\n----- TRAVELER PROFILE -----");
        System.out.println("User ID: " + getUserId());
        System.out.println("Name: " + getName());
        System.out.println("Email: " + getEmail());
        System.out.println("Role: " + getRole());
        System.out.println("Preferences: " + preferences);
        System.out.println("Joined Trips: " + joinedTripIds.size());
    }
}
