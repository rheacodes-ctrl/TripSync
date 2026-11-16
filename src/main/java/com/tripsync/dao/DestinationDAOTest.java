
package com.tripsync.dao;

import com.tripsync.model.Destination;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DestinationDAOTest {

    public static void main(String[] args) {

        DestinationDAO dao = new DestinationDAO();

        // Existing seeded trip
        String tripId = "T001";

        String uniqueId = UUID.randomUUID()
                .toString()
                .substring(0, 8);

        Destination destination = new Destination(
                "Test Destination " + uniqueId,
                "Jaipur",
                "India",
                "Temporary destination for JDBC testing",
                LocalDate.of(2027, 1, 12),
                LocalDate.of(2027, 1, 13)
        );

        try {

            // CREATE
            System.out.println("\n========== CREATE DESTINATION ==========");

            boolean created = dao.createDestination(
                    tripId, destination);

            System.out.println("Destination created: " + created);
            System.out.println(
                    "Destination ID: " + destination.getDestinationId());


            // READ BY ID
            System.out.println("\n========== RETRIEVE DESTINATION ==========");

            Destination retrieved = dao.getDestinationById(
                    destination.getDestinationId());

            if (retrieved != null) {
                retrieved.displayDestination();
            }


            // READ ALL DESTINATIONS
            System.out.println("\n========== ALL DESTINATIONS ==========");

            List<Destination> destinations =
                    dao.getDestinationsByTripId(tripId);

            for (Destination d : destinations) {

                System.out.println(
                        d.getName() + " | " +
                        d.getCity() + " | " +
                        d.getCountry());
            }

            System.out.println(
                    "Total destinations: " + destinations.size());


            // UPDATE
            System.out.println("\n========== UPDATE DESTINATION ==========");

            retrieved.updateDescription(
                    "Updated description for JDBC testing");

            boolean updated = dao.updateDestination(retrieved);

            System.out.println("Destination updated: " + updated);


            // VERIFY UPDATE
            System.out.println("\n========== VERIFY UPDATE ==========");

            Destination updatedDestination =
                    dao.getDestinationById(
                            destination.getDestinationId());

            System.out.println(
                    "Updated description: " +
                    updatedDestination.getDescription());


            // COUNT
            System.out.println("\n========== DESTINATION COUNT ==========");

            System.out.println(
                    "Count: " + dao.getDestinationCount(tripId));


            // DELETE
            System.out.println("\n========== DELETE TEST DESTINATION ==========");

            boolean deleted = dao.deleteDestination(
                    destination.getDestinationId());

            System.out.println("Destination deleted: " + deleted);


            System.out.println(
                    "\n========== DESTINATION DAO TEST COMPLETED ==========");

        } catch (Exception e) {

            System.out.println("Destination DAO test failed!");
            e.printStackTrace();
        }
    }
}
