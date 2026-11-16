package com.tripsync.service;

import com.tripsync.dao.ActivityDAO;
import com.tripsync.model.Activity;
import com.tripsync.model.Destination;
import com.tripsync.model.Traveler;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ActivityManager {

    // Database access object replacing the old in-memory map.
    private final ActivityDAO activityDAO;

    // Constructor
    public ActivityManager() {
        this.activityDAO = new ActivityDAO();
    }

    // Create an activity at a destination
    public Activity createActivity(
            Destination destination,
            Activity activity) {

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Destination cannot be null");
        }

        if (activity == null) {
            throw new IllegalArgumentException(
                    "Activity cannot be null");
        }

        try {
            boolean created = activityDAO.createActivity(
                    destination.getDestinationId(), activity);

            if (!created) {
                throw new IllegalStateException(
                        "Activity could not be saved.");
            }

            // Keep the in-memory destination synchronized.
            destination.addActivity(activity);

            System.out.println(
                    "Activity created successfully: " + activity.getName());

            return activity;

        } catch (SQLException e) {
            throw databaseError("creating activity", e);
        }
    }

    // Find activity by ID
    public Activity getActivityById(String activityId) {

        try {
            Activity activity = activityDAO.getActivityById(activityId);

            if (activity == null) {
                throw new IllegalArgumentException(
                        "Activity not found: " + activityId);
            }

            return activity;

        } catch (SQLException e) {
            throw databaseError("retrieving activity", e);
        }
    }

    // Assign traveler to activity
    public void assignTraveler(
            String activityId, Traveler traveler) {

        if (traveler == null) {
            throw new IllegalArgumentException(
                    "Traveler cannot be null");
        }

        Activity activity = getActivityById(activityId);

        try {
            boolean assigned = activityDAO.assignParticipant(
                    activityId, traveler.getUserId());

            if (!assigned) {
                throw new IllegalStateException(
                        "Traveler could not be assigned.");
            }

            System.out.println(
                    traveler.getName() + " assigned to "
                            + activity.getName());

        } catch (SQLException e) {
            throw databaseError("assigning traveler", e);
        }
    }

    // Remove traveler from activity
    public void removeTraveler(
            String activityId, String userId) {

        try {
            boolean removed = activityDAO.removeParticipant(
                    activityId, userId);

            if (!removed) {
                throw new IllegalArgumentException(
                        "Participant not found.");
            }

            System.out.println("Traveler removed from activity.");

        } catch (SQLException e) {
            throw databaseError("removing traveler", e);
        }
    }

    // Vote for activity
    public void voteForActivity(
            String activityId, String userId) {

        try {
            boolean voted = activityDAO.voteForActivity(
                    activityId, userId);

            if (!voted) {
                throw new IllegalStateException(
                        "Vote could not be recorded.");
            }

            System.out.println("Vote recorded successfully!");

        } catch (SQLException e) {
            throw databaseError("recording vote", e);
        }
    }

    // Update activity status
    public void updateActivityStatus(
            String activityId, Activity.Status status) {

        try {
            boolean updated = activityDAO.updateActivityStatus(
                    activityId, status);

            if (!updated) {
                throw new IllegalArgumentException(
                        "Activity not found: " + activityId);
            }

            System.out.println("Activity status updated!");

        } catch (SQLException e) {
            throw databaseError("updating activity status", e);
        }
    }

    // Remove activity from a destination
    public void removeActivity(
            String activityId, Destination destination) {

        Activity activity = getActivityById(activityId);

        try {
            boolean deleted = activityDAO.deleteActivity(activityId);

            if (!deleted) {
                throw new IllegalStateException(
                        "Activity could not be deleted.");
            }

            // Best-effort in-memory synchronization. A destination
            // loaded from the database has an empty activity list, so
            // only remove the activity when it is actually present.
            if (destination != null) {
                boolean inMemory = destination.getActivities().stream()
                        .anyMatch(a -> a.getActivityId().equals(activityId));

                if (inMemory) {
                    destination.removeActivity(activityId);
                }
            }

            System.out.println(
                    activity.getName() + " removed successfully!");

        } catch (SQLException e) {
            throw databaseError("deleting activity", e);
        }
    }

    // Display one activity
    public void displayActivity(String activityId) {

        Activity activity = getActivityById(activityId);

        activity.displayActivity();
    }

    // Display all activities
    public void displayAllActivities() {

        Collection<Activity> activities = getAllActivities();

        if (activities.isEmpty()) {
            System.out.println("No activities available.");
            return;
        }

        System.out.println("\n========== ALL ACTIVITIES ==========");

        for (Activity activity : activities) {
            activity.displayActivity();
        }
    }

    // Display activities at a destination
    public void displayActivitiesAtDestination(
            Destination destination) {

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Destination cannot be null");
        }

        List<Activity> activities;

        try {
            activities = activityDAO.getActivitiesByDestinationId(
                    destination.getDestinationId());

        } catch (SQLException e) {
            throw databaseError("retrieving activities", e);
        }

        System.out.println("\nActivities at " + destination.getName());

        if (activities.isEmpty()) {
            System.out.println("No activities planned yet.");
            return;
        }

        for (Activity activity : activities) {
            activity.displayActivity();
        }
    }

    // Get all activities
    public Collection<Activity> getAllActivities() {

        try {
            return Collections.unmodifiableCollection(
                    activityDAO.getAllActivities());

        } catch (SQLException e) {
            throw databaseError("retrieving activities", e);
        }
    }

    // Get total activity count
    public int getActivityCount() {
        return getAllActivities().size();
    }

    // ==========================================
    // DATABASE ERROR HANDLING
    // ==========================================

    private IllegalStateException databaseError(
            String operation,
            SQLException cause) {

        return new IllegalStateException(
                "Database error while " + operation
                        + ": " + cause.getMessage(),
                cause);
    }
}
