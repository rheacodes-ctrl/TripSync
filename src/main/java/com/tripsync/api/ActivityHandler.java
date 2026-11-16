
package com.tripsync.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.tripsync.dao.ActivityDAO;
import com.tripsync.dao.DestinationDAO;
import com.tripsync.model.Activity;
import com.tripsync.model.Destination;
import com.tripsync.model.Traveler;
import com.tripsync.service.ActivityManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityHandler implements HttpHandler {

    private final ActivityDAO activityDAO = new ActivityDAO();
    private final DestinationDAO destinationDAO = new DestinationDAO();
    private final ActivityManager activityManager = new ActivityManager();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String[] parts = path.split("/");

            // ==========================================
            // 1. GET ACTIVITIES BY DESTINATION
            // GET /api/destinations/{destinationId}/activities
            // ==========================================

            if (parts.length == 5
                    && "api".equals(parts[1])
                    && "destinations".equals(parts[2])
                    && "activities".equals(parts[4])
                    && "GET".equalsIgnoreCase(method)) {

                List<Activity> activities =
                        activityDAO.getActivitiesByDestinationId(parts[3]);

                StringBuilder json = new StringBuilder("[");

                for (int i = 0; i < activities.size(); i++) {
                    if (i > 0) {
                        json.append(",");
                    }
                    json.append(toJson(activities.get(i)));
                }

                json.append("]");
                send(exchange, 200, json.toString());
                return;
            }

            // ==========================================
            // 2. CREATE ACTIVITY
            // POST /api/destinations/{destinationId}/activities
            // ==========================================

            if (parts.length == 5
                    && "api".equals(parts[1])
                    && "destinations".equals(parts[2])
                    && "activities".equals(parts[4])
                    && "POST".equalsIgnoreCase(method)) {

                String body = readBody(exchange);

                String name = getString(body, "name");
                String description = getString(body, "description");
                String date = getString(body, "activityDate");
                String time = getString(body, "activityTime");
                String cost = getNumber(body, "estimatedCost");

                if (name == null || date == null
                        || time == null || cost == null) {
                    send(exchange, 400,
                            "{\"error\":\"Missing required fields\"}");
                    return;
                }

                Destination destination =
                        destinationDAO.getDestinationById(parts[3]);

                if (destination == null) {
                    send(exchange, 404,
                            "{\"error\":\"Destination not found\"}");
                    return;
                }

                Activity activity = new Activity(
                        name,
                        description,
                        LocalDate.parse(date),
                        LocalTime.parse(time),
                        new BigDecimal(cost));

                Activity created =
                        activityManager.createActivity(destination, activity);

                send(exchange, 201, toJson(created));
                return;
            }

            // ==========================================
            // 3. ADD PARTICIPANT
            // POST /api/activities/{id}/participants
            // Body: {"userId":"USER_ID"}
            // ==========================================

            if (parts.length == 5
                    && "api".equals(parts[1])
                    && "activities".equals(parts[2])
                    && "participants".equals(parts[4])
                    && "POST".equalsIgnoreCase(method)) {

                String activityId = parts[3];

                Activity activity =
                        activityDAO.getActivityById(activityId);

                if (activity == null) {
                    send(exchange, 404,
                            "{\"error\":\"Activity not found\"}");
                    return;
                }

                String body = readBody(exchange);
                String userId = getString(body, "userId");

                if (userId == null || userId.isBlank()) {
                    send(exchange, 400,
                            "{\"error\":\"userId is required\"}");
                    return;
                }

                boolean added =
                        activityDAO.assignParticipant(activityId, userId);

                if (!added) {
                    send(exchange, 409,
                            "{\"error\":\"Participant could not be added\"}");
                    return;
                }

                send(exchange, 201,
                        "{\"message\":\"Participant added successfully\"}");
                return;
            }

            // ==========================================
            // 4. REMOVE PARTICIPANT
            // DELETE /api/activities/{id}/participants/{userId}
            // ==========================================

            if (parts.length == 6
                    && "api".equals(parts[1])
                    && "activities".equals(parts[2])
                    && "participants".equals(parts[4])
                    && "DELETE".equalsIgnoreCase(method)) {

                String activityId = parts[3];
                String userId = parts[5];

                Activity activity =
                        activityDAO.getActivityById(activityId);

                if (activity == null) {
                    send(exchange, 404,
                            "{\"error\":\"Activity not found\"}");
                    return;
                }

                boolean removed =
                        activityDAO.removeParticipant(activityId, userId);

                if (!removed) {
                    send(exchange, 404,
                            "{\"error\":\"Participant not found\"}");
                    return;
                }

                send(exchange, 200,
                        "{\"message\":\"Participant removed successfully\"}");
                return;
            }

            // ==========================================
            // 5. VOTE FOR ACTIVITY
            // POST /api/activities/{id}/vote
            // Body: {"userId":"USER_ID"}
            // ==========================================

            if (parts.length == 5
                    && "api".equals(parts[1])
                    && "activities".equals(parts[2])
                    && "vote".equals(parts[4])
                    && "POST".equalsIgnoreCase(method)) {

                String activityId = parts[3];

                Activity activity =
                        activityDAO.getActivityById(activityId);

                if (activity == null) {
                    send(exchange, 404,
                            "{\"error\":\"Activity not found\"}");
                    return;
                }

                String body = readBody(exchange);
                String userId = getString(body, "userId");

                if (userId == null || userId.isBlank()) {
                    send(exchange, 400,
                            "{\"error\":\"userId is required\"}");
                    return;
                }

                boolean voted =
                        activityDAO.voteForActivity(activityId, userId);

                if (!voted) {
                    send(exchange, 409,
                            "{\"error\":\"Vote could not be recorded\"}");
                    return;
                }

                send(exchange, 201,
                        "{\"message\":\"Vote recorded successfully\"}");
                return;
            }

            // ==========================================
            // 6. GET PARTICIPANTS
            // GET /api/activities/{id}/participants
            // ==========================================

            if (parts.length == 5
                    && "api".equals(parts[1])
                    && "activities".equals(parts[2])
                    && "participants".equals(parts[4])
                    && "GET".equalsIgnoreCase(method)) {

                String activityId = parts[3];

                Activity activity =
                        activityDAO.getActivityById(activityId);

                if (activity == null) {
                    send(exchange, 404,
                            "{\"error\":\"Activity not found\"}");
                    return;
                }

                List<Traveler> participants =
                        activityDAO.getParticipants(activityId);

                StringBuilder json = new StringBuilder("[");

                for (int i = 0; i < participants.size(); i++) {

                    if (i > 0) {
                        json.append(",");
                    }

                    Traveler traveler = participants.get(i);

                    json.append("{")
                            .append("\"userId\":\"")
                            .append(escape(traveler.getUserId()))
                            .append("\",")
                            .append("\"name\":\"")
                            .append(escape(traveler.getName()))
                            .append("\",")
                            .append("\"email\":\"")
                            .append(escape(traveler.getEmail()))
                            .append("\"}");
                }

                json.append("]");

                send(exchange, 200, json.toString());
                return;
            }

            // ==========================================
            // 7. GET VOTE COUNT
            // GET /api/activities/{id}/votes
            // ==========================================

            if (parts.length == 5
                    && "api".equals(parts[1])
                    && "activities".equals(parts[2])
                    && "votes".equals(parts[4])
                    && "GET".equalsIgnoreCase(method)) {

                String activityId = parts[3];

                Activity activity =
                        activityDAO.getActivityById(activityId);

                if (activity == null) {
                    send(exchange, 404,
                            "{\"error\":\"Activity not found\"}");
                    return;
                }

                int votes = activityDAO.getVoteCount(activityId);

                send(exchange, 200,
                        "{\"activityId\":\"" + escape(activityId)
                                + "\",\"voteCount\":" + votes + "}");
                return;
            }

            // ==========================================
            // 8. GET, PUT, PATCH, DELETE ACTIVITY
            // /api/activities/{id}
            // ==========================================

            if (parts.length == 4
                    && "api".equals(parts[1])
                    && "activities".equals(parts[2])) {

                String activityId = parts[3];

                // GET ACTIVITY
                if ("GET".equalsIgnoreCase(method)) {

                    Activity activity =
                            activityDAO.getActivityById(activityId);

                    if (activity == null) {
                        send(exchange, 404,
                                "{\"error\":\"Activity not found\"}");
                        return;
                    }

                    send(exchange, 200, toJson(activity));
                    return;
                }

                // UPDATE ACTIVITY
                if ("PUT".equalsIgnoreCase(method)) {

                    Activity existing =
                            activityDAO.getActivityById(activityId);

                    if (existing == null) {
                        send(exchange, 404,
                                "{\"error\":\"Activity not found\"}");
                        return;
                    }

                    String body = readBody(exchange);

                    String name = getString(body, "name");
                    String description = getString(body, "description");
                    String date = getString(body, "activityDate");
                    String time = getString(body, "activityTime");
                    String cost = getNumber(body, "estimatedCost");

                    if (name == null || date == null
                            || time == null || cost == null) {
                        send(exchange, 400,
                                "{\"error\":\"All activity fields are required\"}");
                        return;
                    }

                    Activity updated = new Activity(
                            activityId,
                            name,
                            description,
                            LocalDate.parse(date),
                            LocalTime.parse(time),
                            new BigDecimal(cost),
                            existing.getStatus());

                    boolean success = activityDAO.updateActivity(updated);

                    if (!success) {
                        send(exchange, 404,
                                "{\"error\":\"Activity not found\"}");
                        return;
                    }

                    send(exchange, 200, toJson(updated));
                    return;
                }

                // UPDATE ACTIVITY STATUS
                if ("PATCH".equalsIgnoreCase(method)) {

                    String body = readBody(exchange);
                    String status = getString(body, "status");

                    if (status == null) {
                        send(exchange, 400,
                                "{\"error\":\"Status is required\"}");
                        return;
                    }

                    Activity.Status newStatus =
                            Activity.Status.valueOf(status.toUpperCase());

                    activityManager.updateActivityStatus(
                            activityId, newStatus);

                    Activity updated =
                            activityDAO.getActivityById(activityId);

                    send(exchange, 200, toJson(updated));
                    return;
                }

                // DELETE ACTIVITY
                if ("DELETE".equalsIgnoreCase(method)) {

                    Activity existing =
                            activityDAO.getActivityById(activityId);

                    if (existing == null) {
                        send(exchange, 404,
                                "{\"error\":\"Activity not found\"}");
                        return;
                    }

                    boolean deleted =
                            activityDAO.deleteActivity(activityId);

                    if (!deleted) {
                        send(exchange, 500,
                                "{\"error\":\"Could not delete activity\"}");
                        return;
                    }

                    send(exchange, 200,
                            "{\"message\":\"Activity deleted successfully\"}");
                    return;
                }
            }

            // UNKNOWN ENDPOINT
            send(exchange, 404,
                    "{\"error\":\"Endpoint not found\"}");

        } catch (IllegalArgumentException e) {

            send(exchange, 400,
                    "{\"error\":\"" + escape(e.getMessage()) + "\"}");

        } catch (Exception e) {

            e.printStackTrace();

            send(exchange, 500,
                    "{\"error\":\"Internal server error\"}");
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private String readBody(HttpExchange exchange) throws IOException {

        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private String getString(String body, String key) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key)
                        + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

        Matcher matcher = pattern.matcher(body);

        return matcher.find() ? matcher.group(1) : null;
    }

    private String getNumber(String body, String key) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key)
                        + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

        Matcher matcher = pattern.matcher(body);

        return matcher.find() ? matcher.group(1) : null;
    }

    private String toJson(Activity activity) {

        return "{"
                + "\"activityId\":\""
                + escape(activity.getActivityId()) + "\","

                + "\"name\":\""
                + escape(activity.getName()) + "\","

                + "\"description\":\""
                + escape(activity.getDescription()) + "\","

                + "\"activityDate\":\""
                + activity.getActivityDate() + "\","

                + "\"activityTime\":\""
                + activity.getActivityTime() + "\","

                + "\"estimatedCost\":"
                + activity.getEstimatedCost() + ","

                + "\"status\":\""
                + activity.getStatus() + "\","

                + "\"participantCount\":"
                + activity.getParticipants().size() + ","

                + "\"voteCount\":"
                + activity.getVoteCount()

                + "}";
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void send(
            HttpExchange exchange,
            int status,
            String response) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
