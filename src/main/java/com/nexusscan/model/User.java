package com.nexusscan.model;

/**
 * A User is a person who can log into the system.
 */
public class User {
    /**
     * Different levels of access for users.
     */
    public enum Role {
        ADMIN, // Can manage profiles and users
        USER   // Can only scan and review documents
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
