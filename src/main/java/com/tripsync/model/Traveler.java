package com.tripsync.model;

import java.util.ArrayList;
import java.util.List;

public class Traveler extends User {

    private List<String> preferences;
    private List<String> joinedTripIds;

    public Traveler(String name, String email, String password) {

        super(name, email, password);

        this.preferences = new ArrayList<>();
        this.joinedTripIds = new ArrayList<>();
    }

    public void addPreference(String preference) {
        if (preference != null && !preference.isBlank()) {
            preferences.add(preference);
        }
    }

    public void joinTrip(String tripId) {
        if (!joinedTripIds.contains(tripId)) {
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
        System.out.println("Name: " + getName());
        System.out.println("Email: " + getEmail());
        System.out.println("Role: " + getRole());
        System.out.println("Preferences: " + preferences);
        System.out.println("Joined Trips: " + joinedTripIds.size());
    }
}
