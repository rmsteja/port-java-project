package com.wgu.app;

public class DataProcessor {
    // Default buffer size used by this processor
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * Safely writes the input data into a fixed-size buffer with proper bounds checking
     * to prevent buffer overflow. If the input is larger than the buffer, only the first
     * buffer-length bytes are copied.
     *
     * @param data input byte array, may be larger than the internal buffer
     * @return a new byte array containing the copied bytes without overflow
     */
    public byte[] writeDataSafely(byte[] data) {
        if (data == null) {
            return new byte[0];
        }
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int len = Math.min(data.length, buffer.length);
        // Copy only up to the buffer capacity to avoid IndexOutOfBounds
        System.arraycopy(data, 0, buffer, 0, len);
        // Return only the valid portion to avoid exposing unused buffer space
        byte[] result = new byte[len];
        System.arraycopy(buffer, 0, result, 0, len);
        return result;
    }

    /**
     * Processes data by delegating to safe writer to ensure no buffer overflow occurs.
     */
    public byte[] process(byte[] data) {
        return writeDataSafely(data);
    }
}

