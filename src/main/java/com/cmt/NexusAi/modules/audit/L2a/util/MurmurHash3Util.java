package com.cmt.NexusAi.modules.audit.L2a.util;

import java.nio.charset.StandardCharsets;

public final class MurmurHash3Util {

    public static long hash64(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        int length = data.length;

        long seed = 0xe17a1465L;
        long m = 0xc6a4a7935bd1e995L;
        int r = 47;

        long h = seed ^ (length * m);

        int len = length;
        int i = 0;
        while (len >= 8) {
            long k = ((long) data[i] & 0xffL)
                    | (((long) data[i + 1] & 0xffL) << 8)
                    | (((long) data[i + 2] & 0xffL) << 16)
                    | (((long) data[i + 3] & 0xffL) << 24)
                    | (((long) data[i + 4] & 0xffL) << 32)
                    | (((long) data[i + 5] & 0xffL) << 40)
                    | (((long) data[i + 6] & 0xffL) << 48)
                    | (((long) data[i + 7] & 0xffL) << 56);

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;

            i += 8;
            len -= 8;
        }

        switch (len) {
            case 7: h ^= (data[i + 6] & 0xffL) << 48;
            case 6: h ^= (data[i + 5] & 0xffL) << 40;
            case 5: h ^= (data[i + 4] & 0xffL) << 32;
            case 4: h ^= (data[i + 3] & 0xffL) << 24;
            case 3: h ^= (data[i + 2] & 0xffL) << 16;
            case 2: h ^= (data[i + 1] & 0xffL) << 8;
            case 1: h ^= (data[i] & 0xffL);
                h *= m;
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        return h;
    }
}