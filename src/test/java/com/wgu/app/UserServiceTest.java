package com.wgu.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for UserService.
 */
public class UserServiceTest {
    
    private UserService userService;
    
    @BeforeEach
    public void setUp() {
        userService = new UserService();
    }
    
    @Test
    public void testAuthenticateWithValidCredentials() {
        // Test with valid admin credentials
        boolean result = userService.authenticate("admin", "secret123");
        assertTrue(result, "Valid admin credentials should authenticate successfully");
    }
    
    @Test
    public void testAuthenticateWithValidUserCredentials() {
        // Test with valid user credentials
        boolean result = userService.authenticate("john", "password");
        assertTrue(result, "Valid user credentials should authenticate successfully");
    }
    
    @Test
    public void testAuthenticateWithInvalidCredentials() {
        // Test with invalid credentials
        boolean result = userService.authenticate("admin", "wrongpassword");
        assertFalse(result, "Invalid credentials should not authenticate");
    }
    
    @Test
    public void testAuthenticateWithNonExistentUser() {
        // Test with non-existent user
        boolean result = userService.authenticate("nonexistent", "password");
        assertFalse(result, "Non-existent user should not authenticate");
    }
    
    @Test
    public void testAuthenticateWithNullUsername() {
        // Test with null username
        boolean result = userService.authenticate(null, "password");
        assertFalse(result, "Null username should not authenticate");
    }
    
    @Test
    public void testAuthenticateWithNullPassword() {
        // Test with null password
        boolean result = userService.authenticate("admin", null);
        assertFalse(result, "Null password should not authenticate");
    }
}

