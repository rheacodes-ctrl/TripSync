package com.tripsync;

import com.tripsync.model.User;
import com.tripsync.model.Traveler;
import com.tripsync.model.TripAdmin;

public class Main {

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("       WELCOME TO TRIPSYNC");
        System.out.println("==================================");

        User traveler = new Traveler(
            "Ishi",
            "ishi@example.com",
            "password123"
        );

        User admin = new TripAdmin(
            "Rhea",
            "rhea@example.com",
            "admin123"
        );

        traveler.displayProfile();
        admin.displayProfile();

        System.out.println("\nRole of first user: " + traveler.getRole());
        System.out.println("Role of second user: " + admin.getRole());
    }
}