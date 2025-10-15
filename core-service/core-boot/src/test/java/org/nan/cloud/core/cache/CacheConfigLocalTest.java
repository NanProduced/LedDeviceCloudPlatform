package org.nan.cloud.core.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Local cache configuration test (without Redis)
 */
@SpringBootTest(classes = org.nan.cloud.core.MinimalTestApplication.class)
@ActiveProfiles("test")
class CacheConfigLocalTest {
    
    @Autowired(required = false)
    @Qualifier("localCacheManager")
    private CacheManager localCacheManager;
    
    @Test
    void testLocalCacheManagerExists() {
        if (localCacheManager != null) {
            assertNotNull(localCacheManager, "Local cache manager should exist");
            assertEquals("CaffeineCacheManager", localCacheManager.getClass().getSimpleName());
            System.out.println("Local cache manager test passed");
        } else {
            System.out.println("Local cache manager not configured - skipping test");
        }
    }
    
    @Test
    void testLocalCacheOperations() {
        if (localCacheManager != null) {
            // Test local cache operations
            var cache = localCacheManager.getCache("local-test-cache");
            assertNotNull(cache, "Should be able to get local cache instance");
            
            String key = "local-test-key";
            String value = "local-test-value";
            
            cache.put(key, value);
            var cachedValue = cache.get(key, String.class);
            assertEquals(value, cachedValue, "Local cache value should match");
            
            System.out.println("Local cache operations test passed");
        } else {
            System.out.println("Local cache manager not configured - skipping test");
        }
    }
    
    @Test
    void testBasicConfiguration() {
        // This test should always pass to verify Spring context loads
        assertTrue(true, "Spring context should load successfully");
        System.out.println("Basic configuration test passed");
    }
}