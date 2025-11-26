package com.wgu.app;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.Optional;

/**
 * CacheManager provides simple file-based caching for Serializable objects.
 *
 * Security hardening:
 * - Adds ObjectInputFilter with a strict allow-list to prevent unsafe deserialization (RCE).
 * - Enforces maximum file size and object graph depth to mitigate DoS.
 * - Validates a magic header/version before attempting deserialization.
 */
public class CacheManager {
    private static final String MAGIC = "WGU_CACHE";
    private static final int VERSION = 1;
    // Limit cache entry size to 1 MiB to reduce DoS risk
    private static final long MAX_BYTES = 1 * 1024 * 1024L;
    // Limit object graph depth
    private static final int MAX_DEPTH = 20;

    private final Path cacheDir;

    public CacheManager(String cacheDirectory) throws IOException {
        this.cacheDir = Paths.get(cacheDirectory);
        if (!Files.exists(this.cacheDir)) {
            Files.createDirectories(this.cacheDir);
        }
    }

    public void put(String key, Serializable value) throws IOException {
        Path file = cacheDir.resolve(safeFileName(key));
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toFile()));
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            // Write header
            oos.writeUTF(MAGIC);
            oos.writeInt(VERSION);
            // Write payload
            oos.writeObject(value);
            oos.flush();
        }
    }

    public Optional<Object> get(String key) throws IOException {
        Path file = cacheDir.resolve(safeFileName(key));
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        // Enforce a maximum size before deserializing
        long size = Files.size(file);
        if (size <= 0 || size > MAX_BYTES) {
            // Delete suspicious/oversized cache entry to be safe
            try { Files.deleteIfExists(file); } catch (IOException ignored) {}
            return Optional.empty();
        }

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toFile()));
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            // Validate header
            String magic = ois.readUTF();
            int version = ois.readInt();
            if (!MAGIC.equals(magic) || version != VERSION) {
                // Not our cache format; refuse to deserialize
                try { Files.deleteIfExists(file); } catch (IOException ignored) {}
                return Optional.empty();
            }

            // Apply a strict deserialization filter
            // Allow only classes from our application package and core JDK (java.base)
            // Deny everything else and enforce max depth/bytes
            String filterSpec = String.format(
                "maxdepth=%d;maxbytes=%d;package com.wgu.app.*;java.base/*;!*",
                MAX_DEPTH, MAX_BYTES
            );
            ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(filterSpec);
            ois.setObjectInputFilter(filter);

            Object obj = ois.readObject();
            return Optional.ofNullable(obj);
        } catch (ClassNotFoundException e) {
            // If the class is unknown, treat as cache miss
            return Optional.empty();
        } catch (IOException e) {
            // On IO or filter violations, consider the cache entry invalid
            try { Files.deleteIfExists(file); } catch (IOException ignored) {}
            throw e;
        }
    }

    public void remove(String key) throws IOException {
        Path file = cacheDir.resolve(safeFileName(key));
        Files.deleteIfExists(file);
    }

    private String safeFileName(String key) {
        // Basic sanitation: replace path separators and restrict length
        String sanitized = key.replace('/', '_').replace('\\', '_');
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        return sanitized + ".cache";
    }
}

