package com.wgu.app;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * DataProcessor - hardened against buffer overflows by enforcing strict bounds checks
 * and safe copying. All writes are limited to allocated buffer sizes.
 */
public class DataProcessor {
    // Define an upper bound to prevent oversized allocations or copies
    private static final int MAX_BUFFER_SIZE = 8192; // 8KB cap, adjust as needed

    /**
     * Safely processes raw byte data. Input is truncated to MAX_BUFFER_SIZE to avoid
     * writing beyond buffer boundaries. All copy operations use checked lengths.
     */
    public byte[] process(byte[] data) {
        if (data == null) {
            return new byte[0];
        }
        // Enforce bounds to avoid overflow during copy
        int safeLen = Math.min(data.length, MAX_BUFFER_SIZE);
        byte[] buffer = new byte[safeLen];
        // Safe copy: only copy within the allocated buffer size
        System.arraycopy(data, 0, buffer, 0, safeLen);
        return transform(buffer);
    }

    /**
     * Safely processes a String by converting to UTF-8 bytes and delegating to process(byte[]).
     */
    public String processString(String input) {
        if (input == null) {
            return "";
        }
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] out = process(bytes);
        return new String(out, StandardCharsets.UTF_8);
    }

    /**
     * Example transform that returns a copy. Replace with real logic as needed,
     * but never write past the provided buffer length.
     */
    private byte[] transform(byte[] buf) {
        if (buf == null) {
            return new byte[0];
        }
        // Safe copy preserving exact size
        return Arrays.copyOf(buf, buf.length);
    }
}

