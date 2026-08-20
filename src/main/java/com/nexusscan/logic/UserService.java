package com.nexusscan.logic;

import com.nexusscan.dal.DAOFactory;
import com.nexusscan.dal.interfaces.IUserDAO;
import com.nexusscan.model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Service class for User-related business logic.
 * This represents the Logic Layer in the 3-layered architecture.
 */
public class UserService {
    private final IUserDAO userDAO;

    /**
     * Default constructor that retrieves the IUserDAO implementation from the DAOFactory.
     */
    public UserService() {
        this(DAOFactory.getUserDAO());
    }

    /**
     * Dependency injection constructor to allow custom IUserDAO implementations (e.g. for unit testing).
     *
     * @param userDAO The IUserDAO instance to use.
     */
    public UserService(IUserDAO userDAO) {
        this.userDAO = userDAO;
        ensureDefaultAdmin();
    }

    /**
     * Ensures that the default admin user is seeded in the system.
     */
    private void ensureDefaultAdmin() {
        try {
            if (userDAO.getUser("admin") == null) {
                userDAO.addUser(new User("admin", "admin", User.Role.ADMIN));
            }
        } catch (SQLException e) {
            System.err.println("Failed to ensure default admin exists: " + e.getMessage());
        }
    }

    /**
     * Authenticates a user based on username and password.
     * In offline mode, this utilizes fallbacks in the UserDAO for 'admin' and 'user'.
     *
     * @param username The username of the user.
     * @param password The password of the user.
     * @return The authenticated User object if successful, or null if credentials are invalid.
     * @throws SQLException If a database error occurs during authentication.
     */
    public User authenticate(String username, String password) throws SQLException {
        User user = userDAO.getUser(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * Retrieves all system users.
     *
     * @return A list of all available User objects.
     * @throws SQLException If a database error occurs.
     */
    public List<User> getAllUsers() throws SQLException {
        return userDAO.getAllUsers();
    }

    /**
     * Registers a new user in the system.
     *
     * @param user The User object containing details of the new user.
     * @throws SQLException If a database error occurs.
     */
    public void registerUser(User user) throws SQLException {
        userDAO.addUser(user);
    }

    /**
     * Deletes a user account from the system by username.
     *
     * @param username The username of the account to delete.
     * @throws SQLException If a database error occurs.
     */
    public void deleteUser(String username) throws SQLException {
        userDAO.deleteUser(username);
    }
}
