package com.wgu.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for NetworkUtils.
 */
public class NetworkUtilsTest {
    
    private NetworkUtils networkUtils;
    
    @BeforeEach
    public void setUp() {
        networkUtils = new NetworkUtils();
    }
    
    @Test
    public void testPingHostWithLocalhost() {
        // Test pinging localhost
        String result = networkUtils.pingHost("localhost");
        assertNotNull(result, "Ping result should not be null");
        // Result might be empty on some systems, so just check it doesn't throw
    }
    
    @Test
    public void testPingHostWithInvalidHost() {
        // Test pinging invalid host (should handle gracefully)
        String result = networkUtils.pingHost("invalid-host-that-does-not-exist-12345");
        assertNotNull(result, "Ping result should not be null even for invalid host");
    }
    
    @Test
    public void testPingHostWithNull() {
        // Test with null host
        String result = networkUtils.pingHost(null);
        assertNotNull(result, "Ping result should not be null");
    }
}

