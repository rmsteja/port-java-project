package com.wgu.app;

import java.io.*;

/**
 * Manages object caching using Java serialization.
 */
public class CacheManager {
    
    /**
     * Loads a cached object from file.
     */
    public Object loadFromCache(String filename) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            return ois.readObject();
        }
    }
    
    /**
     * Saves an object to cache file.
     */
    public void saveToCache(Object obj, String filename) throws Exception {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            oos.writeObject(obj);
        }
    }
    
    /**
     * Loads an object from byte array.
     */
    public Object loadFromBytes(byte[] data) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }
    
    /**
     * Cache entry class.
     */
    public static class CacheEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private String data;
        private long timestamp;
        
        public CacheEntry(String data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            System.out.println("Cache entry loaded: " + data);
            System.out.println("Timestamp: " + timestamp);
        }
        
        @Override
        public String toString() {
            return "CacheEntry{data='" + data + "', timestamp=" + timestamp + "}";
        }
        
        public String getData() {
            return data;
        }
    }
}

