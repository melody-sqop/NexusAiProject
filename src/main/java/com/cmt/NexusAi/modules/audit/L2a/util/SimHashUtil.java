package com.cmt.NexusAi.modules.audit.L2a.util;

import java.util.List;

public class SimHashUtil {

    public static long compute(String text) {
        List<String> words = IkTokenizerUtil.segment(text);
        int[] score = new int[64];

        for (String word : words) {
            if (word == null || word.trim().isEmpty()) continue;

            long hash = MurmurHash3Util.hash64(word);
            int weight = 1; // 生产环境可接TF-IDF提升关键词权重

            for (int i = 0; i < 64; i++) {
                long bit = (hash >> i) & 1L;
                if (bit == 1) {
                    score[i] += weight;
                } else {
                    score[i] -= weight;
                }
            }
        }

        long simHash = 0L;
        for (int i = 0; i < 64; i++) {
            if (score[i] > 0) {
                simHash |= (1L << i);
            }
        }
        return simHash;
    }

    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    public static int[] split4(long simHash) {
        int[] s = new int[4];
        s[0] = (int) (simHash & 0xFFFFL);
        s[1] = (int) ((simHash >> 16) & 0xFFFFL);
        s[2] = (int) ((simHash >> 32) & 0xFFFFL);
        s[3] = (int) ((simHash >> 48) & 0xFFFFL);
        return s;
    }
}