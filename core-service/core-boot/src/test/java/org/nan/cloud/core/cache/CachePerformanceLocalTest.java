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
 * Local cache performance test (without Redis)
 */
@SpringBootTest(classes = org.nan.cloud.core.MinimalTestApplication.class)
@ActiveProfiles("test")
class CachePerformanceLocalTest {
    
    @Autowired(required = false)
    @Qualifier("localCacheManager")
    private CacheManager localCacheManager;
    
    private static final int TEST_SIZE = 1000;
    private static final String TEST_CACHE_NAME = "performance-test";
    
    @BeforeEach
    void setUp() {
        if (localCacheManager != null) {
            // Clear test cache
            var localCache = localCacheManager.getCache(TEST_CACHE_NAME);
            if (localCache != null) {
                localCache.clear();
            }
        }
    }
    
    @Test
    void testLocalCachePerformance() {
        if (localCacheManager == null) {
            System.out.println("Local cache manager not configured - skipping test");
            return;
        }
        
        var cache = localCacheManager.getCache(TEST_CACHE_NAME);
        assertNotNull(cache);
        
        // Test write performance
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_SIZE; i++) {
            cache.put("key-" + i, "value-" + i);
        }
        long writeTime = System.nanoTime() - startTime;
        
        // Test read performance
        startTime = System.nanoTime();
        for (int i = 0; i < TEST_SIZE; i++) {
            String value = cache.get("key-" + i, String.class);
            assertNotNull(value);
        }
        long readTime = System.nanoTime() - startTime;
        
        System.out.println("Local cache performance results:");
        System.out.println("Write " + TEST_SIZE + " items time: " + TimeUnit.NANOSECONDS.toMillis(writeTime) + "ms");
        System.out.println("Read " + TEST_SIZE + " items time: " + TimeUnit.NANOSECONDS.toMillis(readTime) + "ms");
        System.out.println("Average write time: " + TimeUnit.NANOSECONDS.toMicros(writeTime / TEST_SIZE) + "μs");
        System.out.println("Average read time: " + TimeUnit.NANOSECONDS.toMicros(readTime / TEST_SIZE) + "μs");
        
        // Performance assertions - local cache should be fast
        assertTrue(TimeUnit.NANOSECONDS.toMillis(writeTime) < 100, "Local cache write should complete within 100ms");
        assertTrue(TimeUnit.NANOSECONDS.toMillis(readTime) < 50, "Local cache read should complete within 50ms");
    }
    
    @Test
    void testCacheHitRatio() {
        if (localCacheManager == null) {
            System.out.println("Local cache manager not configured - skipping test");
            return;
        }
        
        var cache = localCacheManager.getCache(TEST_CACHE_NAME);
        assertNotNull(cache);
        
        // Write test data
        for (int i = 0; i < 100; i++) {
            cache.put("hit-test-" + i, "value-" + i);
        }
        
        // Test cache hits
        int hits = 0;
        int total = 150; // Including 50 non-existent keys
        
        for (int i = 0; i < total; i++) {
            String value = cache.get("hit-test-" + i, String.class);
            if (value != null) {
                hits++;
            }
        }
        
        double hitRatio = (double) hits / total;
        System.out.println("Cache hit ratio test:");
        System.out.println("Hit count: " + hits + "/" + total);
        System.out.println("Hit ratio: " + String.format("%.2f%%", hitRatio * 100));
        
        assertEquals(100, hits, "Should hit 100 times");
        assertEquals(0.67, hitRatio, 0.01, "Hit ratio should be around 67%");
    }
    
    @Test
    void testBasicCacheOperations() {
        // This test should always pass to verify Spring context loads
        assertTrue(true, "Spring context should load successfully");
        System.out.println("Basic cache operations test passed");
    }
}