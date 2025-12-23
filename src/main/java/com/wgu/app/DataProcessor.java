package com.wgu.app;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * DataProcessor with robust bounds checking to prevent buffer overflows.
 * All copy and write operations clamp lengths to the destination buffer size
 * and validate offsets before performing writes.
 */
public class DataProcessor {

    /**
     * Safely copies the requested slice of input into a new array, clamping the
     * length to avoid writing past the end of the destination buffer.
     *
     * @param input  source data (may be null)
     * @param offset starting offset in input (must be >= 0)
     * @param length requested number of bytes to copy (must be >= 0)
     * @return a new array containing the copied bytes (possibly 0-length)
     */
    public byte[] safeSlice(byte[] input, int offset, int length) {
        if (input == null || input.length == 0 || length <= 0) {
            return new byte[0];
        }
        // Normalize negative values
        int safeOffset = Math.max(0, offset);
        int safeLength = Math.max(0, length);

        // If offset beyond input, nothing to copy
        if (safeOffset >= input.length) {
            return new byte[0];
        }

        // Clamp length so (offset + length) does not exceed input.length
        int maxLen = input.length - safeOffset;
        int copyLen = Math.min(safeLength, maxLen);

        byte[] out = new byte[copyLen];
        System.arraycopy(input, safeOffset, out, 0, copyLen);
        return out;
    }

    /**
     * Safely writes data into the provided destination buffer starting at destOffset.
     * The number of bytes written is the largest safe value that fits in dest.
     *
     * @param dest       destination buffer (must not be null)
     * @param destOffset starting index to write into dest
     * @param src        source data (may be null)
     * @return number of bytes actually written
     */
    public int safeWrite(byte[] dest, int destOffset, byte[] src) {
        if (dest == null || dest.length == 0 || src == null || src.length == 0) {
            return 0;
        }
        int offset = Math.max(0, destOffset);
        if (offset >= dest.length) {
            return 0; // nothing fits
        }
        int room = dest.length - offset;
        int toWrite = Math.min(room, src.length);
        System.arraycopy(src, 0, dest, offset, toWrite);
        return toWrite;
    }

    /**
     * Example processing method that converts input text to upper-case safely,
     * ensuring the output buffer size is bounded and copies are clamped.
     *
     * @param inputText input string (may be null)
     * @param maxOutputBytes maximum allowed bytes for output buffer
     * @return UTF-8 upper-case bytes of input clamped to maxOutputBytes
     */
    public byte[] processTextUpperBounded(String inputText, int maxOutputBytes) {
        int limit = Math.max(0, maxOutputBytes);
        if (inputText == null || limit == 0) {
            return new byte[0];
        }
        byte[] src = inputText.toUpperCase().getBytes(StandardCharsets.UTF_8);
        int copyLen = Math.min(src.length, limit);
        return Arrays.copyOf(src, copyLen);
    }
}

