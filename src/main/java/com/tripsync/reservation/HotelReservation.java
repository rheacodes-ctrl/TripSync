
package com.tripsync.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HotelReservation extends Reservation {

    private String hotelName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numberOfRooms;

    public HotelReservation(
            String tripId,
            String hotelName,
            BigDecimal cost,
            LocalDate reservationDate,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numberOfRooms) {

        super(tripId, hotelName, cost, reservationDate);
        validate(checkInDate, checkOutDate, numberOfRooms);

        this.hotelName = hotelName;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfRooms = numberOfRooms;
    }

    // Database-loading constructor
    public HotelReservation(
            String reservationId,
            String tripId,
            String hotelName,
            BigDecimal cost,
            LocalDate reservationDate,
            Status status,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numberOfRooms) {

        super(reservationId, tripId, hotelName,
                cost, reservationDate, status);

        validate(checkInDate, checkOutDate, numberOfRooms);

        this.hotelName = hotelName;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfRooms = numberOfRooms;
    }

    private void validate(LocalDate checkIn, LocalDate checkOut,
                          int rooms) {
        if (checkIn == null || checkOut == null ||
                checkOut.isBefore(checkIn)) {
            throw new IllegalArgumentException("Invalid hotel dates.");
        }

        if (rooms <= 0) {
            throw new IllegalArgumentException(
                    "Number of rooms must be positive.");
        }
    }

    public String getHotelName() {
        return hotelName;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public int getNumberOfRooms() {
        return numberOfRooms;
    }

    @Override
    public String getReservationType() {
        return "Hotel";
    }

    @Override
    public void displayReservation() {
        System.out.println("\n----- HOTEL RESERVATION -----");
        System.out.println("Reservation ID: " + getReservationId());
        System.out.println("Hotel: " + hotelName);
        System.out.println("Check-in: " + checkInDate);
        System.out.println("Check-out: " + checkOutDate);
        System.out.println("Rooms: " + numberOfRooms);
        System.out.println("Cost: Rs. " + getCost());
        System.out.println("Status: " + getStatus());
    }
}
