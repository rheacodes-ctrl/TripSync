
package com.tripsync.service;

import com.tripsync.model.Activity;
import com.tripsync.model.Destination;
import com.tripsync.model.Traveler;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActivityManager {

    // Stores activities using their unique IDs
    private final Map<String, Activity> activities;

    // Constructor
    public ActivityManager() {
        this.activities = new LinkedHashMap<>();
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

        destination.addActivity(activity);

        activities.put(activity.getActivityId(), activity);

        System.out.println(
                "Activity created successfully: " + activity.getName());

        return activity;
    }

    // Find activity by ID
    public Activity getActivityById(String activityId) {

        Activity activity = activities.get(activityId);

        if (activity == null) {
            throw new IllegalArgumentException(
                    "Activity not found: " + activityId);
        }

        return activity;
    }

    // Assign traveler to activity
    public void assignTraveler(
            String activityId, Traveler traveler) {

        Activity activity = getActivityById(activityId);

        activity.addParticipant(traveler);

        System.out.println(
                traveler.getName() + " assigned to "
                        + activity.getName());
    }

    // Remove traveler from activity
    public void removeTraveler(
            String activityId, String userId) {

        Activity activity = getActivityById(activityId);

        activity.removeParticipant(userId);

        System.out.println("Traveler removed from activity.");
    }

    // Vote for activity
    public void voteForActivity(
            String activityId, String userId) {

        Activity activity = getActivityById(activityId);

        activity.vote(userId);

        System.out.println("Vote recorded successfully!");
    }

    // Update activity status
    public void updateActivityStatus(
            String activityId, Activity.Status status) {

        Activity activity = getActivityById(activityId);

        activity.updateStatus(status);

        System.out.println("Activity status updated!");
    }

    // Remove activity from a destination
    public void removeActivity(
            String activityId, Destination destination) {

        Activity activity = getActivityById(activityId);

        destination.removeActivity(activityId);

        activities.remove(activityId);

        System.out.println(
                activity.getName() + " removed successfully!");
    }

    // Display one activity
    public void displayActivity(String activityId) {

        Activity activity = getActivityById(activityId);

        activity.displayActivity();
    }

    // Display all activities
    public void displayAllActivities() {

        if (activities.isEmpty()) {
            System.out.println("No activities available.");
            return;
        }

        System.out.println("\n========== ALL ACTIVITIES ==========");

        for (Activity activity : activities.values()) {
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

        destination.displayActivities();
    }

    // Get all activities
    public Collection<Activity> getAllActivities() {

        return java.util.Collections.unmodifiableCollection(
                activities.values());
    }

    // Get total activity count
    public int getActivityCount() {
        return activities.size();
    }
}
