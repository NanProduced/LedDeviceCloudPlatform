package org.nan.cloud.core.service;

/**
 * 缓存统计信息接口
 */
public interface CacheStatistics {
    
    /**
     * 获取命中次数
     */
    long getHitCount();
    
    /**
     * 获取未命中次数
     */
    long getMissCount();
    
    /**
     * 获取命中率
     */
    double getHitRate();
    
    /**
     * 获取驱逐次数
     */
    long getEvictionCount();
    
    /**
     * 获取加载次数
     */
    long getLoadCount();
    
    /**
     * 获取平均加载时间（纳秒）
     */
    double getAverageLoadTime();
}