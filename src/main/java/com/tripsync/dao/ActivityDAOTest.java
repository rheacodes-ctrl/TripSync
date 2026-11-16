
package com.tripsync.dao;

import com.tripsync.model.Activity;
import com.tripsync.model.Destination;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public class ActivityDAOTest {

    public static void main(String[] args) {

        ActivityDAO activityDAO = new ActivityDAO();
        DestinationDAO destinationDAO = new DestinationDAO();

        // Existing seeded destination and traveler
        String destinationId = "D001";
        String travelerId = "U002";

        String activityId = null;

        try {

            // GET DESTINATION
            System.out.println("\n========== GET DESTINATION ==========");

            Destination destination =
                    destinationDAO.getDestinationById(destinationId);

            if (destination == null) {
                throw new IllegalStateException(
                        "Seeded destination D001 was not found");
            }

            System.out.println("Destination: " + destination.getName());
            System.out.println("City: " + destination.getCity());


            // CREATE ACTIVITY
            System.out.println("\n========== CREATE ACTIVITY ==========");

            Activity activity = new Activity(
                    "JDBC Test Activity",
                    "Temporary activity for testing ActivityDAO",
                    destination.getArrivalDate(),
                    LocalTime.of(10, 30),
                    new BigDecimal("1500.00")
            );

            activityId = activity.getActivityId();

            boolean created =
                    activityDAO.createActivity(destinationId, activity);

            System.out.println("Activity created: " + created);
            System.out.println("Activity ID: " + activityId);


            // ASSIGN PARTICIPANT
            System.out.println("\n========== ASSIGN PARTICIPANT ==========");

            boolean assigned =
                    activityDAO.assignParticipant(activityId, travelerId);

            System.out.println("Participant assigned: " + assigned);


            // VOTE
            System.out.println("\n========== RECORD VOTE ==========");

            boolean voted =
                    activityDAO.voteForActivity(activityId, travelerId);

            System.out.println("Vote recorded: " + voted);


            // RETRIEVE ACTIVITY
            System.out.println("\n========== RETRIEVE ACTIVITY ==========");

            Activity retrieved =
                    activityDAO.getActivityById(activityId);

            if (retrieved != null) {
                retrieved.displayActivity();

                System.out.println(
                        "Loaded participant count: " +
                        retrieved.getParticipants().size());

                System.out.println(
                        "Loaded vote count: " +
                        retrieved.getVoteCount());
            }


            // GET ALL ACTIVITIES
            System.out.println("\n========== ALL ACTIVITIES ==========");

            List<Activity> activities =
                    activityDAO.getActivitiesByDestinationId(destinationId);

            for (Activity a : activities) {
                System.out.println(
                        a.getName() + " | " +
                        a.getActivityDate() + " | " +
                        a.getStatus());
            }

            System.out.println("Total activities: " + activities.size());


            // UPDATE ACTIVITY
            System.out.println("\n========== UPDATE ACTIVITY ==========");

            retrieved.updateDetails(
                    "Updated JDBC Test Activity",
                    "Updated activity description");

            retrieved.updateEstimatedCost(
                    new BigDecimal("2000.00"));

            retrieved.updateStatus(Activity.Status.PLANNED);

            boolean updated =
                    activityDAO.updateActivity(retrieved);

            System.out.println("Activity updated: " + updated);


            // VERIFY UPDATE
            System.out.println("\n========== VERIFY UPDATE ==========");

            Activity updatedActivity =
                    activityDAO.getActivityById(activityId);

            System.out.println(
                    "Updated name: " + updatedActivity.getName());

            System.out.println(
                    "Updated cost: " +
                    updatedActivity.getEstimatedCost());


            // VOTE COUNT
            System.out.println("\n========== VOTE COUNT ==========");

            System.out.println(
                    "Votes: " + activityDAO.getVoteCount(activityId));


            // REMOVE PARTICIPANT
            System.out.println("\n========== REMOVE PARTICIPANT ==========");

            boolean removed = activityDAO.removeParticipant(
                    activityId, travelerId);

            System.out.println("Participant removed: " + removed);


            // DELETE ACTIVITY
            System.out.println("\n========== DELETE ACTIVITY ==========");

            boolean deleted = activityDAO.deleteActivity(activityId);

            System.out.println("Activity deleted: " + deleted);


            System.out.println(
                    "\n========== ACTIVITY DAO TEST COMPLETED ==========");

        } catch (Exception e) {

            System.out.println("\nACTIVITY DAO TEST FAILED!");
            e.printStackTrace();

            // Try to clean up the temporary activity if it was created.
            if (activityId != null) {
                try {
                    activityDAO.deleteActivity(activityId);
                    System.out.println(
                            "Temporary activity cleanup attempted.");
                } catch (Exception cleanupError) {
                    System.out.println(
                            "Could not clean up temporary activity.");
                    cleanupError.printStackTrace();
                }
            }
        }
    }
}
