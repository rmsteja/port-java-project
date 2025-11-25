package com.wgu.app;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Utility class for processing binary data.
 */
public class DataProcessor {
    
    private static final int BUFFER_SIZE = 10;
    private byte[] buffer = new byte[BUFFER_SIZE];
    
    /**
     * Copies input data to internal buffer.
     */
    public void processData(byte[] input) {
        System.arraycopy(input, 0, buffer, 0, input.length);
    }
    
    /**
     * Writes data to a ByteBuffer.
     */
    public void writeToBuffer(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.put(data);
    }
    
    /**
     * Sets a value at the specified index.
     */
    public void setValue(int index, byte value) {
        buffer[index] = value;
    }
    
    public byte[] getBuffer() {
        return Arrays.copyOf(buffer, buffer.length);
    }
}

