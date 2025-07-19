package org.nan.cloud.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单缓存功能测试
 * 不依赖Spring上下文，直接测试Caffeine缓存功能
 */
class SimpleCacheTest {
    
    private Cache<String, String> cache;
    
    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();
    }
    
    @Test
    void testBasicCacheOperations() {
        // 测试基本的缓存操作
        String key = "test-key";
        String value = "test-value";
        
        // 测试存储
        cache.put(key, value);
        
        // 测试读取
        String cachedValue = cache.getIfPresent(key);
        assertEquals(value, cachedValue, "缓存值应该匹配");
        
        // 测试删除
        cache.invalidate(key);
        String deletedValue = cache.getIfPresent(key);
        assertNull(deletedValue, "删除后应该返回null");
    }
    
    @Test
    void testCacheWithComputeIfAbsent() {
        String key = "compute-key";
        String expectedValue = "computed-value";
        
        // 测试计算并缓存
        String value = cache.get(key, k -> expectedValue);
        assertEquals(expectedValue, value, "计算的值应该匹配");
        
        // 验证缓存命中
        String cachedValue = cache.getIfPresent(key);
        assertEquals(expectedValue, cachedValue, "应该从缓存中获取值");
    }
    
    @Test
    void testCacheStats() {
        // 测试缓存统计
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        // 触发缓存命中
        cache.getIfPresent("key1");
        cache.getIfPresent("key1");
        
        // 触发缓存未命中
        cache.getIfPresent("nonexistent");
        
        var stats = cache.stats();
        assertEquals(2, stats.hitCount(), "应该有2次命中");
        assertEquals(1, stats.missCount(), "应该有1次未命中");
        assertTrue(stats.hitRate() > 0, "命中率应该大于0");
    }
    
    @Test
    void testCacheSize() {
        // 测试缓存大小
        assertEquals(0, cache.estimatedSize(), "初始大小应该为0");
        
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        assertEquals(2, cache.estimatedSize(), "添加2个元素后大小应该为2");
        
        cache.invalidateAll();
        assertEquals(0, cache.estimatedSize(), "清空后大小应该为0");
    }
    
    @Test 
    void testCachePerformance() {
        int testSize = 1000;
        
        // 测试写入性能
        long startTime = System.nanoTime();
        for (int i = 0; i < testSize; i++) {
            cache.put("perf-key-" + i, "perf-value-" + i);
        }
        long writeTime = System.nanoTime() - startTime;
        
        // 测试读取性能
        startTime = System.nanoTime();
        for (int i = 0; i < testSize; i++) {
            String value = cache.getIfPresent("perf-key-" + i);
            assertNotNull(value);
        }
        long readTime = System.nanoTime() - startTime;
        
        System.out.println("性能测试结果:");
        System.out.println("写入 " + testSize + " 条数据耗时: " + TimeUnit.NANOSECONDS.toMillis(writeTime) + "ms");
        System.out.println("读取 " + testSize + " 条数据耗时: " + TimeUnit.NANOSECONDS.toMillis(readTime) + "ms");
        
        // 性能断言 - 本地缓存应该很快
        assertTrue(TimeUnit.NANOSECONDS.toMillis(writeTime) < 100, "写入应该在100ms内完成");
        assertTrue(TimeUnit.NANOSECONDS.toMillis(readTime) < 50, "读取应该在50ms内完成");
    }
}