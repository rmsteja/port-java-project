package com.wgu.app;

import java.util.Arrays;

/**
 * DataProcessor with strict bounds checking to prevent buffer overflows.
 * All copy operations clamp to destination buffer length and validate indices.
 */
public class DataProcessor {
    // Upper safety cap to avoid unbounded allocations when processing input
    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 1 MiB cap

    /**
     * Safely processes the incoming data by copying at most the destination capacity.
     * No writes occur beyond allocated buffer boundaries.
     *
     * @param input source bytes to process
     * @return a processed copy limited to safe bounds (never null)
     */
    public byte[] process(byte[] input) {
        if (input == null) {
            return new byte[0];
        }
        // Clamp requested size to our safety cap
        final int safeLen = Math.min(input.length, MAX_BUFFER_SIZE);
        final byte[] buffer = new byte[safeLen];
        // Copy only what fits into the destination buffer
        System.arraycopy(input, 0, buffer, 0, safeLen);

        // If additional transformation is needed, ensure index checks are performed
        // Example: simple normalization loop with strict bounds
        for (int i = 0; i < buffer.length; i++) {
            // Example transformation (no out-of-bounds access)
            buffer[i] = (byte)(buffer[i] & 0xFF);
        }

        // Return a defensive copy limited to the actual safe length
        return Arrays.copyOf(buffer, safeLen);
    }

    /**
     * Safe write into a destination buffer with explicit bounds checking.
     * Returns the number of bytes written.
     *
     * @param src source array
     * @param dst destination array
     * @param dstOffset destination start index
     * @return number of bytes copied
     */
    public int safeWrite(byte[] src, byte[] dst, int dstOffset) {
        if (src == null || dst == null) {
            return 0;
        }
        if (dstOffset < 0 || dstOffset > dst.length) {
            return 0;
        }
        int writable = Math.min(src.length, dst.length - dstOffset);
        if (writable <= 0) {
            return 0;
        }
        System.arraycopy(src, 0, dst, dstOffset, writable);
        return writable;
    }
}

