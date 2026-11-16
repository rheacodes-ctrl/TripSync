
package com.tripsync.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tripsync.model.Trip;
import com.tripsync.model.Destination;
import com.tripsync.service.TripManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DestinationHandler implements HttpHandler {

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

        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            // Expected: /api/trips/{tripId}/destinations
            if (parts.length != 5
                    || !"api".equals(parts[1])
                    || !"trips".equals(parts[2])
                    || !"destinations".equals(parts[4])
                    || parts[3].isBlank()) {

                sendJson(exchange, 400,
                    "{\"error\":\"Invalid destination endpoint\"}");
                return;
            }

            String tripId = parts[3];

            Trip trip = tripManager.getTripById(tripId);
            List<Destination> destinations = trip.getDestinations();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            for (Destination destination : destinations) {

                if (!first) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"destinationId\":\"")
                    .append(escape(destination.getDestinationId()))
                    .append("\",")
                    .append("\"name\":\"")
                    .append(escape(destination.getName()))
                    .append("\",")
                    .append("\"city\":\"")
                    .append(escape(destination.getCity()))
                    .append("\",")
                    .append("\"country\":\"")
                    .append(escape(destination.getCountry()))
                    .append("\",")
                    .append("\"description\":\"")
                    .append(escape(destination.getDescription()))
                    .append("\",")
                    .append("\"arrivalDate\":\"")
                    .append(destination.getArrivalDate())
                    .append("\",")
                    .append("\"departureDate\":\"")
                    .append(destination.getDepartureDate())
                    .append("\"")
                    .append("}");

                first = false;
            }

            json.append("]");

            sendJson(exchange, 200, json.toString());

        } catch (IllegalArgumentException e) {

            sendJson(exchange, 404,
                "{\"error\":\"" + escape(e.getMessage()) + "\"}");

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(exchange, 500,
                "{\"error\":\"Internal server error\"}");
        }
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

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
