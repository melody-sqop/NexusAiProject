package com.cmt.NexusAi.modules.audit.L2a.util;

import java.util.List;

public class SimHashUtil {

    private static final int HASH_BITS = 64;

    public static long computeSimHash(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0L;
        }

        List<String> tokens = IkTokenizerUtil.tokenize(text);
        if (tokens == null || tokens.isEmpty()) {
            return 0L;
        }

        int[] weights = new int[HASH_BITS];

        for (String token : tokens) {
            long hash = MurmurHash3Util.hash64(token);
            for (int i = 0; i < HASH_BITS; i++) {
                long bit = (hash >>> i) & 1L;
                weights[i] += (bit == 1L) ? 1 : -1;
            }
        }

        long simHash = 0L;
        for (int i = 0; i < HASH_BITS; i++) {
            if (weights[i] > 0) {
                simHash |= (1L << i);
            }
        }
        return simHash;
    }

    public static int hammingDistance(long hash1, long hash2) {
        return Long.bitCount(hash1 ^ hash2);
    }
}