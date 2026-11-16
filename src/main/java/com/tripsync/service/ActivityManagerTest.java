package com.tripsync.service;

import com.tripsync.dao.ActivityDAO;
import com.tripsync.dao.DestinationDAO;
import com.tripsync.dao.UserDAO;

import com.tripsync.model.Activity;
import com.tripsync.model.Destination;
import com.tripsync.model.User;
import com.tripsync.model.Traveler;

import java.math.BigDecimal;
import java.time.LocalTime;

public class ActivityManagerTest {

    public static void main(String[] args) throws Exception {

        ActivityDAO activityDAO = new ActivityDAO();
        DestinationDAO destinationDAO = new DestinationDAO();
        UserDAO userDAO = new UserDAO();

        ActivityManager activityManager = new ActivityManager();

        // Seeded records used as context. These are never modified or deleted.
        String seededDestinationId = "D001";
        String rheaId = "U002";
        String shreyaId = "U003";

        String activityId = null;

        System.out.println("========== ACTIVITY MANAGER INTEGRATION TEST ==========");

        try {

            // ==========================================
            // 1. RETRIEVE SEEDED CONTEXT
            // ==========================================

            System.out.println("\n========== RETRIEVE SEEDED CONTEXT ==========");

            Destination destination =
                    destinationDAO.getDestinationById(seededDestinationId);

            if (destination == null) {
                throw new IllegalStateException(
                    "Seeded destination D001 was not found."
                );
            }

            User rheaUser = userDAO.getUserById(rheaId);
            User shreyaUser = userDAO.getUserById(shreyaId);

            if (!(rheaUser instanceof Traveler)
                    || !(shreyaUser instanceof Traveler)) {
                throw new IllegalStateException(
                    "Seeded travelers U002 and U003 were not found."
                );
            }

            Traveler rhea = (Traveler) rheaUser;
            Traveler shreya = (Traveler) shreyaUser;

            System.out.println("Destination: " + destination.getName());
            System.out.println("Traveler 1: " + rhea.getName());
            System.out.println("Traveler 2: " + shreya.getName());

            int initialCount = activityManager.getActivityCount();

            System.out.println(
                "Initial activity count (seeded): " + initialCount
            );

            // ==========================================
            // 2. CREATE ACTIVITY (PERSISTS IN MYSQL)
            // ==========================================

            System.out.println("\n========== CREATE ACTIVITY ==========");

            Activity activity = new Activity(
                "JDBC Test Activity",
                "Temporary activity for ActivityManager test",
                destination.getArrivalDate(),
                LocalTime.of(10, 30),
                new BigDecimal("1750.00")
            );

            activityId = activity.getActivityId();

            Activity created =
                    activityManager.createActivity(destination, activity);

            System.out.println("Created activity ID: " + created.getActivityId());

            System.out.println(
                "Activity exists in database: "
                + (activityDAO.getActivityById(activityId) != null)
            );

            System.out.println(
                "Activity count after create: "
                + activityManager.getActivityCount()
            );

            if (activityManager.getActivityCount() != initialCount + 1) {
                throw new IllegalStateException(
                    "Activity count did not increase after create."
                );
            }

            // ==========================================
            // 3. ASSIGN TRAVELERS (PERSISTS IN MYSQL)
            // ==========================================

            System.out.println("\n========== ASSIGN TRAVELERS ==========");

            activityManager.assignTraveler(activityId, rhea);
            activityManager.assignTraveler(activityId, shreya);

            Activity afterAssign = activityDAO.getActivityById(activityId);

            System.out.println(
                "Participants after assignment: "
                + afterAssign.getParticipants().size()
            );

            if (afterAssign.getParticipants().size() != 2) {
                throw new IllegalStateException(
                    "Two participants were expected."
                );
            }

            // ==========================================
            // 4. VOTE (PERSISTS IN MYSQL)
            // ==========================================

            System.out.println("\n========== RECORD VOTES ==========");

            activityManager.voteForActivity(activityId, rhea.getUserId());
            activityManager.voteForActivity(activityId, shreya.getUserId());

            Activity afterVotes = activityDAO.getActivityById(activityId);

            System.out.println(
                "Vote count after voting: " + afterVotes.getVoteCount()
            );

            if (afterVotes.getVoteCount() != 2) {
                throw new IllegalStateException(
                    "Two votes were expected."
                );
            }

            // Duplicate vote must be rejected by the DAO.
            boolean duplicateRejected = false;

            try {
                activityManager.voteForActivity(activityId, rhea.getUserId());
            } catch (IllegalArgumentException expected) {
                duplicateRejected = true;
                System.out.println(
                    "Duplicate vote rejected: " + expected.getMessage()
                );
            }

            if (!duplicateRejected) {
                throw new IllegalStateException(
                    "Duplicate vote was not rejected."
                );
            }

            // ==========================================
            // 5. UPDATE ACTIVITY STATUS (PERSISTS IN MYSQL)
            // ==========================================

            System.out.println("\n========== UPDATE STATUS ==========");

            activityManager.updateActivityStatus(
                activityId,
                Activity.Status.COMPLETED
            );

            Activity afterStatus = activityDAO.getActivityById(activityId);

            System.out.println(
                "Status after update: " + afterStatus.getStatus()
            );

            if (afterStatus.getStatus() != Activity.Status.COMPLETED) {
                throw new IllegalStateException(
                    "Activity status was not persisted."
                );
            }

            // ==========================================
            // 6. REMOVE TRAVELER (PERSISTS IN MYSQL)
            // ==========================================

            System.out.println("\n========== REMOVE TRAVELER ==========");

            activityManager.removeTraveler(activityId, shreya.getUserId());

            Activity afterRemoval = activityDAO.getActivityById(activityId);

            System.out.println(
                "Participants after removal: "
                + afterRemoval.getParticipants().size()
            );

            System.out.println(
                "Votes after removal: " + afterRemoval.getVoteCount()
            );

            if (afterRemoval.getParticipants().size() != 1
                    || afterRemoval.getVoteCount() != 1) {
                throw new IllegalStateException(
                    "Participant and their vote were not removed together."
                );
            }

            // ==========================================
            // 7. RETRIEVE AND DISPLAY
            // ==========================================

            System.out.println("\n========== RETRIEVE AND DISPLAY ==========");

            activityManager.displayActivity(activityId);
            activityManager.displayActivitiesAtDestination(destination);

            System.out.println(
                "Total activities (all destinations): "
                + activityManager.getAllActivities().size()
            );

            activityManager.displayAllActivities();

            // ==========================================
            // 8. DELETE ACTIVITY (PERSISTS IN MYSQL)
            // ==========================================

            System.out.println("\n========== DELETE ACTIVITY ==========");

            activityManager.removeActivity(activityId, destination);

            System.out.println(
                "Activity exists after deletion: "
                + (activityDAO.getActivityById(activityId) != null)
            );

            System.out.println(
                "Activity count after delete: "
                + activityManager.getActivityCount()
            );

            if (activityDAO.getActivityById(activityId) != null) {
                throw new IllegalStateException(
                    "Activity was not deleted from the database."
                );
            }

            if (activityManager.getActivityCount() != initialCount) {
                throw new IllegalStateException(
                    "Activity count did not return to its seeded value."
                );
            }

            activityId = null;

            System.out.println(
                "\n========== ACTIVITY MANAGER INTEGRATION TEST COMPLETED =========="
            );

        } catch (Exception e) {

            System.out.println("\nACTIVITY MANAGER TEST FAILED!");
            e.printStackTrace();

            throw e;

        } finally {

            // Best-effort cleanup of the temporary activity.
            if (activityId != null) {
                try {
                    activityDAO.deleteActivity(activityId);
                    System.out.println(
                        "Temporary activity cleanup attempted: "
                        + activityId
                    );
                } catch (Exception cleanupError) {
                    System.out.println(
                        "Could not clean up temporary activity."
                    );
                    cleanupError.printStackTrace();
                }
            }
        }
    }
}
