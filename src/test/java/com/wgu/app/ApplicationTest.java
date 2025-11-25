package com.wgu.app;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for Application main class.
 */
public class ApplicationTest {
    
    @Test
    public void testApplicationMainMethod() {
        // Test that main method runs without throwing exception
        assertDoesNotThrow(() -> {
            Application.main(new String[]{});
        }, "Application main method should run without throwing exception");
    }
    
    @Test
    public void testApplicationMainMethodWithArgs() {
        // Test that main method handles arguments
        assertDoesNotThrow(() -> {
            Application.main(new String[]{"arg1", "arg2"});
        }, "Application main method should handle arguments");
    }
}

