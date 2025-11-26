package com.wgu.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * CacheManager with safe serialization/deserialization.
 *
 * Vulnerability fixed: unsafe Java deserialization of untrusted data.
 *
 * Mitigations:
 * - Enforce an ObjectInputFilter whitelist and limits (max bytes, depth).
 * - Enforce a maximum payload size on read/write.
 * - Validate the expected type on read.
 */
public class CacheManager {
    // Reasonable max cache entry size (5MB)
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    // Limit object graph depth to reduce DoS risk
    private static final int MAX_DEPTH = 5;

    private final Path cacheDir;

    public CacheManager(Path cacheDir) {
        this.cacheDir = Objects.requireNonNull(cacheDir, "cacheDir");
    }

    public static CacheManager inUserCacheDir(String appName) throws IOException {
        Path base = Paths.get(System.getProperty("user.home"), "." + appName, "cache");
        Files.createDirectories(base);
        return new CacheManager(base);
    }

    private Path pathForKey(String key) {
        // Very simple filename derivation; in real usage, sanitize key or hash it
        return cacheDir.resolve(key + ".bin");
    }

    /**
     * Store a Serializable object safely.
     */
    public void put(String key, Object value) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        // Serialize to an in-memory buffer first to enforce size limits
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
        }
        byte[] bytes = bos.toByteArray();
        if (bytes.length > MAX_BYTES) {
            throw new IOException("Serialized object exceeds max size: " + bytes.length);
        }

        Path target = pathForKey(key);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * Load a cached object safely, ensuring it is of the expected type.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> expectedType) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(expectedType, "expectedType");

        Path source = pathForKey(key);
        if (!Files.exists(source)) {
            return null;
        }
        long size = Files.size(source);
        if (size > MAX_BYTES) {
            throw new IOException("Cached entry too large: " + size);
        }

        byte[] bytes = Files.readAllBytes(source);
        // Set up a restrictive filter: allow only our package, common JDK harmless types, set limits
        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                "maxdepth=" + MAX_DEPTH + ";maxbytes=" + MAX_BYTES + ";" +
                "com.wgu.app.*;java.lang.String;java.lang.Integer;java.lang.Long;java.lang.Double;java.lang.Float;java.lang.Boolean;java.util.*;!*"
        );

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            ois.setObjectInputFilter(filter);
            Object obj = ois.readObject();
            if (obj == null) {
                return null;
            }
            if (!expectedType.isInstance(obj)) {
                throw new IOException("Cached object type mismatch: " + obj.getClass().getName());
            }
            return (T) obj;
        } catch (InvalidClassException | StreamCorruptedException e) {
            // Corrupted or dangerous stream – delete the cache entry and surface error
            try { Files.deleteIfExists(source); } catch (IOException ignore) {}
            throw new IOException("Invalid or corrupted cached data", e);
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found during deserialization", e);
        }
    }

    /**
     * Remove a cached entry.
     */
    public void remove(String key) throws IOException {
        Objects.requireNonNull(key, "key");
        Files.deleteIfExists(pathForKey(key));
    }
}

