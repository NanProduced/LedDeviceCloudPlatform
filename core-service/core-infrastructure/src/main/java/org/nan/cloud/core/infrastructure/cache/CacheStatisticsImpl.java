package org.nan.cloud.core.infrastructure.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.nan.cloud.core.service.CacheStatistics;

/**
 * 缓存统计信息实现
 */
public class CacheStatisticsImpl implements CacheStatistics {
    
    private final CacheStats stats;
    
    public CacheStatisticsImpl(CacheStats stats) {
        this.stats = stats;
    }
    
    @Override
    public long getHitCount() {
        return stats.hitCount();
    }
    
    @Override
    public long getMissCount() {
        return stats.missCount();
    }
    
    @Override
    public double getHitRate() {
        return stats.hitRate();
    }
    
    @Override
    public long getEvictionCount() {
        return stats.evictionCount();
    }
    
    @Override
    public long getLoadCount() {
        return stats.loadCount();
    }
    
    @Override
    public double getAverageLoadTime() {
        return stats.averageLoadPenalty();
    }
}