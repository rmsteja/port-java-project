package com.wgu.app;

import java.util.Objects;

/**
 * Safe data processing utilities.
 *
 * Fixes a potential buffer overflow by adding strict bounds checking
 * and using System.arraycopy for controlled copying.
 */
public class DataProcessor {

    /**
     * Copies input bytes into an internal buffer safely, without writing beyond
     * allocated boundaries.
     *
     * If the original implementation expected a fixed-size buffer, we cap the copy
     * length to the buffer size to avoid overflow while preserving behavior.
     */
    public byte[] process(byte[] input) {
        if (input == null || input.length == 0) {
            return new byte[0];
        }

        // Define a maximum buffer size if the application relies on a fixed upper bound.
        // Adjust this constant to match the original design constraints if needed.
        final int MAX_BUFFER_SIZE = 1024;

        // We only copy up to the maximum allowed buffer size to avoid overflow.
        int copyLen = Math.min(input.length, MAX_BUFFER_SIZE);
        byte[] buffer = new byte[copyLen];
        System.arraycopy(input, 0, buffer, 0, copyLen);
        return buffer;
    }

    /**
     * Safely copies input into the provided output buffer without overflowing it.
     * Extra input bytes are ignored if output is smaller.
     */
    public void processInto(byte[] input, byte[] output) {
        if (output == null) {
            throw new IllegalArgumentException("output buffer must not be null");
        }
        if (input == null || input.length == 0) {
            return; // nothing to copy
        }
        int copyLen = Math.min(input.length, output.length);
        System.arraycopy(input, 0, output, 0, copyLen);
    }

    /**
     * Returns a bounded slice of the input, ensuring no overflow when used as a buffer.
     */
    public byte[] boundedSlice(byte[] input, int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize must be non-negative");
        }
        if (input == null) {
            return new byte[0];
        }
        int copyLen = Math.min(input.length, maxSize);
        byte[] result = new byte[copyLen];
        System.arraycopy(input, 0, result, 0, copyLen);
        return result;
    }
}

