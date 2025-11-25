package com.wgu.app;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * DataProcessor with safe bounds-checked copying to prevent buffer overflow.
 */
public class DataProcessor {
    // Define a sane upper bound to prevent excessive allocation
    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 1 MB cap

    /**
     * Safely processes input data by copying it with bounds checks.
     * If data exceeds MAX_BUFFER_SIZE, it will be truncated to the maximum allowed size.
     *
     * @param input The input byte array (may be null)
     * @return a safely copied byte array containing the processed data
     */
    public byte[] process(byte[] input) {
        if (input == null) {
            return new byte[0];
        }
        // Enforce upper bound to avoid overflow/over-allocation
        int safeLen = Math.min(input.length, MAX_BUFFER_SIZE);
        byte[] out = new byte[safeLen];
        System.arraycopy(input, 0, out, 0, safeLen);
        return out;
    }

    /**
     * Safely appends a string to existing data with bounds checks.
     *
     * @param existing existing data (may be null)
     * @param toAppend string to append (may be null)
     * @return combined byte[] with truncation if exceeding MAX_BUFFER_SIZE
     */
    public byte[] append(byte[] existing, String toAppend) {
        byte[] left = existing == null ? new byte[0] : existing;
        byte[] right = Objects.toString(toAppend, "").getBytes(StandardCharsets.UTF_8);
        int totalLen = Math.min(left.length + right.length, MAX_BUFFER_SIZE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(totalLen);
        int leftLen = Math.min(left.length, totalLen);
        baos.write(left, 0, leftLen);

        int remaining = totalLen - leftLen;
        if (remaining > 0) {
            int rightLen = Math.min(right.length, remaining);
            baos.write(right, 0, rightLen);
        }
        return baos.toByteArray();
    }
}

