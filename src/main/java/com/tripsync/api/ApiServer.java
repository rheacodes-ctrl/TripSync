
package com.tripsync.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ApiServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(PORT), 0
        );

        
server.createContext("/api/health", ApiServer::healthCheck);
server.createContext("/api/trips", new TripHandler());
server.createContext("/api/trips/", new DestinationHandler());
server.createContext("/api/destinations/", new ActivityHandler());
server.createContext("/api/activities/", new ActivityHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("================================");
        System.out.println("       TRIPSYNC API SERVER");
        System.out.println("================================");
        System.out.println("Server: http://localhost:" + PORT);
        System.out.println("Health: http://localhost:" + PORT + "/api/health");
        System.out.println("Trips:  http://localhost:" + PORT + "/api/trips");
    }

    private static void healthCheck(HttpExchange exchange)
            throws IOException {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405,
                "{\"error\":\"Method not allowed\"}");
            return;
        }

        sendResponse(exchange, 200,
            "{\"status\":\"UP\",\"message\":\"TripSync API is running\"}");
    }

    private static void sendResponse(
        HttpExchange exchange,
        int status,
        String response
    ) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Origin", "*"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
