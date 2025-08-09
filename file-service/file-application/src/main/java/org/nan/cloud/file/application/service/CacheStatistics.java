package org.nan.cloud.file.application.service;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 缓存统计信息
 * 参照core-service标准，提供详细的缓存性能统计
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
public class CacheStatistics {
    
    // ==================== 基础统计 ====================
    
    /**
     * 统计时间
     */
    private LocalDateTime statisticsTime;
    
    /**
     * 缓存总命中次数
     */
    private long totalHitCount;
    
    /**
     * 缓存总未命中次数
     */
    private long totalMissCount;
    
    /**
     * 缓存命中率
     */
    private double hitRate;
    
    /**
     * 缓存总请求次数
     */
    private long totalRequestCount;
    
    // ==================== 本地缓存统计 ====================
    
    /**
     * 本地缓存命中次数
     */
    private long localHitCount;
    
    /**
     * 本地缓存未命中次数
     */
    private long localMissCount;
    
    /**
     * 本地缓存命中率
     */
    private double localHitRate;
    
    /**
     * 本地缓存条目数
     */
    private long localCacheSize;
    
    /**
     * 本地缓存最大大小
     */
    private long localMaxSize;
    
    /**
     * 本地缓存驱逐次数
     */
    private long localEvictionCount;
    
    /**
     * 本地缓存平均加载时间（纳秒）
     */
    private double localAverageLoadTime;
    
    // ==================== 分布式缓存统计 ====================
    
    /**
     * Redis缓存命中次数
     */
    private long redisHitCount;
    
    /**
     * Redis缓存未命中次数
     */
    private long redisMissCount;
    
    /**
     * Redis缓存命中率
     */
    private double redisHitRate;
    
    /**
     * Redis连接池活跃连接数
     */
    private int redisActiveConnections;
    
    /**
     * Redis连接池空闲连接数
     */
    private int redisIdleConnections;
    
    /**
     * Redis平均响应时间（毫秒）
     */
    private double redisAverageResponseTime;
    
    // ==================== 性能统计 ====================
    
    /**
     * 平均获取时间（毫秒）
     */
    private double averageGetTime;
    
    /**
     * 平均设置时间（毫秒）
     */
    private double averagePutTime;
    
    /**
     * 缓存加载次数
     */
    private long loadCount;
    
    /**
     * 缓存加载异常次数
     */
    private long loadExceptionCount;
    
    // ==================== 业务统计 ====================
    
    /**
     * 按缓存类型的命中率统计
     * Key: FileCacheType.name(), Value: 命中率
     */
    private Map<String, Double> hitRateByType;
    
    /**
     * 按缓存类型的请求次数统计
     * Key: FileCacheType.name(), Value: 请求次数
     */
    private Map<String, Long> requestCountByType;
    
    /**
     * 热门缓存键统计
     * Key: 缓存键, Value: 访问次数
     */
    private Map<String, Long> topHotKeys;
    
    /**
     * 缓存大小统计
     * Key: FileCacheType.name(), Value: 缓存条目数
     */
    private Map<String, Long> cacheSizeByType;
    
    // ==================== 计算方法 ====================
    
    /**
     * 计算总命中率
     * @return 命中率
     */
    public double calculateHitRate() {
        if (totalRequestCount == 0) {
            return 0.0;
        }
        return (double) totalHitCount / totalRequestCount;
    }
    
    /**
     * 计算本地缓存命中率
     * @return 本地缓存命中率
     */
    public double calculateLocalHitRate() {
        long localTotal = localHitCount + localMissCount;
        if (localTotal == 0) {
            return 0.0;
        }
        return (double) localHitCount / localTotal;
    }
    
    /**
     * 计算Redis缓存命中率
     * @return Redis缓存命中率
     */
    public double calculateRedisHitRate() {
        long redisTotal = redisHitCount + redisMissCount;
        if (redisTotal == 0) {
            return 0.0;
        }
        return (double) redisHitCount / redisTotal;
    }
    
    /**
     * 获取缓存效率评分（0-100分）
     * 综合考虑命中率、响应时间、错误率等因素
     */
    public double getCacheEfficiencyScore() {
        // 命中率权重 60%
        double hitRateScore = hitRate * 60;
        
        // 响应时间权重 25%（越低越好，以10ms为基准）
        double responseTimeScore = Math.max(0, 25 - (averageGetTime / 10) * 5);
        
        // 错误率权重 15%（越低越好）
        double errorRate = totalRequestCount > 0 ? (double) loadExceptionCount / totalRequestCount : 0;
        double errorScore = Math.max(0, 15 - errorRate * 100);
        
        return Math.min(100, hitRateScore + responseTimeScore + errorScore);
    }
    
    /**
     * 判断缓存性能是否健康
     * @return 是否健康
     */
    public boolean isHealthy() {
        return hitRate >= 0.8 && averageGetTime <= 50 && loadExceptionCount == 0;
    }
    
    /**
     * 获取性能建议
     * @return 性能建议列表
     */
    public java.util.List<String> getPerformanceSuggestions() {
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        
        if (hitRate < 0.7) {
            suggestions.add("缓存命中率偏低，建议检查缓存策略和TTL配置");
        }
        
        if (averageGetTime > 100) {
            suggestions.add("缓存获取时间偏长，建议检查网络延迟和Redis性能");
        }
        
        if (localHitRate < redisHitRate * 0.8) {
            suggestions.add("本地缓存效率偏低，建议增加本地缓存大小");
        }
        
        if (loadExceptionCount > 0) {
            suggestions.add("存在缓存加载异常，请检查数据源连接和异常处理");
        }
        
        if (localEvictionCount > localCacheSize) {
            suggestions.add("本地缓存频繁驱逐，建议增加最大缓存大小");
        }
        
        return suggestions;
    }
}