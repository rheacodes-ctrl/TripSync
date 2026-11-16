package com.tripsync;

import com.tripsync.model.User;
import com.tripsync.model.Traveler;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.Trip;
import com.tripsync.model.Destination;

import com.tripsync.service.TripManager;
import com.tripsync.service.ActivityManager;
import com.tripsync.service.ReservationManager;
import com.tripsync.service.ExpenseManager;

import com.tripsync.dao.UserDAO;
import com.tripsync.dao.PaymentDAO;

import com.tripsync.util.FileManager;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("       WELCOME TO TRIPSYNC!");
        System.out.println("======================================");

        // This demo is READ-ONLY. It retrieves and displays the
        // existing seeded records (trip T001, users U001-U003,
        // destinations, activities, reservations, expenses and
        // payments) using the existing DAO and manager methods.
        // Nothing is inserted, updated, deleted or settled, so the
        // database is left unchanged on every run.

        try {

            // ==========================================
            // DATA ACCESS LAYER
            // ==========================================

            UserDAO userDAO = new UserDAO();
            PaymentDAO paymentDAO = new PaymentDAO();

            TripManager tripManager = new TripManager();
            ActivityManager activityManager = new ActivityManager();
            ReservationManager reservationManager =
                new ReservationManager();
            ExpenseManager expenseManager = new ExpenseManager();


            // ==========================================
            // 1. USER MANAGEMENT (RETRIEVE SEEDED USERS)
            // ==========================================

            System.out.println("\n========== USER MANAGEMENT ==========");

            // Seeded users loaded straight from MySQL. No passwords
            // are stored or hardcoded in the demo.
            TripAdmin admin =
                (TripAdmin) userDAO.getUserById("U001");
            Traveler traveler1 =
                (Traveler) userDAO.getUserById("U002");
            Traveler traveler2 =
                (Traveler) userDAO.getUserById("U003");

            if (admin == null || traveler1 == null || traveler2 == null) {
                throw new IllegalStateException(
                    "Seeded users U001, U002 and U003 were not found."
                );
            }

            // Runtime polymorphism: a User reference dispatches to
            // the concrete TripAdmin / Traveler display method.
            User user1 = admin;
            User user2 = traveler1;
            User user3 = traveler2;

            user1.displayProfile();
            user2.displayProfile();
            user3.displayProfile();


            // ==========================================
            // 2. TRIP MANAGEMENT (RETRIEVE TRIP T001)
            // ==========================================

            System.out.println("\n========== TRIP MANAGEMENT ==========");

            Trip goaTrip = tripManager.getTripById("T001");

            tripManager.displayTrip(goaTrip.getTripId());

            System.out.println(
                "Total trips in database: "
                + tripManager.getTripCount()
            );


            // ==========================================
            // 3. DESTINATION MANAGEMENT
            // ==========================================

            System.out.println("\n========== DESTINATION MANAGEMENT ==========");

            tripManager.displayDestinations(goaTrip.getTripId());

            System.out.println(
                "Destinations in this trip: "
                + goaTrip.getDestinations().size()
            );


            // ==========================================
            // 4. ACTIVITY MANAGEMENT
            // ==========================================

            System.out.println("\n========== ACTIVITY MANAGEMENT ==========");

            activityManager.displayAllActivities();

            System.out.println("\n----- ACTIVITIES BY DESTINATION -----");

            for (Destination destination : goaTrip.getDestinations()) {
                activityManager.displayActivitiesAtDestination(destination);
            }

            activityManager.displayActivity("A001");

            System.out.println("\n----- ACTIVITY STATISTICS -----");

            System.out.println(
                "Total activities: "
                + activityManager.getActivityCount()
            );

            System.out.println(
                "Water Sports (A001) votes: "
                + activityManager.getActivityById("A001").getVoteCount()
            );


            // ==========================================
            // 5. RESERVATION MANAGEMENT
            // ==========================================

            System.out.println("\n========== RESERVATION MANAGEMENT ==========");

            reservationManager.displayAllReservations();

            System.out.println("\n----- GOA TRIP RESERVATIONS -----");

            reservationManager.displayReservationsByTrip(goaTrip.getTripId());

            reservationManager.displayReservation("R001");

            System.out.println("\n----- RESERVATION STATISTICS -----");

            System.out.println(
                "Total reservations: "
                + reservationManager.getReservationCount()
            );

            System.out.println(
                "Confirmed reservations: "
                + reservationManager.getConfirmedReservationCount()
            );

            System.out.println(
                "Total reservation cost for Goa: Rs. "
                + reservationManager.getTotalReservationCost(
                    goaTrip.getTripId()
                )
            );


            // ==========================================
            // 6. EXPENSE MANAGEMENT
            // ==========================================

            System.out.println("\n========== EXPENSE MANAGEMENT ==========");

            expenseManager.displayAllExpenses();

            System.out.println("\n----- EXPENSE STATISTICS -----");

            System.out.println(
                "Number of expenses: "
                + expenseManager.getExpenseCount()
            );

            System.out.println(
                "Total expenses (all trips): Rs. "
                + expenseManager.getTotalExpenses()
            );

            System.out.println(
                "Total expenses for Goa: Rs. "
                + expenseManager.getTotalExpenses(goaTrip.getTripId())
            );

            System.out.println(
                "Remaining trip budget: Rs. "
                + expenseManager.getRemainingBudget(goaTrip)
            );

            // Outstanding balances are read straight from the
            // database through PaymentDAO. ExpenseManager's Payable
            // methods are scoped to a "managed" trip that is only
            // set when an expense is added; since this demo never
            // adds an expense, the seeded balances are read directly.
            System.out.println("\n----- OUTSTANDING BALANCES -----");

            Map<String, BigDecimal> balances =
                paymentDAO.getOutstandingBalances(goaTrip.getTripId());

            for (Map.Entry<String, BigDecimal> entry
                    : balances.entrySet()) {

                BigDecimal balance = entry.getValue();
                String status;

                if (balance.compareTo(BigDecimal.ZERO) < 0) {
                    status = "owes Rs. " + balance.abs();
                } else if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    status = "should receive Rs. " + balance;
                } else {
                    status = "is settled";
                }

                System.out.println("Traveler " + entry.getKey()
                    + " " + status);
            }

            System.out.println("\n----- SETTLEMENT HISTORY -----");

            List<Map<String, Object>> payments =
                paymentDAO.getPaymentsByTripId(goaTrip.getTripId());

            if (payments.isEmpty()) {
                System.out.println("No payments recorded.");
            } else {
                for (Map<String, Object> payment : payments) {
                    System.out.println(
                        payment.get("fromUserId") + " -> "
                        + payment.get("toUserId")
                        + " : Rs. " + payment.get("amount")
                        + " (" + payment.get("status") + ")"
                    );
                }
            }


            // ==========================================
            // 7. FILE MANAGEMENT (READ-ONLY)
            // ==========================================

            System.out.println("\n========== FILE MANAGEMENT ==========");

            FileManager fileManager = new FileManager();

            List<String> storedFiles = fileManager.listFiles();

            System.out.println(
                "Storage location: " + fileManager.getStorageDirectory()
            );

            if (storedFiles.isEmpty()) {
                System.out.println("No files stored.");
            } else {
                System.out.println("Stored files:");
                for (String file : storedFiles) {
                    System.out.println("  " + file);
                }
            }


            // ==========================================
            // 8. COMPLETE TRIP SUMMARY
            // ==========================================

            System.out.println("\n========== COMPLETE TRIP SUMMARY ==========");

            tripManager.displayTrip(goaTrip.getTripId());


            // ==========================================
            // 9. TRIPSYNC STATISTICS
            // ==========================================

            System.out.println("\n========== TRIPSYNC STATISTICS ==========");

            System.out.println(
                "Total trips: " + tripManager.getTripCount()
            );

            System.out.println(
                "Goa travelers: " + goaTrip.getMembers().size()
            );

            System.out.println(
                "Goa destinations: " + goaTrip.getDestinations().size()
            );

            System.out.println(
                "Total activities: " + activityManager.getActivityCount()
            );

            System.out.println(
                "Water Sports votes: "
                + activityManager.getActivityById("A001").getVoteCount()
            );

            System.out.println(
                "Total reservations: "
                + reservationManager.getReservationCount()
            );

            System.out.println(
                "Confirmed reservations: "
                + reservationManager.getConfirmedReservationCount()
            );

            System.out.println(
                "Total expenses: " + expenseManager.getExpenseCount()
            );

            System.out.println(
                "Total expense amount: Rs. "
                + expenseManager.getTotalExpenses(goaTrip.getTripId())
            );

            System.out.println(
                "Remaining budget: Rs. "
                + expenseManager.getRemainingBudget(goaTrip)
            );


            // ==========================================
            // FINAL MESSAGE
            // ==========================================

            System.out.println("\n======================================");
            System.out.println("       TRIPSYNC DEMO COMPLETED!");
            System.out.println("======================================");

        } catch (SQLException e) {

            // Database connection or query failure (e.g. MySQL is
            // not running or TRIPSYNC_DB_PASSWORD is not set).
            System.err.println(
                "\nDatabase error: " + e.getMessage()
            );

            System.err.println(
                "Ensure MySQL is running and the "
                + "TRIPSYNC_DB_PASSWORD environment variable is set."
            );

        } catch (Exception e) {

            System.err.println(
                "\nTripSync demo stopped: " + e.getMessage()
            );
        }
    }
}
