package com.wgu.app;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * DataProcessor with defensive bounds checking to prevent buffer overflows.
 * This implementation avoids writing past allocated buffers by:
 * - Validating indices and sizes before any copy.
 * - Using ByteArrayOutputStream for dynamic accumulation where appropriate.
 */
public class DataProcessor {

    /**
     * Safely copies data from src to dest ensuring we never write beyond dest's bounds
     * or read beyond src's bounds. Returns the number of bytes actually copied.
     */
    public static int safeCopy(byte[] src, int srcPos, byte[] dest, int destPos, int requestedLen) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(dest, "dest");
        if (srcPos < 0 || destPos < 0 || requestedLen < 0) {
            throw new IllegalArgumentException("Positions and length must be non-negative");
        }
        if (srcPos > src.length || destPos > dest.length) {
            throw new IndexOutOfBoundsException("Source or destination position out of bounds");
        }
        int maxReadable = src.length - srcPos;
        int maxWritable = dest.length - destPos;
        int len = Math.min(requestedLen, Math.min(maxReadable, maxWritable));
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(src, srcPos, dest, destPos, len);
        return len;
    }

    /**
     * Example processing method that normalizes input to uppercase safely.
     * Demonstrates dynamic buffering to avoid fixed-size buffer risks.
     */
    public byte[] process(byte[] input) {
        Objects.requireNonNull(input, "input");
        // Use dynamic buffer instead of fixed-size arrays to prevent overflow.
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        // In real logic, transformation would be applied chunk-wise safely.
        String s = new String(input, StandardCharsets.UTF_8);
        byte[] transformed = s.toUpperCase().getBytes(StandardCharsets.UTF_8);
        out.write(transformed, 0, transformed.length);
        return out.toByteArray();
    }
}

