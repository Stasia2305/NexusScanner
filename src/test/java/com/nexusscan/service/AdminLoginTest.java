package com.nexusscan.service;

import com.nexusscan.model.User;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

public class AdminLoginTest {

    @Test
    void testAdminExistsAndAuthenticates() throws SQLException {
        UserService userService = new UserService();
        User admin = userService.authenticate("admin", "admin");
        assertNotNull(admin, "Admin should be able to authenticate with 'admin'/'admin'");
        assertEquals(User.Role.ADMIN, admin.getRole());
    }
}
