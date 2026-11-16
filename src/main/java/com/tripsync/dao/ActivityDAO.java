
package com.tripsync.dao;

import com.tripsync.config.DBConnection;
import com.tripsync.model.Activity;
import com.tripsync.model.Destination;
import com.tripsync.model.Traveler;
import com.tripsync.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityDAO {

    private final UserDAO userDAO = new UserDAO();
    private final DestinationDAO destinationDAO = new DestinationDAO();

    // CREATE ACTIVITY
    public boolean createActivity(String destinationId, Activity activity)
            throws SQLException {

        if (destinationId == null || destinationId.isBlank()
                || activity == null) {
            throw new IllegalArgumentException(
                    "Destination ID and activity are required");
        }

        Destination destination =
                destinationDAO.getDestinationById(destinationId);

        if (destination == null) {
            throw new IllegalArgumentException("Destination not found");
        }

        // Validate activity date against destination dates
        if (activity.getActivityDate().isBefore(destination.getArrivalDate())
                || activity.getActivityDate().isAfter(destination.getDepartureDate())) {
            throw new IllegalArgumentException(
                    "Activity date must fall within destination dates");
        }

        String sql = """
                INSERT INTO activities
                (activity_id, destination_id, name, description,
                 activity_date, activity_time, estimated_cost, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, activity.getActivityId());
            stmt.setString(2, destinationId);
            stmt.setString(3, activity.getName());
            stmt.setString(4, activity.getDescription());
            stmt.setDate(5, Date.valueOf(activity.getActivityDate()));
            stmt.setTime(6, Time.valueOf(activity.getActivityTime()));
            stmt.setBigDecimal(7, activity.getEstimatedCost());
            stmt.setString(8, activity.getStatus().name());

            return stmt.executeUpdate() > 0;
        }
    }

    // GET ACTIVITY BY ID
    public Activity getActivityById(String activityId)
            throws SQLException {

        String sql = """
                SELECT *
                FROM activities
                WHERE activity_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, activityId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    Activity activity = mapActivity(rs);

                    loadParticipants(activity);
                    loadVotes(activity);

                    return activity;
                }
            }
        }

        return null;
    }

    // GET ALL ACTIVITIES FOR A DESTINATION
    public List<Activity> getActivitiesByDestinationId(String destinationId)
            throws SQLException {

        List<Activity> activities = new ArrayList<>();

        String sql = """
                SELECT *
                FROM activities
                WHERE destination_id = ?
                ORDER BY activity_date, activity_time
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, destinationId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    activities.add(mapActivity(rs));
                }
            }
        }

        // Load participants and votes after closing the first query.
        for (Activity activity : activities) {
            loadParticipants(activity);
            loadVotes(activity);
        }

        return activities;
    }

    // GET ALL ACTIVITIES ACROSS EVERY DESTINATION
    public List<Activity> getAllActivities()
            throws SQLException {

        List<Activity> activities = new ArrayList<>();

        String sql = """
                SELECT *
                FROM activities
                ORDER BY activity_date, activity_time
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                activities.add(mapActivity(rs));
            }
        }

        // Load participants and votes after closing the first query.
        for (Activity activity : activities) {
            loadParticipants(activity);
            loadVotes(activity);
        }

        return activities;
    }

    // UPDATE ACTIVITY
    public boolean updateActivity(Activity activity)
            throws SQLException {

        if (activity == null) {
            throw new IllegalArgumentException("Activity cannot be null");
        }

        String sql = """
                UPDATE activities
                SET name = ?,
                    description = ?,
                    activity_date = ?,
                    activity_time = ?,
                    estimated_cost = ?,
                    status = ?
                WHERE activity_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, activity.getName());
            stmt.setString(2, activity.getDescription());
            stmt.setDate(3, Date.valueOf(activity.getActivityDate()));
            stmt.setTime(4, Time.valueOf(activity.getActivityTime()));
            stmt.setBigDecimal(5, activity.getEstimatedCost());
            stmt.setString(6, activity.getStatus().name());
            stmt.setString(7, activity.getActivityId());

            return stmt.executeUpdate() > 0;
        }
    }

    // UPDATE ACTIVITY STATUS
    public boolean updateActivityStatus(
            String activityId, Activity.Status status)
            throws SQLException {

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        String sql = """
                UPDATE activities
                SET status = ?
                WHERE activity_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setString(2, activityId);

            return stmt.executeUpdate() > 0;
        }
    }

    // DELETE ACTIVITY
    public boolean deleteActivity(String activityId)
            throws SQLException {

        String deleteVotes = """
                DELETE FROM activity_votes
                WHERE activity_id = ?
                """;

        String deleteParticipants = """
                DELETE FROM activity_participants
                WHERE activity_id = ?
                """;

        String deleteActivity = """
                DELETE FROM activities
                WHERE activity_id = ?
                """;

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                try (PreparedStatement stmt =
                             conn.prepareStatement(deleteVotes)) {
                    stmt.setString(1, activityId);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt =
                             conn.prepareStatement(deleteParticipants)) {
                    stmt.setString(1, activityId);
                    stmt.executeUpdate();
                }

                int deleted;

                try (PreparedStatement stmt =
                             conn.prepareStatement(deleteActivity)) {
                    stmt.setString(1, activityId);
                    deleted = stmt.executeUpdate();
                }

                conn.commit();
                return deleted > 0;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ASSIGN PARTICIPANT
    public boolean assignParticipant(String activityId, String userId)
            throws SQLException {

        Activity activity = getActivityById(activityId);

        if (activity == null) {
            throw new IllegalArgumentException("Activity not found");
        }

        User user = userDAO.getUserById(userId);

        if (!(user instanceof Traveler)) {
            throw new IllegalArgumentException(
                    "Only travelers can be assigned to activities");
        }

        String sql = """
                INSERT INTO activity_participants
                (activity_id, user_id)
                VALUES (?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, activityId);
            stmt.setString(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    // REMOVE PARTICIPANT
    public boolean removeParticipant(String activityId, String userId)
            throws SQLException {

        String deleteVotes = """
                DELETE FROM activity_votes
                WHERE activity_id = ? AND user_id = ?
                """;

        String deleteParticipant = """
                DELETE FROM activity_participants
                WHERE activity_id = ? AND user_id = ?
                """;

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                try (PreparedStatement stmt =
                             conn.prepareStatement(deleteVotes)) {
                    stmt.setString(1, activityId);
                    stmt.setString(2, userId);
                    stmt.executeUpdate();
                }

                int deleted;

                try (PreparedStatement stmt =
                             conn.prepareStatement(deleteParticipant)) {
                    stmt.setString(1, activityId);
                    stmt.setString(2, userId);
                    deleted = stmt.executeUpdate();
                }

                conn.commit();
                return deleted > 0;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // VOTE FOR AN ACTIVITY
    public boolean voteForActivity(String activityId, String userId)
            throws SQLException {

        String checkParticipant = """
                SELECT 1
                FROM activity_participants
                WHERE activity_id = ? AND user_id = ?
                """;

        String checkVote = """
                SELECT 1
                FROM activity_votes
                WHERE activity_id = ? AND user_id = ?
                """;

        String insertVote = """
                INSERT INTO activity_votes
                (vote_id, activity_id, user_id)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                // Check whether the user is a participant.
                try (PreparedStatement stmt =
                             conn.prepareStatement(checkParticipant)) {

                    stmt.setString(1, activityId);
                    stmt.setString(2, userId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Only assigned participants can vote");
                        }
                    }
                }

                // Prevent duplicate votes.
                try (PreparedStatement stmt =
                             conn.prepareStatement(checkVote)) {

                    stmt.setString(1, activityId);
                    stmt.setString(2, userId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "You have already voted");
                        }
                    }
                }

                // Save vote.
                try (PreparedStatement stmt =
                             conn.prepareStatement(insertVote)) {

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, activityId);
                    stmt.setString(3, userId);

                    int inserted = stmt.executeUpdate();

                    conn.commit();
                    return inserted > 0;
                }

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // GET VOTE COUNT
    public int getVoteCount(String activityId)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM activity_votes
                WHERE activity_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, activityId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    // GET PARTICIPANTS
    public List<Traveler> getParticipants(String activityId)
            throws SQLException {

        List<Traveler> participants = new ArrayList<>();

        String sql = """
                SELECT user_id
                FROM activity_participants
                WHERE activity_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, activityId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    User user = userDAO.getUserById(
                            rs.getString("user_id"));

                    if (user instanceof Traveler) {
                        participants.add((Traveler) user);
                    }
                }
            }
        }

        return participants;
    }

    // GET ACTIVITY COUNT FOR A DESTINATION
    public int getActivityCount(String destinationId)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM activities
                WHERE destination_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, destinationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    // LOAD PARTICIPANTS INTO ACTIVITY OBJECT
    private void loadParticipants(Activity activity)
            throws SQLException {

        List<Traveler> participants =
                getParticipants(activity.getActivityId());

        for (Traveler traveler : participants) {
            activity.addParticipant(traveler);
        }
    }

    // LOAD VOTES INTO ACTIVITY OBJECT
    private void loadVotes(Activity activity)
            throws SQLException {

        String sql = """
                SELECT user_id
                FROM activity_votes
                WHERE activity_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, activity.getActivityId());

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    activity.vote(rs.getString("user_id"));
                }
            }
        }
    }

    // MAP DATABASE ROW TO ACTIVITY OBJECT
    private Activity mapActivity(ResultSet rs)
            throws SQLException {

        Time time = rs.getTime("activity_time");

        // The current Activity model requires a time.
        if (time == null) {
            throw new SQLException(
                    "Activity " + rs.getString("activity_id")
                    + " has no activity_time in the database");
        }

        BigDecimal cost = rs.getBigDecimal("estimated_cost");

        if (cost == null) {
            cost = BigDecimal.ZERO;
        }

        return new Activity(
                rs.getString("activity_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDate("activity_date").toLocalDate(),
                time.toLocalTime(),
                cost,
                Activity.Status.valueOf(rs.getString("status"))
        );
    }
}
