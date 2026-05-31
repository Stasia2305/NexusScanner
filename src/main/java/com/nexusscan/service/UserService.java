package com.nexusscan.service;

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

    public UserService() {
        this(DAOFactory.getUserDAO());
    }

    public UserService(IUserDAO userDAO) {
        this.userDAO = userDAO;
        ensureDefaultAdmin();
    }

    private void ensureDefaultAdmin() {
        try {
            if (userDAO.getUser("admin") == null) {
                userDAO.addUser(new User("admin", "admin", User.Role.ADMIN));
            }
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }

    public User authenticate(String username, String password) throws SQLException {
        User user = userDAO.getUser(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        return userDAO.getAllUsers();
    }

    public void registerUser(User user) throws SQLException {
        userDAO.addUser(user);
    }

    public void deleteUser(String username) throws SQLException {
        userDAO.deleteUser(username);
    }
}
