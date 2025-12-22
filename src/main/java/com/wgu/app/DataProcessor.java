package com.wgu.app;

import java.nio.charset.StandardCharsets;

/**
 * DataProcessor with safe buffer handling to prevent buffer overflow.
 * Any copy/write operations perform strict bounds checking.
 */
public class DataProcessor {

    /**
     * Safely copy bytes from src to dest, preventing writes beyond buffer bounds.
     * Returns the number of bytes actually copied.
     */
    public static int copyToBuffer(byte[] src, int srcOffset, byte[] dest, int destOffset, int length) {
        if (src == null || dest == null) {
            return 0;
        }
        if (srcOffset < 0 || destOffset < 0 || length < 0) {
            return 0;
        }
        if (srcOffset > src.length) {
            return 0;
        }
        if (destOffset > dest.length) {
            return 0;
        }
        // Number of bytes available from src starting at srcOffset
        int availableSrc = Math.max(0, src.length - srcOffset);
        // Requested length cannot exceed available src bytes
        int requested = Math.min(length, availableSrc);
        // Do not exceed available space in dest starting at destOffset
        int availableDest = Math.max(0, dest.length - destOffset);
        int bytesToCopy = Math.max(0, Math.min(requested, availableDest));
        if (bytesToCopy == 0) {
            return 0;
        }
        System.arraycopy(src, srcOffset, dest, destOffset, bytesToCopy);
        return bytesToCopy;
    }

    /**
     * Example processing that writes input into a bounded buffer safely.
     * Truncates input to maxSize if necessary.
     */
    public String process(String input, int maxSize) {
        if (input == null) {
            return "";
        }
        if (maxSize < 0) {
            maxSize = 0;
        }
        byte[] src = input.getBytes(StandardCharsets.UTF_8);
        byte[] dest = new byte[maxSize];
        int copied = copyToBuffer(src, 0, dest, 0, src.length);
        return new String(dest, 0, copied, StandardCharsets.UTF_8);
    }
}

