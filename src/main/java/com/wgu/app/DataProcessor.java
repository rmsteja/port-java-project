package com.wgu.app;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * DataProcessor with explicit bounds checking to prevent buffer overflows.
 * All copy/put operations clamp sizes to available buffer capacity.
 */
public class DataProcessor {

    /**
     * Safely extracts a slice from the input without exceeding bounds.
     * If requested length exceeds available data, it is clamped.
     */
    public byte[] process(byte[] input, int offset, int length) {
        if (input == null) {
            return new byte[0];
        }
        // Normalize offset
        if (offset < 0) {
            offset = 0;
        } else if (offset > input.length) {
            offset = input.length;
        }
        // Clamp length to remaining bytes
        int safeLen = Math.max(0, Math.min(length, input.length - offset));
        byte[] out = new byte[safeLen];
        if (safeLen > 0) {
            System.arraycopy(input, offset, out, 0, safeLen);
        }
        return out;
    }

    /**
     * Safely writes into a ByteBuffer, limiting the number of bytes to its remaining capacity.
     */
    public void copyIntoBuffer(byte[] src, ByteBuffer dest) {
        if (src == null || dest == null) {
            return;
        }
        int toWrite = Math.min(src.length, dest.remaining());
        if (toWrite > 0) {
            dest.put(src, 0, toWrite);
        }
    }

    /**
     * Concatenate strings with a maximum output length to avoid oversized allocations.
     */
    public String concat(String a, String b, int maxLen) {
        if (a == null) a = "";
        if (b == null) b = "";
        int targetMax = Math.max(0, maxLen);
        String combined = a + b;
        if (combined.length() > targetMax) {
            combined = combined.substring(0, targetMax);
        }
        return combined;
    }
}

