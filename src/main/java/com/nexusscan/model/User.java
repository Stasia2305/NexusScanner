package com.nexusscan.model;

/**
 * Represents a system user with authentication credentials and a specific role.
 */
public class User {
    /**
     * Authorization roles defining user permissions.
     */
    public enum Role {
        ADMIN, // Access to administrative tools and profile management
        USER   // Access to scanning workspace
    }

    private String username;
    private String password;
    private Role role;

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }
}
