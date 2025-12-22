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
    
    @Test
    public void testWriteToBufferWithValidData() {
        // Test writing valid data to buffer
        byte[] data = {10, 20, 30};
        assertDoesNotThrow(() -> processor.writeToBuffer(data), 
            "Writing valid data should not throw exception");
    }
    
    @Test
    public void testSetValue() {
        // Test setting value at valid index
        assertDoesNotThrow(() -> processor.setValue(0, (byte) 100), 
            "Setting value at valid index should not throw exception");
    }
    
    @Test
    public void testGetBuffer() {
        // Test getting buffer
        byte[] buffer = processor.getBuffer();
        assertNotNull(buffer, "Buffer should not be null");
        assertTrue(buffer.length > 0, "Buffer should have length > 0");
    }
}


