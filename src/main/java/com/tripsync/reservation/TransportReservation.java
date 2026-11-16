
package com.tripsync.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class TransportReservation extends Reservation {

    public enum TransportType {
        FLIGHT,
        TRAIN,
        BUS,
        CAB
    }

    private TransportType transportType;
    private String departureLocation;
    private String arrivalLocation;
    private LocalTime departureTime;
    private String bookingReference;

    public TransportReservation(
            String tripId,
            String providerName,
            BigDecimal cost,
            LocalDate reservationDate,
            TransportType transportType,
            String departureLocation,
            String arrivalLocation,
            LocalTime departureTime,
            String bookingReference) {

        super(tripId, providerName, cost, reservationDate);

        if (transportType == null || departureTime == null) {
            throw new IllegalArgumentException(
                "Transport type and departure time are required."
            );
        }

        if (departureLocation == null || departureLocation.isBlank()
                || arrivalLocation == null || arrivalLocation.isBlank()) {
            throw new IllegalArgumentException(
                "Departure and arrival locations are required."
            );
        }

        this.transportType = transportType;
        this.departureLocation = departureLocation;
        this.arrivalLocation = arrivalLocation;
        this.departureTime = departureTime;
        this.bookingReference = bookingReference;
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public String getDepartureLocation() {
        return departureLocation;
    }

    public String getArrivalLocation() {
        return arrivalLocation;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    @Override
    public String getReservationType() {
        return "Transport";
    }

    @Override
    public void displayReservation() {
        System.out.println("\n----- TRANSPORT RESERVATION -----");
        System.out.println("Reservation ID: " + getReservationId());
        System.out.println("Provider: " + getProviderName());
        System.out.println("Transport Type: " + transportType);
        System.out.println("From: " + departureLocation);
        System.out.println("To: " + arrivalLocation);
        System.out.println("Departure Time: " + departureTime);
        System.out.println("Booking Reference: " + bookingReference);
        System.out.println("Cost: Rs. " + getCost());
        System.out.println("Status: " + getStatus());
    }
}
