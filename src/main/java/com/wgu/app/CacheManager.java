package com.wgu.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * CacheManager with hardened serialization/deserialization to prevent
 * unsafe deserialization of untrusted data (RCE/DoS/info disclosure).
 *
 * Changes:
 * - Applies a restrictive ObjectInputFilter (JEP 290) when deserializing.
 * - Enforces limits on bytes, depth, and array sizes to mitigate DoS.
 * - Uses try-with-resources and validates input files.
 */
public class CacheManager {
    private final Path cacheFile;

    // In-memory cache representation
    private Map<String, Serializable> cache = new HashMap<>();

    public CacheManager(String cacheFilePath) {
        this.cacheFile = Paths.get(cacheFilePath);
        load();
    }

    public synchronized void put(String key, Serializable value) {
        cache.put(key, value);
        save();
    }

    public synchronized Serializable get(String key) {
        return cache.get(key);
    }

    public synchronized void remove(String key) {
        cache.remove(key);
        save();
    }

    private void save() {
        try {
            Files.createDirectories(cacheFile.getParent());
        } catch (IOException ignored) {
            // Parent may already exist; ignore
        }

        try (FileOutputStream fos = new FileOutputStream(cacheFile.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(cache);
            oos.flush();
        } catch (IOException e) {
            // Log appropriately in real application
            throw new RuntimeException("Failed to save cache", e);
        }
    }

    /**
     * Load cache from disk with secure deserialization.
     * Uses JEP 290 serialization filtering to restrict allowed classes
     * and resource limits to prevent exploitation.
     */
    @SuppressWarnings("unchecked")
    private void load() {
        File file = cacheFile.toFile();
        if (!file.exists() || file.length() == 0L) {
            cache = new HashMap<>();
            return;
        }

        // Hard limits to mitigate DoS via oversized payloads
        final long maxBytes = Math.min(file.length(), 1_048_576L); // 1 MiB cap

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = readBounded(fis, maxBytes);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
                // Restrictive filter: only allow java.base and our package, deny everything else
                // Also enforce depth/array limits
                ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                        "maxdepth=10;maxarray=100000;maxbytes=1048576;com.wgu.app/*;java.base/*;!*"
                );
                ois.setObjectInputFilter(filter);

                Object obj = ois.readObject();
                if (obj instanceof Map) {
                    this.cache = (Map<String, Serializable>) obj;
                } else {
                    // Unexpected type; reinitialize cache to avoid using untrusted types
                    this.cache = new HashMap<>();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // On failure, do not propagate untrusted state; reset cache
            this.cache = new HashMap<>();
        }
    }

    /**
     * Reads up to maxBytes from the input stream to avoid DoS via huge files.
     */
    private static byte[] readBounded(FileInputStream fis, long maxBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int read;
        while ((read = fis.read(buf)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("Cache file exceeds maximum allowed size");
            }
            baos.write(buf, 0, read);
        }
        return baos.toByteArray();
    }
}

