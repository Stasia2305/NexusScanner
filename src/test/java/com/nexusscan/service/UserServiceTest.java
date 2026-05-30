package com.nexusscan.service;

import com.nexusscan.dal.interfaces.IUserDAO;
import com.nexusscan.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {
    private IUserDAO mockUserDAO;
    private UserService userService;

    @BeforeEach
    void setUp() throws SQLException {
        mockUserDAO = mock(IUserDAO.class);
        // Ensure getUser("admin") returns something to avoid default admin creation in constructor if needed
        // but since we want to test authenticate, let's just mock it.
        userService = new UserService(mockUserDAO);
    }

    @Test
    void testAuthenticateSuccess() throws SQLException {
        User testUser = new User("test", "pass", User.Role.USER);
        when(mockUserDAO.getUser("test")).thenReturn(testUser);

        User result = userService.authenticate("test", "pass");

        assertNotNull(result);
        assertEquals("test", result.getUsername());
    }

    @Test
    void testAuthenticateFailure() throws SQLException {
        when(mockUserDAO.getUser("test")).thenReturn(null);

        User result = userService.authenticate("test", "pass");

        assertNull(result);
    }

    @Test
    void testAuthenticateWrongPassword() throws SQLException {
        User testUser = new User("test", "pass", User.Role.USER);
        when(mockUserDAO.getUser("test")).thenReturn(testUser);

        User result = userService.authenticate("test", "wrong");

        assertNull(result);
    }
}
