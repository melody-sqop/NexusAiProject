package com.cmt.NexusAi.modules.thumb.manager;

import cn.hutool.core.util.HashUtil;
import lombok.Data;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class HeavyKeeper implements TopK {
    private static final int LOOKUP_TABLE_SIZE = 256;
    private final int k;
    private final int width;
    private final int depth;
    private final double[] lookupTable;
    private final Bucket[][] buckets;
    private final PriorityQueue<Node> minHeap;
    private final BlockingQueue<Item> expelledQueue;
    private final Random random;
    private final AtomicLong total;
    private final int minCount;

    public HeavyKeeper(int k, int width, int depth, double decay, int minCount) {
        this.k = k;
        this.width = width;
        this.depth = depth;
        this.minCount = minCount;

        this.lookupTable = new double[LOOKUP_TABLE_SIZE];
        for (int i = 0; i < LOOKUP_TABLE_SIZE; i++) {
            lookupTable[i] = Math.pow(decay, i);
        }

        this.buckets = new Bucket[depth][width];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                buckets[i][j] = new Bucket();
            }
        }

        this.minHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
        this.expelledQueue = new LinkedBlockingQueue<>();
        this.random = new Random();
        this.total = new AtomicLong(0); // 初始化 AtomicLong
    }



    @Override
    public List<Item> list() {
        synchronized (minHeap) {
            List<Item> result = new ArrayList<>(minHeap.size());
            for (Node node : minHeap) {
                result.add(new Item(node.key, node.count));
            }
            result.sort((a, b) -> Integer.compare(b.count(), a.count()));
            return result;
        }
    }

    @Override
    public BlockingQueue<Item> expelled() {
        return expelledQueue;
    }


    /**
     * 衰减方法
     * 每20秒将桶和top100里面的右移1位 即除以2
     * 保证当前一直都是热点数据
     */
    @Override
    public void fading() {
        for (Bucket[] row : buckets) {
            for (Bucket bucket : row) {
                synchronized (bucket) {
                    bucket.count = bucket.count >> 1;
                }
            }
        }

        synchronized (minHeap) {
            PriorityQueue<Node> newHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
            for (Node node : minHeap) {
                newHeap.add(new Node(node.key, node.count >> 1));
            }
            minHeap.clear();
            minHeap.addAll(newHeap);
        }

        total.updateAndGet(t -> t >> 1);
    }

    @Override
    public long total() {
        return total.get();
    }

    private static class Bucket {
        long fingerprint;  // key指纹
        int count; // 访问次数
    }

    private static class Node {
        final String key;
        final int count;

        Node(String key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    @Override
    public AddResult add(String key, int increment) {
        byte[] keyBytes = key.getBytes();
        long itemFingerprint = hashFingerprint(keyBytes);
        int maxCount = 0;

        for (int i = 0; i < depth; i++) {
            // ✅ 修复 2：手动将种子 i 拼接到 byte[] 后面，模拟带种子的哈希
            byte[] bytesWithSeed = mergeBytes(keyBytes, intToBytes(i));
            int bucketHash = hash(bytesWithSeed);
            int bucketNumber = (bucketHash & Integer.MAX_VALUE) % width;

            Bucket bucket = buckets[i][bucketNumber];

            // 这里锁的是5个大桶中 1w个小桶中的1个桶  颗粒度极低 性能很高
            synchronized (bucket) {
                if (bucket.count == 0) {
                    // hash后的桶中如果为空 则将key的指纹和访问次数都保存起来
                    bucket.fingerprint = itemFingerprint;
                    bucket.count = increment;
                    maxCount = Math.max(maxCount, increment);
                } else if (bucket.fingerprint == itemFingerprint) {
                    // 如果hash后的桶中已经存在该key 则访问次数进行+1
                    bucket.count += increment;
                    maxCount = Math.max(maxCount, bucket.count);
                } else {
                    // hash后的桶如果跟其他热点冲突 则通过概率尝试-1
                    for (int j = 0; j < increment; j++) {
                        double decay = bucket.count < LOOKUP_TABLE_SIZE ?
                                lookupTable[bucket.count] :
                                lookupTable[LOOKUP_TABLE_SIZE - 1];
                        if (random.nextDouble() < decay) {
                            bucket.count--;
                            if (bucket.count == 0) {
                                // 当冲突后减1已经为0 则将当前指纹替换上去 并将访问次数变成1
                                bucket.fingerprint = itemFingerprint;
                                bucket.count = increment - j;
                                maxCount = Math.max(maxCount, bucket.count);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 所有博客的全局访问量+1  用来监控总请求数或者进行判断热点数据的比例进行维护作用
        total.addAndGet(increment);

        // 判断是否是热点数据 我们这里设置的阈值是大于10 大于10则为热点数据
        if (maxCount < minCount) {
            // 访问量小于10 不是热点数据
            return new AddResult(null, false, null);
        }

        // 访问量大于10  是热点数据 判断top100中是否存在该数据
        synchronized (minHeap) {
            boolean isHot = false;
            String expelled = null;

            Optional<Node> existing = minHeap.stream()
                    .filter(n -> n.key.equals(key))
                    .findFirst();

            // 热点榜单中存在该数据 替换下来更新成新的访问次数
            if (existing.isPresent()) {
                minHeap.remove(existing.get());
                minHeap.add(new Node(key, maxCount));
                isHot = true;
            } else {
                // 热点榜单中不存在该数据 则分支判断是要直接放进去还是删除里面最旧的冷数据
                if (minHeap.size() < k || maxCount >= Objects.requireNonNull(minHeap.peek()).count) {
                    Node newNode = new Node(key, maxCount);
                    // 删除里面最旧的冷数据即最后一名 替换上新的热点数据
                    if (minHeap.size() >= k) {
                        expelled = minHeap.poll().key;
                        // 将旧数据存到寄出队列
                        expelledQueue.offer(new Item(expelled, maxCount));
                    }
                    minHeap.add(newNode);
                    isHot = true;
                }
            }

            // 判断是否有挤进去 是否是热点数据 原始key即博客id
            return new AddResult(expelled, isHot, key);
        }
    }


    // 原有的 hash 方法改为带种子
    private static int hash(byte[] data) {
        return HashUtil.murmur32(data);
    }

    // 新增：专门用于生成指纹的方法（用固定种子或 64 位哈希）
    private static long hashFingerprint(byte[] data) {
        // 指纹用 64 位哈希，减少碰撞概率
        return HashUtil.murmur64(data);
    }

    // ✅ 新增辅助方法：int 转 byte[]
    private static byte[] intToBytes(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    // ✅ 新增辅助方法：拼接两个 byte[]
    private static byte[] mergeBytes(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

}
// 新增返回结果类
@Data
class AddResult {
    // 被挤出的 key
    private final String expelledKey;
    // 当前 key 是否进入 TopK
    private final boolean isHotKey;
    // 当前操作的 key
    private final String currentKey;

    public AddResult(String expelledKey, boolean isHotKey, String currentKey) {
        this.expelledKey = expelledKey;
        this.isHotKey = isHotKey;
        this.currentKey = currentKey;
    }

}
