package org.nan.cloud.core.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存配置测试
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheConfigTest {
    
    @Autowired
    private CacheManager redisCacheManager;
    
    @Autowired
    @Qualifier("localCacheManager")
    private CacheManager localCacheManager;
    
    @Test
    void testRedisCacheManagerExists() {
        assertNotNull(redisCacheManager, "Redis缓存管理器应该存在");
        assertEquals("RedisCacheManager", redisCacheManager.getClass().getSimpleName());
    }
    
    @Test
    void testLocalCacheManagerExists() {
        assertNotNull(localCacheManager, "本地缓存管理器应该存在");
        assertEquals("CaffeineCacheManager", localCacheManager.getClass().getSimpleName());
    }
    
    @Test
    void testCacheOperations() {
        // 测试Redis缓存操作
        var cache = redisCacheManager.getCache("test-cache");
        assertNotNull(cache, "应该能够获取缓存实例");
        
        // 测试缓存存取
        String key = "test-key";
        String value = "test-value";
        
        cache.put(key, value);
        var cachedValue = cache.get(key, String.class);
        assertEquals(value, cachedValue, "缓存值应该匹配");
        
        // 测试缓存清理
        cache.evict(key);
        var evictedValue = cache.get(key);
        assertNull(evictedValue, "清理后缓存值应该为null");
    }
    
    @Test
    void testLocalCacheOperations() {
        // 测试本地缓存操作
        var cache = localCacheManager.getCache("local-test-cache");
        assertNotNull(cache, "应该能够获取本地缓存实例");
        
        String key = "local-test-key";
        String value = "local-test-value";
        
        cache.put(key, value);
        var cachedValue = cache.get(key, String.class);
        assertEquals(value, cachedValue, "本地缓存值应该匹配");
    }
}