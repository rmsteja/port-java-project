package com.wgu.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputFilter;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CacheManager with safe serialization/deserialization.
 * Fixes: unsafe deserialization of untrusted data by enforcing allowlist via JEP-290 filters
 * and explicit expected type casting, plus payload size limits to mitigate DoS.
 */
public class CacheManager {
    private final ConcurrentMap<String, byte[]> cache = new ConcurrentHashMap<>();
    private static final int MAX_OBJECT_SIZE = 5 * 1024 * 1024; // 5 MB safety cap

    /**
     * Store a Serializable value in cache under the given key.
     */
    public void put(String key, Serializable value) throws IOException {
        if (key == null) throw new IllegalArgumentException("key cannot be null");
        byte[] data = serialize(value);
        cache.put(key, data);
    }

    /**
     * Retrieve and safely deserialize a cached value to the expected type.
     */
    public <T> T get(String key, Class<T> expectedType) throws IOException, ClassNotFoundException {
        if (key == null) throw new IllegalArgumentException("key cannot be null");
        if (expectedType == null) throw new IllegalArgumentException("expectedType cannot be null");
        byte[] data = cache.get(key);
        if (data == null) return null;
        Object obj = deserialize(data, expectedType);
        return expectedType.cast(obj);
    }

    /**
     * Remove an entry from the cache.
     */
    public void remove(String key) {
        if (key == null) return;
        cache.remove(key);
    }

    private static byte[] serialize(Serializable obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            out.flush();
            return bos.toByteArray();
        }
    }

    private static Object deserialize(byte[] data, Class<?> expectedType) throws IOException, ClassNotFoundException {
        if (data == null) return null;
        if (data.length > MAX_OBJECT_SIZE) {
            throw new IOException("Cached payload exceeds safe size limit");
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             SafeObjectInputStream in = new SafeObjectInputStream(bis, expectedType)) {
            Object o = in.readObject();
            if (!expectedType.isInstance(o)) {
                throw new InvalidObjectException("Deserialized type mismatch: " + o.getClass());
            }
            return o;
        }
    }

    // Allowed package prefixes for deserialization
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "java.lang.",
            "java.util.",
            "com.wgu.app."
    );

    /**
     * ObjectInputStream with an allowlist-based filter to prevent gadget deserialization.
     */
    private static final class SafeObjectInputStream extends ObjectInputStream {
        private final Class<?> expected;

        SafeObjectInputStream(InputStream in, Class<?> expected) throws IOException {
            super(in);
            this.expected = expected;
            // Apply JEP-290 deserialization filter when available (Java 9+)
            try {
                ObjectInputFilter filter = info -> {
                    Class<?> cl = info.serialClass();
                    if (cl == null) return ObjectInputFilter.Status.UNDECIDED; // primitive types

                    // Arrays: ensure component type is allowed
                    if (cl.isArray()) {
                        Class<?> component = cl.getComponentType();
                        if (component == null) return ObjectInputFilter.Status.REJECTED;
                        if (component.isPrimitive() || isAllowed(component.getName())) {
                            return ObjectInputFilter.Status.ALLOWED;
                        }
                        return ObjectInputFilter.Status.REJECTED;
                    }

                    // Primitives are safe
                    if (cl.isPrimitive()) return ObjectInputFilter.Status.ALLOWED;

                    // Expected type is always allowed
                    if (expected != null && cl.getName().equals(expected.getName())) {
                        return ObjectInputFilter.Status.ALLOWED;
                    }

                    // Allow selected JDK and app packages only
                    if (isAllowed(cl.getName())) {
                        return ObjectInputFilter.Status.ALLOWED;
                    }

                    return ObjectInputFilter.Status.REJECTED;
                };
                this.setObjectInputFilter(filter);
            } catch (Throwable ignored) {
                // Older JVM without ObjectInputFilter; still guarded by manual checks after readObject
            }
        }

        private boolean isAllowed(String className) {
            for (String p : ALLOWED_PREFIXES) {
                if (className.startsWith(p)) return true;
            }
            return false;
        }
    }
}

