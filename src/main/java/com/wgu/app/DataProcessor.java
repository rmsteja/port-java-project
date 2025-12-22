package com.wgu.app;

import java.util.Arrays;

/**
 * DataProcessor with safe buffer handling.
 * Ensures no writes occur beyond allocated buffer boundaries.
 */
public class DataProcessor {
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * Processes input bytes safely, avoiding buffer overflows.
     * - Ensures destination buffer is at least input length
     * - Uses System.arraycopy with bounded length
     */
    public byte[] process(byte[] input) {
        if (input == null || input.length == 0) {
            return new byte[0];
        }
        int size = Math.max(DEFAULT_BUFFER_SIZE, input.length);
        byte[] buffer = new byte[size];
        // Copy only within buffer bounds
        System.arraycopy(input, 0, buffer, 0, input.length);
        // Return exactly the data copied (no exposing extra capacity)
        return Arrays.copyOf(buffer, input.length);
    }

    /**
     * Safely writes characters from src into dest, truncating if needed.
     */
    public void writeInto(char[] src, char[] dest) {
        if (src == null || dest == null) {
            return;
        }
        int toCopy = Math.min(src.length, dest.length);
        System.arraycopy(src, 0, dest, 0, toCopy);
    }

    /**
     * Fills the destination byte array starting at offset with src bytes safely.
     * Returns the number of bytes copied.
     */
    public int fill(byte[] src, int offset, byte[] dest) {
        if (src == null || dest == null) {
            return 0;
        }
        if (offset < 0) {
            offset = 0;
        }
        if (offset >= dest.length) {
            return 0;
        }
        int maxCopy = Math.min(src.length, dest.length - offset);
        System.arraycopy(src, 0, dest, offset, maxCopy);
        return maxCopy;
    }
}

