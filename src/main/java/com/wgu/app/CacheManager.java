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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;

/**
 * CacheManager with safe serialization/deserialization.
 *
 * Security hardening:
 * - Adds a strict ObjectInputFilter (JEP 290) to block deserializing arbitrary types.
 * - Enforces maximum payload size to mitigate DoS via oversized inputs.
 * - Uses try-with-resources and validates paths to avoid path traversal.
 */
public class CacheManager {

    private final Path cacheDir;
    private final long maxBytes;

    /**
     * Create a CacheManager rooted at the given directory.
     * @param cacheDirectory directory for cache files
     * @param maxBytesPerEntry maximum allowed size per cached entry in bytes (e.g., 5 MB)
     */
    public CacheManager(Path cacheDirectory, long maxBytesPerEntry) {
        this.cacheDir = Objects.requireNonNull(cacheDirectory, "cacheDirectory");
        this.maxBytes = Math.max(0, maxBytesPerEntry);
    }

    /**
     * Store a value for the given key using Java serialization.
     * Only basic types should be cached (String, byte[], primitives, etc.).
     */
    public void put(String key, Object value) throws IOException {
        Objects.requireNonNull(key, "key");
        Path file = safePathForKey(key);
        Files.createDirectories(cacheDir);
        byte[] data = serialize(value);
        if (data.length > maxBytes) {
            throw new IOException("Serialized payload exceeds max allowed size: " + data.length);
        }
        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            fos.write(data);
        }
    }

    /**
     * Retrieve a cached value by key using safe deserialization.
     * Returns null if the cache file does not exist.
     */
    public Object get(String key) throws IOException {
        Objects.requireNonNull(key, "key");
        Path file = safePathForKey(key);
        if (!Files.exists(file)) {
            return null;
        }
        long size = Files.size(file);
        if (size > maxBytes) {
            throw new IOException("Cached payload exceeds max allowed size: " + size);
        }
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            data = fis.readAllBytes();
        }
        return deserializeSafely(data);
    }

    /**
     * Remove a cached entry.
     */
    public void remove(String key) throws IOException {
        Objects.requireNonNull(key, "key");
        Path file = safePathForKey(key);
        Files.deleteIfExists(file);
    }

    /**
     * Serialize using ObjectOutputStream.
     */
    private byte[] serialize(Object value) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            oos.flush();
            return bos.toByteArray();
        }
    }

    /**
     * Safe deserialization with a strict whitelist using ObjectInputFilter (JEP 290).
     *
     * Allowed:
     * - Classes from java.base (String, arrays, collections implementations from java.base)
     * - Primitive arrays
     *
     * Disallowed:
     * - Any application or third-party classes not explicitly allowed
     *
     * Limits:
     * - maxdepth=10, maxarray=100000, maxrefs=100000
     */
    private Object deserializeSafely(byte[] data) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            // Create a strict filter: allow only java.base module classes and block everything else.
            // Pattern grammar (JEP 290):
            //   "maxdepth=..;maxarray=..;maxrefs=..;class1;class2;module/*;!*"
            ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                "maxdepth=10;maxarray=100000;maxrefs=100000;java.base/*;!*"
            );
            ois.setObjectInputFilter(filter);

            return ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Disallowed class during deserialization", e);
        }
    }

    /**
     * Prevent path traversal by restricting keys to [A-Za-z0-9._-] and mapping to a file under cacheDir.
     */
    private Path safePathForKey(String key) throws IOException {
        String safe = key.replaceAll("[^A-Za-z0-9._-]", "_");
        Path file = cacheDir.resolve(safe + ".cache").normalize();
        Path root = cacheDir.toAbsolutePath().normalize();
        if (!file.toAbsolutePath().startsWith(root)) {
            throw new IOException("Invalid cache key path");
        }
        return file;
    }

    /** Optional utility to write raw bytes safely (subject to size limit). */
    public void putBytes(String key, byte[] bytes) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length > maxBytes) {
            throw new IOException("Payload exceeds max size");
        }
        Path file = safePathForKey(key);
        Files.createDirectories(cacheDir);
        Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Optional utility to read raw bytes safely. */
    public byte[] getBytes(String key) throws IOException {
        Path file = safePathForKey(key);
        if (!Files.exists(file)) return null;
        long size = Files.size(file);
        if (size > maxBytes) {
            throw new IOException("Cached payload exceeds max size: " + size);
        }
        return Files.readAllBytes(file);
    }

    // Basic metadata helper
    public Instant lastModified(String key) throws IOException {
        Path file = safePathForKey(key);
        if (!Files.exists(file)) return null;
        return Files.getLastModifiedTime(file).toInstant();
    }
}

