package com.nexusscan.dal.interfaces;

import com.nexusscan.model.User;
import java.sql.SQLException;
import java.util.List;

public interface IUserDAO {
    User getUser(String username) throws SQLException;
    List<User> getAllUsers() throws SQLException;
    void addUser(User user) throws SQLException;
    void updateUser(User user) throws SQLException;
    void deleteUser(String username) throws SQLException;
}
