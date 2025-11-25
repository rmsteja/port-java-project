package com.wgu.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;

/**
 * Test cases for CacheManager.
 */
public class CacheManagerTest {
    
    private CacheManager cacheManager;
    private static final String TEST_CACHE_FILE = "test_cache.ser";
    
    @BeforeEach
    public void setUp() {
        cacheManager = new CacheManager();
        // Clean up any existing test files
        new File(TEST_CACHE_FILE).delete();
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up test files
        new File(TEST_CACHE_FILE).delete();
    }
    
    @Test
    public void testSaveAndLoadCache() throws Exception {
        // Test saving and loading cache entry
        CacheManager.CacheEntry entry = new CacheManager.CacheEntry("test data");
        
        cacheManager.saveToCache(entry, TEST_CACHE_FILE);
        assertTrue(new File(TEST_CACHE_FILE).exists(), "Cache file should be created");
        
        Object loaded = cacheManager.loadFromCache(TEST_CACHE_FILE);
        assertNotNull(loaded, "Loaded cache entry should not be null");
        assertTrue(loaded instanceof CacheManager.CacheEntry, 
            "Loaded object should be CacheEntry");
        
        CacheManager.CacheEntry loadedEntry = (CacheManager.CacheEntry) loaded;
        assertEquals("test data", loadedEntry.getData(), 
            "Cache entry data should match");
    }
    
    @Test
    public void testLoadFromBytes() throws Exception {
        // Test loading from byte array
        CacheManager.CacheEntry entry = new CacheManager.CacheEntry("byte test");
        cacheManager.saveToCache(entry, TEST_CACHE_FILE);
        
        java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(TEST_CACHE_FILE));
        byte[] bytes = java.nio.file.Files.readAllBytes(
            java.nio.file.Paths.get(TEST_CACHE_FILE));
        
        Object loaded = cacheManager.loadFromBytes(bytes);
        assertNotNull(loaded, "Loaded from bytes should not be null");
    }
    
    @Test
    public void testCacheEntryCreation() {
        // Test creating cache entry
        CacheManager.CacheEntry entry = new CacheManager.CacheEntry("test");
        assertNotNull(entry, "Cache entry should be created");
        assertEquals("test", entry.getData(), "Cache entry data should match");
    }
    
    @Test
    public void testLoadNonExistentCache() {
        // Test loading non-existent cache file
        assertThrows(Exception.class, () -> {
            cacheManager.loadFromCache("nonexistent.ser");
        }, "Loading non-existent cache should throw exception");
    }
}

