package com.wgu.app;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * DataProcessor with strict bounds checking to prevent buffer overflows.
 *
 * This implementation ensures no writes exceed the underlying buffer capacity
 * and that reads/writes validate offsets and lengths against source arrays.
 */
public class DataProcessor {
    private final ByteBuffer buffer;

    /**
     * Create a processor with a fixed-capacity buffer.
     *
     * @param capacity size of internal buffer; must be > 0
     */
    public DataProcessor(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.buffer = ByteBuffer.allocate(capacity);
    }

    /**
     * Safely writes up to {@code length} bytes from {@code data} starting at {@code offset}
     * into the internal buffer without exceeding its remaining capacity.
     *
     * - Validates nulls and bounds
     * - Caps the write to the minimum of requested length, available data, and buffer remaining
     *
     * @return number of bytes actually written (may be less than requested if capacity limits)
     */
    public synchronized int write(byte[] data, int offset, int length) {
        Objects.requireNonNull(data, "data");
        if (offset < 0 || length < 0 || offset > data.length) {
            throw new IndexOutOfBoundsException("Invalid offset/length");
        }
        int availableInData = data.length - offset;
        int maxLen = Math.min(length, availableInData);
        int writable = Math.min(maxLen, buffer.remaining());
        if (writable <= 0) {
            return 0;
        }
        buffer.put(data, offset, writable);
        return writable;
    }

    /**
     * Returns a copy of all bytes written so far.
     */
    public synchronized byte[] readAll() {
        ByteBuffer dup = buffer.asReadOnlyBuffer();
        dup.flip();
        byte[] out = new byte[dup.remaining()];
        dup.get(out);
        return out;
    }

    /**
     * Clears the buffer for reuse.
     */
    public synchronized void reset() {
        buffer.clear();
    }
}

