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
    private String email;
    private Role role;

    /**
     * Constructs a new User with username, password, and role, setting email to null.
     *
     * @param username The user's username.
     * @param password The user's password.
     * @param role     The user's role access level.
     */
    public User(String username, String password, Role role) {
        this(username, password, null, role);
    }

    /**
     * Constructs a new User with all details: username, password, email, and role.
     *
     * @param username The user's username.
     * @param password The user's password.
     * @param email    The user's email address.
     * @param role     The user's role access level.
     */
    public User(String username, String password, String email, Role role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    /**
     * Gets the username of the user.
     *
     * @return The username string.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password of the user.
     *
     * @return The password string.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the email address of the user.
     *
     * @return The email string.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the user.
     *
     * @param email The new email address.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the role of the user.
     *
     * @return The user role access level.
     */
    public Role getRole() {
        return role;
    }
}
