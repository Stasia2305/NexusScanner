package com.nexusscan.dal.interfaces;

import com.nexusscan.model.User;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for Data Access Objects managing system users.
 */
public interface IUserDAO {
    /**
     * Retrieves a user by their username.
     *
     * @param username The username of the user.
     * @return The User object, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    User getUser(String username) throws SQLException;

    /**
     * Retrieves all users registered in the system.
     *
     * @return A list of all User objects.
     * @throws SQLException If a database error occurs.
     */
    List<User> getAllUsers() throws SQLException;

    /**
     * Adds a new user to the system.
     *
     * @param user The User object containing the user's details.
     * @throws SQLException If a database error occurs.
     */
    void addUser(User user) throws SQLException;

    /**
     * Deletes a user from the system by username.
     *
     * @param username The username of the user to delete.
     * @throws SQLException If a database error occurs.
     */
    void deleteUser(String username) throws SQLException;
}
