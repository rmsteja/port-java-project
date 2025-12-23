package com.wgu.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * DataProcessor with explicit bounds checking to prevent buffer overflows.
 * All copy operations use safe limits and never write beyond allocated buffers.
 */
public class DataProcessor {
    // Define an upper bound for buffer operations to avoid excessive allocations
    public static final int MAX_BUFFER_SIZE = 8192; // 8KB sane default

    /**
     * Safely processes the input bytes and returns a copy truncated to MAX_BUFFER_SIZE if needed.
     * This method never writes past the allocated array size.
     */
    public byte[] process(byte[] input) {
        if (input == null) {
            return new byte[0];
        }
        // Enforce bounds to avoid writing past the target buffer
        int safeLen = Math.min(input.length, MAX_BUFFER_SIZE);
        byte[] out = new byte[safeLen];
        System.arraycopy(input, 0, out, 0, safeLen);
        return out;
    }

    /**
     * Safely copies data from an InputStream to an OutputStream using a fixed-size buffer
     * with proper bounds and offset handling. No writes beyond buffer boundaries occur.
     *
     * @return total bytes transferred
     */
    public int pipe(InputStream in, OutputStream out) throws IOException {
        Objects.requireNonNull(in, "InputStream must not be null");
        Objects.requireNonNull(out, "OutputStream must not be null");
        byte[] buf = new byte[4096];
        int total = 0;
        int read;
        while ((read = in.read(buf)) != -1) {
            // Always respect the number of bytes actually read
            out.write(buf, 0, read);
            total += read;
        }
        return total;
    }

    /**
     * Safely converts a String to bytes and returns up to MAX_BUFFER_SIZE bytes.
     */
    public byte[] toBytes(String s) {
        if (s == null) {
            return new byte[0];
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int safeLen = Math.min(bytes.length, MAX_BUFFER_SIZE);
        byte[] out = new byte[safeLen];
        System.arraycopy(bytes, 0, out, 0, safeLen);
        return out;
    }
}

