package com.wgu.app;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class DataProcessor {
    // Safely processes input data without exceeding buffer sizes.
    public byte[] process(byte[] input) {
        if (input == null) return new byte[0];
        // Use ByteArrayOutputStream to avoid manual buffer management
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        final int chunkSize = 4096;
        int offset = 0;
        while (offset < input.length) {
            int len = Math.min(chunkSize, input.length - offset);
            out.write(input, offset, len);
            offset += len;
        }
        return out.toByteArray();
    }

    public String processToString(byte[] input) {
        return new String(process(input), StandardCharsets.UTF_8);
    }

    // Safe copy with bounds checks
    public static int safeCopy(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
        if (src == null || dest == null) return 0;
        if (srcPos < 0 || destPos < 0 || length < 0) return 0;
        int maxLen = Math.min(length, Math.min(src.length - srcPos, dest.length - destPos));
        if (maxLen <= 0) return 0;
        System.arraycopy(src, srcPos, dest, destPos, maxLen);
        return maxLen;
    }
}

