
package com.tripsync.model;

import java.util.UUID;

public abstract class User {

    private final String userId;
    private String name;
    private String email;
    private String password;

    // Constructor
    public User(String name, String email, String password) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                "Password must contain at least 6 characters"
            );
        }

        this.userId = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    // Setters
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid name");
        }
        this.name = name;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        this.email = email;
    }

    // Abstract methods
    public abstract String getRole();

    public abstract void displayProfile();
}
