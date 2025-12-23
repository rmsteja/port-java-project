package com.wgu.app;

import java.util.Arrays;

/**
 * DataProcessor provides safe data copy and processing utilities.
 *
 * Fix: Add strict bounds checking to prevent writing beyond buffer limits
 * and use safe copy operations that cap writes to the destination size.
 */
public class DataProcessor {

    /**
     * Safely copies data from src into dest without exceeding dest length.
     * If length would overflow dest, it is truncated to fit.
     *
     * @param src    source byte array (must not be null)
     * @param srcPos starting offset in src (>= 0)
     * @param dest   destination byte array (must not be null)
     * @param destPos starting offset in dest (>= 0)
     * @param length number of bytes requested to copy (>= 0)
     * @return number of bytes actually copied
     * @throws IllegalArgumentException if inputs are invalid
     */
    public static int safeCopy(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
        if (src == null || dest == null) {
            throw new IllegalArgumentException("src and dest must not be null");
        }
        if (srcPos < 0 || destPos < 0 || length < 0) {
            throw new IllegalArgumentException("srcPos, destPos, and length must be non-negative");
        }
        // Compute maximum allowable bytes from src and into dest
        int maxFromSrc = Math.max(0, src.length - srcPos);
        int maxIntoDest = Math.max(0, dest.length - destPos);
        int toCopy = Math.min(length, Math.min(maxFromSrc, maxIntoDest));

        if (toCopy == 0) {
            return 0; // nothing to copy or out of bounds
        }

        System.arraycopy(src, srcPos, dest, destPos, toCopy);
        return toCopy;
    }

    /**
     * Processes input data into a fixed-size buffer safely.
     * This method ensures no buffer overflow by capping writes
     * to the buffer's capacity.
     *
     * @param input the input bytes (may be larger than buffer)
     * @param bufferSize desired buffer size
     * @return a new byte[] containing up to bufferSize bytes from input
     */
    public static byte[] process(byte[] input, int bufferSize) {
        if (bufferSize < 0) {
            throw new IllegalArgumentException("bufferSize must be non-negative");
        }
        byte[] buffer = new byte[bufferSize];
        if (input == null || input.length == 0 || bufferSize == 0) {
            return buffer; // empty buffer
        }
        int toCopy = Math.min(input.length, buffer.length);
        System.arraycopy(input, 0, buffer, 0, toCopy);
        return buffer;
    }

    /**
     * Concatenates two arrays safely, guarding against overflow.
     *
     * @param a first array
     * @param b second array
     * @param maxSize maximum allowed size of the result
     * @return concatenated array capped at maxSize
     */
    public static byte[] concatCapped(byte[] a, byte[] b, int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize must be non-negative");
        }
        int lenA = (a == null) ? 0 : a.length;
        int lenB = (b == null) ? 0 : b.length;
        int wanted = lenA + lenB;
        int resultLen = Math.min(maxSize, wanted);

        byte[] result = new byte[resultLen];
        int written = 0;
        if (lenA > 0 && written < resultLen) {
            int toCopyA = Math.min(lenA, resultLen - written);
            System.arraycopy(a, 0, result, written, toCopyA);
            written += toCopyA;
        }
        if (lenB > 0 && written < resultLen) {
            int toCopyB = Math.min(lenB, resultLen - written);
            System.arraycopy(b, 0, result, written, toCopyB);
            written += toCopyB;
        }
        return result;
    }

    /**
     * Creates a defensive copy up to a maximum length.
     *
     * @param data source array
     * @param maxLen maximum length to copy
     * @return copy of up to maxLen bytes
     */
    public static byte[] copyOfMax(byte[] data, int maxLen) {
        if (data == null) {
            return new byte[Math.max(0, maxLen)];
        }
        int n = Math.max(0, Math.min(maxLen, data.length));
        return Arrays.copyOf(data, n);
    }
}

