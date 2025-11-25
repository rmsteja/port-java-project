package com.wgu.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Test cases for FileManager.
 */
public class FileManagerTest {
    
    private FileManager fileManager;
    private static final String TEST_FILE = "test_file.txt";
    
    @BeforeEach
    public void setUp() throws IOException {
        fileManager = new FileManager();
        // Create a test file
        try (FileWriter writer = new FileWriter(TEST_FILE)) {
            writer.write("Test file content");
        }
    }
    
    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        // Clean up test file
        new File(TEST_FILE).delete();
    }
    
    @Test
    public void testReadFileWithValidFile() {
        // Test reading a valid file
        String content = fileManager.readFile(TEST_FILE);
        assertNotNull(content, "File content should not be null");
        assertTrue(content.contains("Test file content"), 
            "File content should contain expected text");
    }
    
    @Test
    public void testReadFileWithNonExistentFile() {
        // Test reading non-existent file
        String content = fileManager.readFile("nonexistent_file_12345.txt");
        assertNotNull(content, "Result should not be null");
        assertTrue(content.contains("Error"), 
            "Reading non-existent file should return error message");
    }
    
    @Test
    public void testReadFileWithNull() {
        // Test with null filename
        String content = fileManager.readFile(null);
        assertNotNull(content, "Result should not be null");
    }
}

