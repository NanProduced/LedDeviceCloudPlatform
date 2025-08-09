package org.nan.cloud.file.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.enums.FileCacheType;
import org.nan.cloud.file.application.service.CacheService;
import org.nan.cloud.file.application.service.CacheStatistics;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * File Service缓存服务实现
 * 参照core-service标准实现，提供Redis + Caffeine双层缓存
 * 
 * 架构特性：
 * 1. 双层缓存：本地Caffeine + 分布式Redis
 * 2. 智能缓存策略：根据FileCacheType自动选择缓存层
 * 3. 异步操作：支持异步读写，提升性能
 * 4. 分布式锁：基于Redis的分布式锁实现
 * 5. 统计监控：详细的缓存性能统计
 * 6. 自动清理：过期缓存自动清理机制
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceCacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AsyncCache<String, Object> localCache;
    private final ObjectMapper objectMapper;
    
    // 统一缓存键前缀
    private static final String CACHE_KEY_PREFIX = "file:cache:";
    private static final String LOCK_KEY_PREFIX = "file:lock:";
    private static final String STATS_KEY_PREFIX = "file:stats:";
    
    // 访问统计
    private final Map<String, Long> accessStats = new ConcurrentHashMap<>();
    private final Map<String, Long> hitStats = new ConcurrentHashMap<>();
    private final Map<String, Long> missStats = new ConcurrentHashMap<>();
    private final Map<String, Long> responseTimeStats = new ConcurrentHashMap<>();
    
    // ==================== 基础缓存操作 ====================
    
    @Override
    public <T> T get(String key, Class<T> clazz) {
        return get(key, null, clazz);
    }
    
    @Override
    public <T> T get(String key, Supplier<T> defaultValueSupplier, Class<T> clazz) {
        return get(key, defaultValueSupplier, null, clazz);
    }
    
    @Override
    public <T> T get(String key, Supplier<T> defaultValueSupplier, Duration ttl, Class<T> clazz) {
        long startTime = System.currentTimeMillis();
        String fullKey = buildFullKey(key);
        
        try {
            // 记录访问统计
            accessStats.merge(key, 1L, Long::sum);
            
            // 首先尝试从本地缓存获取
            CompletableFuture<Object> localResult = localCache.getIfPresent(fullKey);
            if (localResult != null && !localResult.isCompletedExceptionally()) {
                Object localValue = localResult.join();
                if (localValue != null) {
                    hitStats.merge(key, 1L, Long::sum);
                    recordResponseTime(key, System.currentTimeMillis() - startTime);
                    return convertValue(localValue, clazz);
                }
            }
            
            // 从Redis获取
            Object redisValue = redisTemplate.opsForValue().get(fullKey);
            if (redisValue != null) {
                // 回写到本地缓存
                localCache.put(fullKey, CompletableFuture.completedFuture(redisValue));
                hitStats.merge(key, 1L, Long::sum);
                recordResponseTime(key, System.currentTimeMillis() - startTime);
                return convertValue(redisValue, clazz);
            }
            
            // 缓存未命中，使用默认值提供者
            missStats.merge(key, 1L, Long::sum);
            if (defaultValueSupplier != null) {
                T defaultValue = defaultValueSupplier.get();
                if (defaultValue != null) {
                    // 存储默认值到缓存
                    if (ttl != null) {
                        put(key, defaultValue, ttl);
                    } else {
                        put(key, defaultValue);
                    }
                }
                recordResponseTime(key, System.currentTimeMillis() - startTime);
                return defaultValue;
            }
            
            recordResponseTime(key, System.currentTimeMillis() - startTime);
            return null;
            
        } catch (Exception e) {
            log.error("获取缓存失败 - key: {}", fullKey, e);
            recordResponseTime(key, System.currentTimeMillis() - startTime);
            return defaultValueSupplier != null ? defaultValueSupplier.get() : null;
        }
    }
    
    @Override
    public <T> Map<String, T> multiGet(Collection<String> keys, Class<T> clazz) {
        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }
        
        long startTime = System.currentTimeMillis();
        Map<String, T> result = new HashMap<>();
        
        try {
            // 构建完整键列表
            List<String> fullKeys = keys.stream()
                    .map(this::buildFullKey)
                    .collect(Collectors.toList());
            
            // 批量从Redis获取
            List<Object> values = redisTemplate.opsForValue().multiGet(fullKeys);
            
            int index = 0;
            for (String originalKey : keys) {
                if (values != null && index < values.size()) {
                    Object value = values.get(index);
                    if (value != null) {
                        result.put(originalKey, convertValue(value, clazz));
                        hitStats.merge(originalKey, 1L, Long::sum);
                    } else {
                        missStats.merge(originalKey, 1L, Long::sum);
                    }
                }
                accessStats.merge(originalKey, 1L, Long::sum);
                index++;
            }
            
            log.debug("批量获取缓存完成 - keys: {}, 命中: {}/{}", keys.size(), result.size(), keys.size());
            
        } catch (Exception e) {
            log.error("批量获取缓存失败 - keys: {}", keys, e);
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            keys.forEach(key -> recordResponseTime(key, responseTime));
        }
        
        return result;
    }
    
    @Override
    public <T> T getWithCacheTypeConfig(String key, FileCacheType cacheType, Class<T> clazz) {
        String fullKey = buildCacheTypeKey(key, cacheType);
        
        // 根据缓存类型配置选择缓存策略
        if (cacheType.isUseLocalCache() && cacheType.isUseDistributedCache()) {
            // 使用双层缓存
            return get(fullKey, null, cacheType.getDefaultTtl(), clazz);
        } else if (cacheType.isUseDistributedCache()) {
            // 仅使用Redis
            return getFromRedis(fullKey, clazz);
        } else if (cacheType.isUseLocalCache()) {
            // 仅使用本地缓存
            return getFromLocal(fullKey, clazz);
        }
        
        return null;
    }
    
    // ==================== 缓存存储操作 ====================
    
    @Override
    public void put(String key, Object value) {
        put(key, value, Duration.ofMinutes(30)); // 默认30分钟
    }
    
    @Override
    public void put(String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        
        String fullKey = buildFullKey(key);
        
        try {
            // 存储到Redis
            redisTemplate.opsForValue().set(fullKey, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            
            // 存储到本地缓存
            localCache.put(fullKey, CompletableFuture.completedFuture(value));
            
            log.debug("缓存存储成功 - key: {}, ttl: {}ms", fullKey, ttl.toMillis());
            
        } catch (Exception e) {
            log.error("存储缓存失败 - key: {}", fullKey, e);
        }
    }
    
    @Override
    public void putWithCacheTypeConfig(String key, Object value, FileCacheType cacheType, Duration customTtl) {
        if (value == null) {
            return;
        }
        
        String fullKey = buildCacheTypeKey(key, cacheType);
        Duration ttl = customTtl != null ? customTtl : cacheType.getDefaultTtl();
        
        try {
            // 根据缓存类型配置选择存储策略
            if (cacheType.isUseDistributedCache()) {
                redisTemplate.opsForValue().set(fullKey, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            }
            
            if (cacheType.isUseLocalCache()) {
                localCache.put(fullKey, CompletableFuture.completedFuture(value));
            }
            
            log.debug("按类型存储缓存成功 - key: {}, type: {}, ttl: {}ms", 
                     fullKey, cacheType.name(), ttl.toMillis());
            
        } catch (Exception e) {
            log.error("按类型存储缓存失败 - key: {}, type: {}", fullKey, cacheType.name(), e);
        }
    }
    
    @Override
    public void multiPut(Map<String, Object> keyValueMap, Duration ttl) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            return;
        }
        
        try {
            // 构建Redis批量操作的键值对
            Map<String, Object> redisMap = keyValueMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> buildFullKey(entry.getKey()),
                            Map.Entry::getValue
                    ));
            
            // 批量存储到Redis
            redisTemplate.opsForValue().multiSet(redisMap);
            
            // 设置过期时间（需要逐个设置）
            redisMap.keySet().forEach(fullKey -> 
                    redisTemplate.expire(fullKey, ttl.toMillis(), TimeUnit.MILLISECONDS)
            );
            
            // 存储到本地缓存
            keyValueMap.forEach((key, value) -> {
                String fullKey = buildFullKey(key);
                localCache.put(fullKey, CompletableFuture.completedFuture(value));
            });
            
            log.debug("批量缓存存储成功 - size: {}, ttl: {}ms", keyValueMap.size(), ttl.toMillis());
            
        } catch (Exception e) {
            log.error("批量存储缓存失败 - size: {}", keyValueMap.size(), e);
        }
    }
    
    // ==================== 缓存删除操作 ====================
    
    @Override
    public void evict(String key) {
        String fullKey = buildFullKey(key);
        
        try {
            // 从Redis删除
            redisTemplate.delete(fullKey);
            
            // 从本地缓存删除
            localCache.synchronous().invalidate(fullKey);
            
            log.debug("缓存删除成功 - key: {}", fullKey);
            
        } catch (Exception e) {
            log.error("删除缓存失败 - key: {}", fullKey, e);
        }
    }
    
    @Override
    public void multiEvict(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        try {
            // 构建完整键列表
            Collection<String> fullKeys = keys.stream()
                    .map(this::buildFullKey)
                    .collect(Collectors.toList());
            
            // 批量从Redis删除
            redisTemplate.delete(fullKeys);
            
            // 从本地缓存删除
            localCache.synchronous().invalidateAll(fullKeys);
            
            log.debug("批量缓存删除成功 - size: {}", keys.size());
            
        } catch (Exception e) {
            log.error("批量删除缓存失败 - keys: {}", keys, e);
        }
    }
    
    @Override
    public void evictByPattern(String pattern) {
        try {
            String fullPattern = CACHE_KEY_PREFIX + pattern;
            Set<String> keys = redisTemplate.keys(fullPattern);
            
            if (keys != null && !keys.isEmpty()) {
                // 批量删除Redis中的键
                redisTemplate.delete(keys);
                
                // 删除本地缓存中的对应键
                localCache.synchronous().invalidateAll(keys);
                
                log.debug("按模式删除缓存成功 - pattern: {}, count: {}", fullPattern, keys.size());
            }
            
        } catch (Exception e) {
            log.error("按模式删除缓存失败 - pattern: {}", pattern, e);
        }
    }
    
    @Override
    public boolean hasKey(String key) {
        String fullKey = buildFullKey(key);
        
        try {
            // 首先检查本地缓存
            CompletableFuture<Object> localResult = localCache.getIfPresent(fullKey);
            if (localResult != null && !localResult.isCompletedExceptionally()) {
                return localResult.join() != null;
            }
            
            // 检查Redis
            return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
            
        } catch (Exception e) {
            log.error("检查缓存键存在性失败 - key: {}", fullKey, e);
            return false;
        }
    }
    
    // ==================== 异步操作 ====================
    
    @Override
    public <T> CompletableFuture<T> getAsync(String key, Supplier<T> defaultValueSupplier, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> get(key, defaultValueSupplier, clazz));
    }
    
    @Override
    public CompletableFuture<Void> putAsync(String key, Object value, Duration ttl) {
        return CompletableFuture.runAsync(() -> put(key, value, ttl));
    }
    
    // ==================== 缓存统计与监控 ====================
    
    @Override
    public CacheStatistics getStatistics() {
        try {
            // 获取本地缓存统计
            Cache<String, Object> syncCache = localCache.synchronous();
            CacheStats localStats = syncCache.stats();
            
            // 获取Redis连接统计（简化实现）
            // 在实际环境中，可以通过Redis INFO命令获取更详细的统计信息
            
            // 计算总体统计
            long totalHit = hitStats.values().stream().mapToLong(Long::longValue).sum();
            long totalMiss = missStats.values().stream().mapToLong(Long::longValue).sum();
            long totalAccess = accessStats.values().stream().mapToLong(Long::longValue).sum();
            
            double hitRate = totalAccess > 0 ? (double) totalHit / totalAccess : 0.0;
            
            // 计算平均响应时间
            double avgResponseTime = responseTimeStats.isEmpty() ? 0.0 :
                    responseTimeStats.values().stream().mapToLong(Long::longValue).average().orElse(0.0);
            
            return CacheStatistics.builder()
                    .statisticsTime(LocalDateTime.now())
                    .totalHitCount(totalHit)
                    .totalMissCount(totalMiss)
                    .hitRate(hitRate)
                    .totalRequestCount(totalAccess)
                    .localHitCount(localStats.hitCount())
                    .localMissCount(localStats.missCount())
                    .localHitRate(localStats.hitRate())
                    .localCacheSize(syncCache.estimatedSize())
                    .localEvictionCount(localStats.evictionCount())
                    .localAverageLoadTime(localStats.averageLoadPenalty())
                    .averageGetTime(avgResponseTime)
                    .loadCount(localStats.loadCount())
                    .loadExceptionCount(localStats.loadCount() - (localStats.hitCount() + localStats.missCount()))
                    .hitRateByType(calculateHitRateByType())
                    .requestCountByType(getRequestCountByType())
                    .topHotKeys(getTopHotKeys(10))
                    .build();
                    
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return CacheStatistics.builder()
                    .statisticsTime(LocalDateTime.now())
                    .build();
        }
    }
    
    @Override
    public long cleanupExpiredCache() {
        try {
            // 清理本地缓存
            localCache.synchronous().cleanUp();
            
            // Redis自动过期，这里主要清理统计数据
            long cleanedCount = 0;
            
            // 清理过期的统计数据（保留最近1小时的数据）
            long expireTime = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
            
            cleanedCount += cleanupExpiredStats(accessStats, expireTime);
            cleanedCount += cleanupExpiredStats(hitStats, expireTime);
            cleanedCount += cleanupExpiredStats(missStats, expireTime);
            cleanedCount += cleanupExpiredStats(responseTimeStats, expireTime);
            
            log.info("缓存清理完成 - 清理条目数: {}", cleanedCount);
            return cleanedCount;
            
        } catch (Exception e) {
            log.error("缓存清理失败", e);
            return 0;
        }
    }
    
    // ==================== 分布式锁支持 ====================
    
    @Override
    public boolean acquireLock(String lockKey, Duration expireTime) {
        String fullLockKey = LOCK_KEY_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();
        
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    fullLockKey, lockValue, expireTime.toMillis(), TimeUnit.MILLISECONDS);
            
            if (Boolean.TRUE.equals(success)) {
                log.debug("获取分布式锁成功 - key: {}, ttl: {}ms", fullLockKey, expireTime.toMillis());
                return true;
            }
            
        } catch (Exception e) {
            log.error("获取分布式锁失败 - key: {}", fullLockKey, e);
        }
        
        return false;
    }
    
    @Override
    public boolean releaseLock(String lockKey) {
        String fullLockKey = LOCK_KEY_PREFIX + lockKey;
        
        try {
            Boolean success = redisTemplate.delete(fullLockKey);
            log.debug("释放分布式锁 - key: {}, success: {}", fullLockKey, success);
            return Boolean.TRUE.equals(success);
            
        } catch (Exception e) {
            log.error("释放分布式锁失败 - key: {}", fullLockKey, e);
            return false;
        }
    }
    
    @Override
    public <T> T withLock(String lockKey, Duration expireTime, Supplier<T> operation) {
        if (acquireLock(lockKey, expireTime)) {
            try {
                return operation.get();
            } finally {
                releaseLock(lockKey);
            }
        }
        
        throw new RuntimeException("无法获取分布式锁: " + lockKey);
    }
    
    // ==================== 有序集合操作 ====================
    
    @Override
    public Set<String> zRange(String key, long start, long end) {
        String fullKey = buildFullKey(key);
        try {
            Set<Object> range = redisTemplate.opsForZSet().range(fullKey, start, end);
            return range != null ? range.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet()) : new HashSet<>();
        } catch (Exception e) {
            log.error("获取有序集合范围失败 - key: {}", fullKey, e);
            return new HashSet<>();
        }
    }
    
    @Override
    public Set<String> zRangeByScore(String key, double min, double max) {
        String fullKey = buildFullKey(key);
        try {
            Set<Object> range = redisTemplate.opsForZSet().rangeByScore(fullKey, min, max);
            return range != null ? range.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet()) : new HashSet<>();
        } catch (Exception e) {
            log.error("按分数获取有序集合失败 - key: {}", fullKey, e);
            return new HashSet<>();
        }
    }
    
    @Override
    public Boolean zAdd(String key, String value, double score) {
        String fullKey = buildFullKey(key);
        try {
            return redisTemplate.opsForZSet().add(fullKey, value, score);
        } catch (Exception e) {
            log.error("添加有序集合元素失败 - key: {}", fullKey, e);
            return false;
        }
    }
    
    @Override
    public Long zAdd(String key, Map<String, Double> scoreMembers) {
        String fullKey = buildFullKey(key);
        try {
            Set<ZSetOperations.TypedTuple<Object>> tuples = scoreMembers.entrySet().stream()
                    .map(entry -> ZSetOperations.TypedTuple.of((Object) entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet());
                    
            return redisTemplate.opsForZSet().add(fullKey, tuples);
        } catch (Exception e) {
            log.error("批量添加有序集合元素失败 - key: {}", fullKey, e);
            return 0L;
        }
    }
    
    @Override
    public Long zRemove(String key, Object... values) {
        String fullKey = buildFullKey(key);
        try {
            return redisTemplate.opsForZSet().remove(fullKey, values);
        } catch (Exception e) {
            log.error("移除有序集合元素失败 - key: {}", fullKey, e);
            return 0L;
        }
    }
    
    @Override
    public Long zCard(String key) {
        String fullKey = buildFullKey(key);
        try {
            return redisTemplate.opsForZSet().zCard(fullKey);
        } catch (Exception e) {
            log.error("获取有序集合大小失败 - key: {}", fullKey, e);
            return 0L;
        }
    }
    
    @Override
    public Double zScore(String key, String value) {
        String fullKey = buildFullKey(key);
        try {
            return redisTemplate.opsForZSet().score(fullKey, value);
        } catch (Exception e) {
            log.error("获取有序集合分数失败 - key: {}", fullKey, e);
            return null;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建完整缓存键
     */
    private String buildFullKey(String key) {
        return CACHE_KEY_PREFIX + key;
    }
    
    /**
     * 根据缓存类型构建缓存键
     */
    private String buildCacheTypeKey(String key, FileCacheType cacheType) {
        return cacheType.buildKey(key);
    }
    
    /**
     * 转换缓存值类型
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }
        
        if (clazz.isInstance(value)) {
            return (T) value;
        }
        
        try {
            // 使用Jackson进行类型转换
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            log.warn("缓存值类型转换失败 - value: {}, targetClass: {}", value, clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * 仅从Redis获取
     */
    private <T> T getFromRedis(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return convertValue(value, clazz);
        } catch (Exception e) {
            log.error("从Redis获取缓存失败 - key: {}", key, e);
            return null;
        }
    }
    
    /**
     * 仅从本地缓存获取
     */
    private <T> T getFromLocal(String key, Class<T> clazz) {
        try {
            CompletableFuture<Object> result = localCache.getIfPresent(key);
            if (result != null && !result.isCompletedExceptionally()) {
                return convertValue(result.join(), clazz);
            }
        } catch (Exception e) {
            log.error("从本地缓存获取失败 - key: {}", key, e);
        }
        return null;
    }
    
    /**
     * 记录响应时间
     */
    private void recordResponseTime(String key, long responseTime) {
        responseTimeStats.put(key, responseTime);
    }
    
    /**
     * 计算按类型的命中率
     */
    private Map<String, Double> calculateHitRateByType() {
        Map<String, Double> hitRateByType = new HashMap<>();
        
        for (FileCacheType cacheType : FileCacheType.values()) {
            String prefix = cacheType.getKeyPrefix();
            
            long typeHits = hitStats.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(prefix))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
                    
            long typeAccess = accessStats.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(prefix))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
                    
            double hitRate = typeAccess > 0 ? (double) typeHits / typeAccess : 0.0;
            hitRateByType.put(cacheType.name(), hitRate);
        }
        
        return hitRateByType;
    }
    
    /**
     * 获取按类型的请求次数
     */
    private Map<String, Long> getRequestCountByType() {
        Map<String, Long> requestCountByType = new HashMap<>();
        
        for (FileCacheType cacheType : FileCacheType.values()) {
            String prefix = cacheType.getKeyPrefix();
            
            long typeRequests = accessStats.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(prefix))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
                    
            requestCountByType.put(cacheType.name(), typeRequests);
        }
        
        return requestCountByType;
    }
    
    /**
     * 获取热门缓存键
     */
    private Map<String, Long> getTopHotKeys(int limit) {
        return accessStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    
    /**
     * 清理过期的统计数据
     */
    private long cleanupExpiredStats(Map<String, Long> statsMap, long expireTime) {
        long cleanedCount = 0;
        Iterator<Map.Entry<String, Long>> iterator = statsMap.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            // 这里简化处理，实际应该记录统计时间
            // 如果需要更精确的过期控制，建议使用时间戳作为值或单独记录时间
            iterator.remove();
            cleanedCount++;
        }
        
        return cleanedCount;
    }
}