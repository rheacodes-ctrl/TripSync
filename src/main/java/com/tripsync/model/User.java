
package com.tripsync.model;

import java.util.UUID;

public abstract class User {

    private final String userId;
    private String name;
    private String email;
    private String password;

    // Normal constructor: creates a new user
    public User(String name, String email, String password) {

        this(UUID.randomUUID().toString(), name, email, password);
    }

    // Constructor used internally for new users
    private User(String userId, String name, String email, String password) {

        validateUserId(userId);
        validateName(name);
        validateEmail(email);

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                    "Password must contain at least 6 characters"
            );
        }

        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Constructor for loading users from database
    protected User(
            String userId,
            String name,
            String email,
            boolean fromDatabase
    ) {
        validateUserId(userId);
        validateName(name);
        validateEmail(email);

        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = null;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Invalid user ID");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name) {
        validateName(name);
        this.name = name;
    }

    public void setEmail(String email) {
        validateEmail(email);
        this.email = email;
    }

    public abstract String getRole();

    public abstract void displayProfile();
}
