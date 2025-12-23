package com.wgu.app;

/**
 * DataProcessor - fixes potential buffer overflow by enforcing strict bounds checking
 * on all buffer write operations. The implementation uses safe copy utilities that
 * never write past the destination buffer capacity.
 */
public class DataProcessor {

    /**
     * Safely processes input bytes into a fixed-size internal buffer.
     * If the input is larger than the buffer, only the first buffer.length bytes are copied.
     * This prevents any write past buffer boundaries.
     *
     * Note: The returned array is the same fixed size as the internal buffer to
     * preserve existing behavior while avoiding overflow.
     */
    public byte[] process(byte[] input) {
        // Fixed-size destination buffer (preserving original intent if present)
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];

        if (input == null || input.length == 0) {
            return buffer; // nothing to copy; return zeroed buffer
        }

        // Copy only up to the buffer's capacity
        int copyLen = Math.min(input.length, buffer.length);
        System.arraycopy(input, 0, buffer, 0, copyLen);
        return buffer;
    }

    /**
     * Safely copies from source to destination ensuring no out-of-bounds writes.
     * Returns the number of bytes copied.
     */
    public static int safeCopy(byte[] src, byte[] dst) {
        if (src == null || dst == null) {
            return 0;
        }
        int len = Math.min(src.length, dst.length);
        System.arraycopy(src, 0, dst, 0, len);
        return len;
    }

    /**
     * Safely writes a subset of the source into the destination starting at dstOffset.
     * Ensures bounds are respected, avoiding buffer overflow and index errors.
     * Returns the number of bytes copied.
     */
    public static int safeCopy(byte[] src, int srcOffset, byte[] dst, int dstOffset, int length) {
        if (src == null || dst == null || length <= 0) {
            return 0;
        }
        // Normalize offsets
        if (srcOffset < 0) srcOffset = 0;
        if (dstOffset < 0) dstOffset = 0;

        // Calculate allowable length within bounds
        int maxSrc = Math.max(0, src.length - srcOffset);
        int maxDst = Math.max(0, dst.length - dstOffset);
        int copyLen = Math.min(length, Math.min(maxSrc, maxDst));
        if (copyLen <= 0) {
            return 0;
        }

        System.arraycopy(src, srcOffset, dst, dstOffset, copyLen);
        return copyLen;
    }

    /**
     * Example method that processes strings safely into a char buffer.
     * Preserves behavior while preventing writes past the buffer.
     */
    public char[] processString(String data) {
        final int BUFFER_SIZE = 1024;
        char[] buffer = new char[BUFFER_SIZE];
        if (data == null || data.isEmpty()) {
            return buffer;
        }
        int copyLen = Math.min(data.length(), buffer.length);
        data.getChars(0, copyLen, buffer, 0);
        return buffer;
    }
}

