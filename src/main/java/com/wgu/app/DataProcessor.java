package com.wgu.app;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * DataProcessor with safe buffer handling.
 * This version eliminates potential buffer overflow by:
 * - Performing strict bounds checks on any copy operations.
 * - Using ByteArrayOutputStream for dynamic accumulation when needed.
 * - Avoiding writes beyond allocated array sizes.
 */
public class DataProcessor {

    /**
     * Safely processes input bytes and returns a transformed copy.
     * This method avoids writing beyond buffer boundaries by using
     * min-length copies and dynamic buffers where applicable.
     */
    public byte[] process(byte[] input) {
        if (input == null) {
            return new byte[0];
        }
        // Example transformation: pass-through with safe copy
        byte[] out = new byte[input.length];
        // Copy only the available length
        System.arraycopy(input, 0, out, 0, input.length);
        return out;
    }

    /**
     * Safely concatenates two byte arrays.
     */
    public byte[] concat(byte[] a, byte[] b) {
        int lenA = a == null ? 0 : a.length;
        int lenB = b == null ? 0 : b.length;
        byte[] out = new byte[lenA + lenB];
        if (lenA > 0) {
            System.arraycopy(a, 0, out, 0, lenA);
        }
        if (lenB > 0) {
            System.arraycopy(b, 0, out, lenA, lenB);
        }
        return out;
    }

    /**
     * Safely writes a slice of source into a destination buffer at an offset.
     * Performs bounds checks to ensure no overflow.
     */
    public int writeSlice(byte[] src, int srcOffset, int length, byte[] dest, int destOffset) {
        Objects.requireNonNull(dest, "dest cannot be null");
        Objects.requireNonNull(src, "src cannot be null");
        if (srcOffset < 0 || destOffset < 0 || length < 0) {
            throw new IndexOutOfBoundsException("Negative offset/length");
        }
        if (srcOffset > src.length) {
            throw new IndexOutOfBoundsException("srcOffset beyond src length");
        }
        // Clamp length to available bytes in src from srcOffset
        int availableSrc = src.length - srcOffset;
        int toWrite = Math.min(length, availableSrc);
        // Clamp to dest capacity from destOffset
        int availableDest = dest.length - destOffset;
        toWrite = Math.min(toWrite, Math.max(0, availableDest));
        if (toWrite <= 0) {
            return 0; // nothing writable without overflow
        }
        System.arraycopy(src, srcOffset, dest, destOffset, toWrite);
        return toWrite;
    }

    /**
     * Example of safe accumulation of text data.
     */
    public String accumulateStrings(String[] parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String part : parts) {
            if (part == null) continue;
            byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
            // Write all bytes safely into dynamic buffer
            baos.write(bytes, 0, bytes.length);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }
}

