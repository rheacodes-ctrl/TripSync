
package com.tripsync.model;

public class TripAdmin extends User {

    private int managedTripCount;

    // Normal constructor
    public TripAdmin(String name, String email, String password) {
        super(name, email, password);

        this.managedTripCount = 0;
    }

    // Database constructor
    public TripAdmin(
            String userId,
            String name,
            String email,
            boolean fromDatabase
    ) {
        super(userId, name, email, fromDatabase);

        this.managedTripCount = 0;
    }

    public void incrementManagedTripCount() {
        managedTripCount++;
    }

    public int getManagedTripCount() {
        return managedTripCount;
    }

    @Override
    public String getRole() {
        return "Trip Administrator";
    }

    @Override
    public void displayProfile() {
        System.out.println("\n----- ADMIN PROFILE -----");
        System.out.println("User ID: " + getUserId());
        System.out.println("Name: " + getName());
        System.out.println("Email: " + getEmail());
        System.out.println("Role: " + getRole());
        System.out.println("Managed Trips: " + managedTripCount);
    }
}
