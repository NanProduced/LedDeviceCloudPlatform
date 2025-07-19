package org.nan.cloud.core.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存性能测试
 */
@SpringBootTest
@ActiveProfiles("test")
class CachePerformanceTest {
    
    @Autowired
    private CacheManager redisCacheManager;
    
    @Autowired
    @Qualifier("localCacheManager")
    private CacheManager localCacheManager;
    
    private static final int TEST_SIZE = 1000;
    private static final String TEST_CACHE_NAME = "performance-test";
    
    @BeforeEach
    void setUp() {
        // 清理测试缓存
        var redisCache = redisCacheManager.getCache(TEST_CACHE_NAME);
        var localCache = localCacheManager.getCache(TEST_CACHE_NAME);
        
        if (redisCache != null) {
            redisCache.clear();
        }
        if (localCache != null) {
            localCache.clear();
        }
    }
    
    @Test
    void testLocalCachePerformance() {
        var cache = localCacheManager.getCache(TEST_CACHE_NAME);
        assertNotNull(cache);
        
        // 测试写性能
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_SIZE; i++) {
            cache.put("key-" + i, "value-" + i);
        }
        long writeTime = System.nanoTime() - startTime;
        
        // 测试读性能
        startTime = System.nanoTime();
        for (int i = 0; i < TEST_SIZE; i++) {
            String value = cache.get("key-" + i, String.class);
            assertNotNull(value);
        }
        long readTime = System.nanoTime() - startTime;
        
        System.out.println("本地缓存性能测试结果:");
        System.out.println("写入 " + TEST_SIZE + " 条数据耗时: " + TimeUnit.NANOSECONDS.toMillis(writeTime) + "ms");
        System.out.println("读取 " + TEST_SIZE + " 条数据耗时: " + TimeUnit.NANOSECONDS.toMillis(readTime) + "ms");
        System.out.println("平均写入耗时: " + TimeUnit.NANOSECONDS.toMicros(writeTime / TEST_SIZE) + "μs");
        System.out.println("平均读取耗时: " + TimeUnit.NANOSECONDS.toMicros(readTime / TEST_SIZE) + "μs");
        
        // 性能断言 - 本地缓存应该很快
        assertTrue(TimeUnit.NANOSECONDS.toMillis(writeTime) < 100, "本地缓存写入应该在100ms内完成");
        assertTrue(TimeUnit.NANOSECONDS.toMillis(readTime) < 50, "本地缓存读取应该在50ms内完成");
    }
    
    @Test
    void testRedisCachePerformance() {
        var cache = redisCacheManager.getCache(TEST_CACHE_NAME);
        assertNotNull(cache);
        
        // 测试写性能
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_SIZE / 10; i++) { // Redis测试数量减少，避免网络延迟影响
            cache.put("redis-key-" + i, "redis-value-" + i);
        }
        long writeTime = System.nanoTime() - startTime;
        
        // 测试读性能
        startTime = System.nanoTime();
        for (int i = 0; i < TEST_SIZE / 10; i++) {
            String value = cache.get("redis-key-" + i, String.class);
            assertNotNull(value);
        }
        long readTime = System.nanoTime() - startTime;
        
        System.out.println("Redis缓存性能测试结果:");
        System.out.println("写入 " + (TEST_SIZE/10) + " 条数据耗时: " + TimeUnit.NANOSECONDS.toMillis(writeTime) + "ms");
        System.out.println("读取 " + (TEST_SIZE/10) + " 条数据耗时: " + TimeUnit.NANOSECONDS.toMillis(readTime) + "ms");
        System.out.println("平均写入耗时: " + TimeUnit.NANOSECONDS.toMicros(writeTime / (TEST_SIZE/10)) + "μs");
        System.out.println("平均读取耗时: " + TimeUnit.NANOSECONDS.toMicros(readTime / (TEST_SIZE/10)) + "μs");
    }
    
    @Test
    void testCacheHitRatio() {
        var cache = localCacheManager.getCache(TEST_CACHE_NAME);
        assertNotNull(cache);
        
        // 写入测试数据
        for (int i = 0; i < 100; i++) {
            cache.put("hit-test-" + i, "value-" + i);
        }
        
        // 测试缓存命中
        int hits = 0;
        int total = 150; // 包含50个不存在的key
        
        for (int i = 0; i < total; i++) {
            String value = cache.get("hit-test-" + i, String.class);
            if (value != null) {
                hits++;
            }
        }
        
        double hitRatio = (double) hits / total;
        System.out.println("缓存命中率测试:");
        System.out.println("命中次数: " + hits + "/" + total);
        System.out.println("命中率: " + String.format("%.2f%%", hitRatio * 100));
        
        assertEquals(100, hits, "应该命中100次");
        assertEquals(0.67, hitRatio, 0.01, "命中率应该约为67%");
    }
}