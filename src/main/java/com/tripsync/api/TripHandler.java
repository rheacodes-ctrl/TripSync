
package com.tripsync.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tripsync.model.Trip;
import com.tripsync.service.TripManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class TripHandler implements HttpHandler {

    private final TripManager tripManager = new TripManager();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                "{\"error\":\"Method not allowed\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String basePath = "/api/trips";

        try {
            if (path.equals(basePath) || path.equals(basePath + "/")) {
                Collection<Trip> trips = tripManager.getAllTrips();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                for (Trip trip : trips) {
                    if (!first) json.append(",");
                    json.append(tripToJson(trip));
                    first = false;
                }

                json.append("]");
                sendJson(exchange, 200, json.toString());
                return;
            }

            if (path.startsWith(basePath + "/")) {
                String tripId = path.substring(
                    (basePath + "/").length()
                );

                if (tripId.isBlank() || tripId.contains("/")) {
                    sendJson(exchange, 400,
                        "{\"error\":\"Invalid trip ID\"}");
                    return;
                }

                Trip trip = tripManager.getTripById(tripId);
                sendJson(exchange, 200, tripToJson(trip));
                return;
            }

            sendJson(exchange, 404,
                "{\"error\":\"Endpoint not found\"}");

        } catch (IllegalArgumentException e) {
            sendJson(exchange, 404,
                "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500,
                "{\"error\":\"Internal server error\"}");
        }
    }

    private String tripToJson(Trip trip) {
        return "{"
            + "\"tripId\":\"" + escape(trip.getTripId()) + "\","
            + "\"tripName\":\"" + escape(trip.getTripName()) + "\","
            + "\"description\":\"" + escape(trip.getDescription()) + "\","
            + "\"startDate\":\"" + trip.getStartDate() + "\","
            + "\"endDate\":\"" + trip.getEndDate() + "\","
            + "\"budget\":" + trip.getBudget().toPlainString() + ","
            + "\"adminName\":\"" + escape(trip.getAdmin().getName()) + "\","
            + "\"destinationCount\":" + trip.getDestinations().size()
            + "}";
    }

    private String escape(String value) {
        if (value == null) return "";

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Origin", "*"
        );
        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Methods", "GET, OPTIONS"
        );
        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Headers", "Content-Type"
        );
    }

    private void sendJson(
        HttpExchange exchange,
        int status,
        String response
    ) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
