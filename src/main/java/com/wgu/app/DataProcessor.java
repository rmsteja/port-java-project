package com.wgu.app;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * DataProcessor with safe bounded writes to prevent buffer overflow.
 */
public class DataProcessor {
    private final ByteBuffer buffer;

    public DataProcessor(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.buffer = ByteBuffer.allocate(capacity);
    }

    /**
     * Writes up to 'length' bytes from 'src' starting at 'offset' into the internal buffer.
     * This method enforces bounds and will only write what fits to avoid overflow.
     *
     * @return number of bytes actually written (may be less than requested if capacity is limited)
     */
    public synchronized int write(byte[] src, int offset, int length) {
        Objects.requireNonNull(src, "src");
        if (offset < 0 || length < 0 || offset > src.length) {
            throw new IndexOutOfBoundsException("Invalid offset/length");
        }
        // Remaining capacity in destination
        int available = buffer.remaining();
        // Maximum bytes available from source starting at offset
        int maxCopy = Math.min(length, src.length - offset);
        // Bound by destination capacity
        int toCopy = Math.min(maxCopy, Math.max(available, 0));
        if (toCopy <= 0) {
            return 0;
        }
        buffer.put(src, offset, toCopy);
        return toCopy;
    }

    /**
     * Returns all bytes written so far and resets the buffer for subsequent writes.
     */
    public synchronized byte[] readAll() {
        int pos = buffer.position();
        byte[] out = new byte[pos];
        buffer.rewind();
        buffer.get(out);
        buffer.clear();
        return out;
    }
}

