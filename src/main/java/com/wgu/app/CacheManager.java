package com.wgu.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * CacheManager handles serialization and deserialization of cache data.
 *
 * SECURITY HARDENING:
 * - Adds ObjectInputFilter to whitelist allowed classes and limit depth/size.
 * - Validates the type of deserialized object before use.
 * - Uses try-with-resources to ensure streams are closed safely.
 */
public class CacheManager {

    // Restrictive deserialization filter: allow java.base types and our package, with sensible limits.
    private static final ObjectInputFilter DESERIALIZATION_FILTER =
            ObjectInputFilter.Config.createFilter(
                    "maxdepth=8;maxbytes=2097152;maxrefs=500;java.base/*;com.wgu.app/*;!*" // deny everything else
            );

    /**
     * Serialize a Serializable cache object to a byte array.
     */
    public static byte[] serialize(Serializable cacheObject) throws IOException {
        if (cacheObject == null) {
            throw new IllegalArgumentException("cacheObject must not be null");
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            // Write using standard Java serialization
            oos.writeObject(cacheObject);
            oos.flush();
            return bos.toByteArray();
        }
    }

    /**
     * Deserialize cache data from a byte array in a safe manner.
     * Returns the original object if it matches expected safe types.
     */
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            // Apply restrictive filter BEFORE reading objects
            ois.setObjectInputFilter(DESERIALIZATION_FILTER);

            Object obj = ois.readObject();

            // Basic type validation: allow common safe structures and app domain classes only
            if (isAllowedType(obj)) {
                return obj;
            }
            throw new IOException("Deserialized object type is not allowed: " + obj.getClass());
        }
    }

    /**
     * Convenience methods to read/write via streams with the same protections.
     */
    public static void writeTo(OutputStream out, Serializable cacheObject) throws IOException {
        if (out == null) throw new IllegalArgumentException("out must not be null");
        if (cacheObject == null) throw new IllegalArgumentException("cacheObject must not be null");
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(cacheObject);
            oos.flush();
        }
    }

    public static Object readFrom(InputStream in) throws IOException, ClassNotFoundException {
        if (in == null) throw new IllegalArgumentException("in must not be null");
        try (ObjectInputStream ois = new ObjectInputStream(in)) {
            ois.setObjectInputFilter(DESERIALIZATION_FILTER);
            Object obj = ois.readObject();
            if (isAllowedType(obj)) {
                return obj;
            }
            throw new IOException("Deserialized object type is not allowed: " + obj.getClass());
        }
    }

    // Whitelist of allowed object graph roots. Adjust as needed for the application domain.
    private static boolean isAllowedType(Object obj) {
        if (obj == null) return false;
        // Allow common structures from java.base
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) return true;
        if (obj instanceof Map) return true; // Maps are allowed; nested classes still gated by filter
        // Allow classes from our application package only
        Package p = obj.getClass().getPackage();
        return p != null && p.getName().startsWith("com.wgu.app");
    }
}

