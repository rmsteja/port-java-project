package com.wgu.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;

/**
 * Test cases for DataProcessor.
 */
public class DataProcessorTest {
    
    private DataProcessor processor;
    
    @BeforeEach
    public void setUp() {
        processor = new DataProcessor();
    }
    
    @Test
    public void testProcessDataWithValidInput() {
        // Test with valid small data
        byte[] input = {1, 2, 3, 4, 5};
        assertDoesNotThrow(() -> processor.process(input), 
            "Processing valid data should not throw exception");
    }
    
    @Test
    public void testProcessDataWithEmptyArray() {
        // Test with empty array
        byte[] input = {};
        assertDoesNotThrow(() -> processor.process(input), 
            "Processing empty array should not throw exception");
    }

}



