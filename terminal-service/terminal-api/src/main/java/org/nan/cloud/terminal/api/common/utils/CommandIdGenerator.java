package org.nan.cloud.terminal.api.common.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class CommandIdGenerator {

    private CommandIdGenerator(){

    }

    // 1. 基准时间：2021-01-01 00:00:00 UTC (任意选一个较新的时间点)
    private static final long EPOCH = 1609459200000L;

    // 2. 原子计数器，只保留低 10 位 (0-1023)，防止同毫秒内冲突
    private static final int COUNTER_BITS = 10;
    private static final int COUNTER_MASK = (1 << COUNTER_BITS) - 1;
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    // 3. 总偏移，保证最小值 ≥ 10000
    private static final int BASE_OFFSET = 10000;

    /**
     * 生成一个不重复的 int ID：
     * ┌───────────┬───────────────┬─────────────────┐
     * │  31…(10+) │  (9) … (0)    │  Constant 10000 │
     * │ timestamp │   counter     │    offset       │
     * └───────────┴───────────────┴─────────────────┘
     */
    public static int generateId() {
        // 1) 取当前相对毫秒，并右移 COUNTER_BITS 位，确保给 counter 留下空间
        long now = System.currentTimeMillis() - EPOCH;
        long tsPart = (now << COUNTER_BITS);

        // 2) 取原子计数器的低 COUNTER_BITS 位
        int count = COUNTER.getAndUpdate(i -> (i + 1) & COUNTER_MASK);

        // 3) 组合三部分
        long raw = tsPart | count;       // | 等同于加，因为两者无位重叠
        long withOffset = raw + BASE_OFFSET;

        // 4) 如果超出 Integer.MAX_VALUE，就取模回绕
        if (withOffset > Integer.MAX_VALUE) {
            withOffset = (withOffset % Integer.MAX_VALUE) + BASE_OFFSET;
        }

        return (int) withOffset;
    }
}
