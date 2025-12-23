package com.wgu.app;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * DataProcessor with safe buffer handling to prevent buffer overflow.
 * Ensures all writes respect allocated buffer boundaries.
 */
public class DataProcessor {

    /**
     * Safely copies input bytes into a buffer of given size without overruns.
     * Returns the portion actually copied.
     */
    public static byte[] processData(byte[] input, int bufferSize) {
        if (bufferSize <= 0) {
            return new byte[0];
        }
        byte[] buffer = new byte[bufferSize];
        int inputLen = (input == null) ? 0 : input.length;
        int copyLen = Math.min(inputLen, buffer.length);
        if (copyLen > 0) {
            System.arraycopy(input, 0, buffer, 0, copyLen);
        }
        // Return only the meaningful portion
        return Arrays.copyOf(buffer, copyLen);
    }

    /**
     * Safely processes a String into a bounded buffer using UTF-8 encoding.
     */
    public static String processString(String input, int bufferSize) {
        byte[] bytes = (input == null) ? new byte[0] : input.getBytes(StandardCharsets.UTF_8);
        byte[] out = processData(bytes, bufferSize);
        return new String(out, StandardCharsets.UTF_8);
    }
}

