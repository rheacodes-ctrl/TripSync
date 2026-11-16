
package com.tripsync;

import com.tripsync.model.User;
import com.tripsync.model.Traveler;
import com.tripsync.model.TripAdmin;
import com.tripsync.model.Trip;
import com.tripsync.model.Destination;
import com.tripsync.model.Activity;
import com.tripsync.model.Expense;

import com.tripsync.service.TripManager;
import com.tripsync.service.ActivityManager;
import com.tripsync.service.ReservationManager;
import com.tripsync.service.ExpenseManager;

import com.tripsync.reservation.Reservation;
import com.tripsync.reservation.HotelReservation;
import com.tripsync.reservation.TransportReservation;
import com.tripsync.reservation.ActivityReservation;

import com.tripsync.exceptions.BudgetExceededException;
import com.tripsync.util.FileManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("       WELCOME TO TRIPSYNC!");
        System.out.println("======================================");


        // ==========================================
        // 1. USER MANAGEMENT
        // ==========================================

        System.out.println("\n========== USER MANAGEMENT ==========");

        TripAdmin admin = new TripAdmin(
            "Ishi",
            "ishi@example.com",
            "admin123"
        );

        Traveler traveler1 = new Traveler(
            "Rhea",
            "rhea@example.com",
            "password123"
        );

        Traveler traveler2 = new Traveler(
            "Shreya",
            "shreya@example.com",
            "password123"
        );

        // Runtime polymorphism
        User user1 = admin;
        User user2 = traveler1;
        User user3 = traveler2;

        user1.displayProfile();
        user2.displayProfile();
        user3.displayProfile();


        // ==========================================
        // 2. TRIP MANAGEMENT
        // ==========================================

        System.out.println("\n========== TRIP MANAGEMENT ==========");

        TripManager tripManager = new TripManager();

        Trip goaTrip = tripManager.createTrip(
            "Goa Vacation",
            "A fun trip to Goa with friends",
            LocalDate.of(2027, 1, 10),
            LocalDate.of(2027, 1, 15),
            new BigDecimal("50000"),
            admin
        );

        tripManager.addTraveler(
            goaTrip.getTripId(),
            traveler1
        );

        tripManager.addTraveler(
            goaTrip.getTripId(),
            traveler2
        );

        tripManager.updateTripBudget(
            goaTrip.getTripId(),
            new BigDecimal("60000")
        );

        System.out.println("\nTrip created successfully!");

        tripManager.displayTrip(goaTrip.getTripId());


        // ==========================================
        // 3. DESTINATION MANAGEMENT
        // ==========================================

        System.out.println("\n========== DESTINATION MANAGEMENT ==========");

        Destination beach = new Destination(
            "Baga Beach",
            "North Goa",
            "India",
            "Famous beach known for water sports",
            LocalDate.of(2027, 1, 10),
            LocalDate.of(2027, 1, 11)
        );

        Destination fort = new Destination(
            "Fort Aguada",
            "Candolim",
            "India",
            "Historic Portuguese fort",
            LocalDate.of(2027, 1, 12),
            LocalDate.of(2027, 1, 12)
        );

        Destination anjuna = new Destination(
            "Anjuna Beach",
            "Anjuna",
            "India",
            "Popular beach with a flea market",
            LocalDate.of(2027, 1, 13),
            LocalDate.of(2027, 1, 14)
        );

        tripManager.addDestination(
            goaTrip.getTripId(),
            beach
        );

        tripManager.addDestination(
            goaTrip.getTripId(),
            fort
        );

        tripManager.addDestination(
            goaTrip.getTripId(),
            anjuna
        );

        tripManager.displayDestinations(
            goaTrip.getTripId()
        );


        // ==========================================
        // 4. ACTIVITY MANAGEMENT
        // ==========================================

        System.out.println("\n========== ACTIVITY MANAGEMENT ==========");

        ActivityManager activityManager = new ActivityManager();

        Activity waterSports = new Activity(
            "Water Sports",
            "Jet skiing and parasailing",
            LocalDate.of(2027, 1, 10),
            LocalTime.of(10, 0),
            new BigDecimal("1500")
        );

        Activity beachDinner = new Activity(
            "Beach Dinner",
            "Dinner with friends near the beach",
            LocalDate.of(2027, 1, 11),
            LocalTime.of(19, 0),
            new BigDecimal("2000")
        );

        Activity sightseeing = new Activity(
            "Historical Sightseeing",
            "Explore the historic Fort Aguada",
            LocalDate.of(2027, 1, 12),
            LocalTime.of(11, 0),
            new BigDecimal("500")
        );

        Activity fleaMarket = new Activity(
            "Anjuna Flea Market",
            "Shopping and exploring local markets",
            LocalDate.of(2027, 1, 13),
            LocalTime.of(16, 0),
            new BigDecimal("1000")
        );

        // Add activities to destinations
        activityManager.createActivity(beach, waterSports);
        activityManager.createActivity(beach, beachDinner);
        activityManager.createActivity(fort, sightseeing);
        activityManager.createActivity(anjuna, fleaMarket);

        // Assign travelers
        activityManager.assignTraveler(
            waterSports.getActivityId(),
            traveler1
        );

        activityManager.assignTraveler(
            waterSports.getActivityId(),
            traveler2
        );

        activityManager.assignTraveler(
            beachDinner.getActivityId(),
            traveler1
        );

        // Voting
        activityManager.voteForActivity(
            waterSports.getActivityId(),
            traveler1.getUserId()
        );

        activityManager.voteForActivity(
            waterSports.getActivityId(),
            traveler2.getUserId()
        );

        // Update activity status
        activityManager.updateActivityStatus(
            waterSports.getActivityId(),
            Activity.Status.COMPLETED
        );

        // Display activities
        activityManager.displayAllActivities();

        System.out.println("\n----- ACTIVITIES AT BAGA BEACH -----");

        activityManager.displayActivitiesAtDestination(beach);


        // ==========================================
        // 5. RESERVATION MANAGEMENT
        // ==========================================

        System.out.println("\n========== RESERVATION MANAGEMENT ==========");

        ReservationManager reservationManager =
            new ReservationManager();

        // Hotel reservation
        HotelReservation hotel = new HotelReservation(
            goaTrip.getTripId(),
            "Taj Resort and Spa",
            new BigDecimal("12000"),
            LocalDate.now(),
            LocalDate.of(2027, 1, 10),
            LocalDate.of(2027, 1, 15),
            2
        );

        // Transport reservation
        TransportReservation transport = new TransportReservation(
            goaTrip.getTripId(),
            "IndiGo Airlines",
            new BigDecimal("8500"),
            LocalDate.now(),
            TransportReservation.TransportType.FLIGHT,
            "Delhi",
            "Goa",
            LocalTime.of(8, 30),
            "IND12345"
        );

        // Activity reservation
        ActivityReservation activityBooking =
            new ActivityReservation(
                goaTrip.getTripId(),
                "Water Sports",
                new BigDecimal("1500"),
                LocalDate.now(),
                "Baga Beach",
                LocalTime.of(10, 0),
                2
            );

        // Add reservations
        reservationManager.addReservation(hotel);
        reservationManager.addReservation(transport);
        reservationManager.addReservation(activityBooking);

        // Confirm reservations
        reservationManager.confirmReservation(
            hotel.getReservationId()
        );

        reservationManager.confirmReservation(
            transport.getReservationId()
        );

        reservationManager.confirmReservation(
            activityBooking.getReservationId()
        );

        // Display reservations
        reservationManager.displayAllReservations();

        System.out.println("\n----- GOA TRIP RESERVATIONS -----");

        reservationManager.displayReservationsByTrip(
            goaTrip.getTripId()
        );

        // Reservation statistics
        System.out.println("\n----- RESERVATION STATISTICS -----");

        System.out.println(
            "Total Reservations: "
            + reservationManager.getReservationCount()
        );

        System.out.println(
            "Confirmed Reservations: "
            + reservationManager.getConfirmedReservationCount()
        );

        System.out.println(
            "Total Reservation Cost: Rs. "
            + reservationManager.getTotalReservationCost(
                goaTrip.getTripId()
            )
        );


        // ==========================================
        // 6. EXPENSE MANAGEMENT
        // ==========================================

        System.out.println("\n========== EXPENSE MANAGEMENT ==========");

        ExpenseManager expenseManager = new ExpenseManager();

        List<Traveler> travelers = Arrays.asList(
            traveler1,
            traveler2
        );

        // Create dinner expense paid by Rhea
        Expense dinnerExpense = new Expense(
            goaTrip.getTripId(),
            "Dinner at Baga Beach",
            new BigDecimal("3000"),
            traveler1,
            LocalDate.of(2027, 1, 11)
        );

        // Add expense with budget validation
        try {

            expenseManager.addExpense(
                goaTrip,
                dinnerExpense,
                travelers
            );

        } catch (BudgetExceededException e) {

            System.out.println(
                "BUDGET ERROR: " + e.getMessage()
            );
        }

        // Display all expenses
        expenseManager.displayAllExpenses();


        // ==========================================
        // OUTSTANDING BALANCES
        // ==========================================

        System.out.println("\n----- OUTSTANDING BALANCES -----");

        System.out.println(
            "Rhea Balance: Rs. "
            + expenseManager.getOutstandingBalance(
                traveler1.getUserId()
            )
        );

        System.out.println(
            "Shreya Balance: Rs. "
            + expenseManager.getOutstandingBalance(
                traveler2.getUserId()
            )
        );

        expenseManager.displayPendingPayments();


        // ==========================================
        // PAYMENT SETTLEMENT
        // ==========================================

        System.out.println("\n----- PAYMENT SETTLEMENT -----");

        expenseManager.settlePayment(
            traveler2.getUserId(),
            traveler1.getUserId(),
            new BigDecimal("1500")
        );

        System.out.println("\nAfter settlement:");

        expenseManager.displayPendingPayments();


        // ==========================================
        // EXPENSE STATISTICS
        // ==========================================

        System.out.println("\n----- EXPENSE STATISTICS -----");

        System.out.println(
            "Total Expenses: Rs. "
            + expenseManager.getTotalExpenses()
        );

        System.out.println(
            "Number of Expenses: "
            + expenseManager.getExpenseCount()
        );

        System.out.println(
            "Remaining Trip Budget: Rs. "
            + expenseManager.getRemainingBudget(goaTrip)
        );


        // ==========================================
        // BUDGET VALIDATION TEST
        // ==========================================

        System.out.println("\n----- BUDGET VALIDATION TEST -----");

        Expense expensiveExpense = new Expense(
            goaTrip.getTripId(),
            "Luxury Yacht Booking",
            new BigDecimal("70000"),
            traveler1,
            LocalDate.of(2027, 1, 12)
        );

        try {

            expenseManager.addExpense(
                goaTrip,
                expensiveExpense,
                travelers
            );

        } catch (BudgetExceededException e) {

            System.out.println("EXPECTED EXCEPTION:");
            System.out.println(e.getMessage());
        }


        // ==========================================
        // 7. FILE MANAGEMENT
        // ==========================================

        System.out.println("\n========== FILE MANAGEMENT ==========");

        FileManager fileManager = new FileManager();

        try {

            // Create a sample receipt file
            Path sampleFile = Path.of("sample-receipt.txt");

            Files.writeString(
                sampleFile,
                "Trip: Goa Vacation\n"
                + "Expense: Dinner at Baga Beach\n"
                + "Amount: Rs. 3000\n"
                + "Paid by: Rhea"
            );

            // Upload file
            String uploadedFile = fileManager.uploadFile(
                sampleFile.toString()
            );

            // Check file existence
            System.out.println(
                "File exists: "
                + fileManager.fileExists(uploadedFile)
            );

            // Read uploaded file
            System.out.println("\n----- FILE CONTENT -----");

            System.out.println(
                fileManager.readTextFile(uploadedFile)
            );

            // Display stored files
            System.out.println("\n----- STORED FILES -----");

            for (String file : fileManager.listFiles()) {
                System.out.println(file);
            }

            // Display storage location
            System.out.println(
                "\nStorage Location: "
                + fileManager.getStorageDirectory()
            );

            // Delete uploaded file
            fileManager.deleteFile(uploadedFile);

            // Delete original sample file
            Files.deleteIfExists(sampleFile);

            System.out.println(
                "File exists after deletion: "
                + fileManager.fileExists(uploadedFile)
            );

        } catch (IOException e) {

            System.out.println(
                "File management error: " + e.getMessage()
            );
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
            "Total Trips: " + tripManager.getTripCount()
        );

        System.out.println(
            "Total Travelers: " + goaTrip.getMembers().size()
        );

        System.out.println(
            "Total Destinations: " + goaTrip.getDestinations().size()
        );

        System.out.println(
            "Total Activities: " + activityManager.getActivityCount()
        );

        System.out.println(
            "Water Sports Votes: " + waterSports.getVoteCount()
        );

        System.out.println(
            "Trips Managed by Admin: "
            + admin.getManagedTripCount()
        );

        System.out.println(
            "Total Reservations: "
            + reservationManager.getReservationCount()
        );

        System.out.println(
            "Confirmed Reservations: "
            + reservationManager.getConfirmedReservationCount()
        );

        System.out.println(
            "Total Expenses: "
            + expenseManager.getExpenseCount()
        );

        System.out.println(
            "Total Expense Amount: Rs. "
            + expenseManager.getTotalExpenses()
        );

        System.out.println(
            "Remaining Budget: Rs. "
            + expenseManager.getRemainingBudget(goaTrip)
        );


        // ==========================================
        // FINAL MESSAGE
        // ==========================================

        System.out.println("\n======================================");
        System.out.println("       TRIPSYNC DEMO COMPLETED!");
        System.out.println("======================================");
    }
}
