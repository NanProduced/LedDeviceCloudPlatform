package org.nan.cloud.core.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.enums.CacheType;
import org.nan.cloud.core.service.CacheService;
import org.nan.cloud.core.service.CacheStatistics;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 缓存服务实现类
 * 提供多级缓存支持：L1本地缓存(Caffeine) + L2分布式缓存(Redis)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final AsyncCache<String, Object> localCache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheProperties cacheProperties;
    private final ObjectMapper objectMapper;

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return get(key, null, clazz);
    }

    @Override
    public <T> T get(String key, Supplier<T> defaultValueSupplier, Class<T> clazz) {
        return get(key, defaultValueSupplier, cacheProperties.getRedis().getDefaultTtl(), clazz);
    }

    @Override
    public <T> T get(String key, Supplier<T> defaultValueSupplier, Duration ttl, Class<T> clazz) {
        try {
            // 1. 尝试从本地缓存获取
            if (cacheProperties.getLocal().isEnabled()) {
                CompletableFuture<Object> localValue = localCache.getIfPresent(key);
                if (localValue != null) {
                    try {
                        Object value = localValue.get();
                        if (value != null) {
                            log.debug("Cache hit in local cache for key: {}", key);
                            return convertValue(value, clazz);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        log.warn("Error getting from local cache for key: {}", key, e);
                    }
                }
            }

            // 2. 尝试从Redis获取
            if (cacheProperties.getRedis().isEnabled()) {
                Object redisValue = redisTemplate.opsForValue().get(key);
                if (redisValue != null) {
                    log.debug("Cache hit in Redis for key: {}", key);
                    T value = convertValue(redisValue, clazz);
                    
                    // 回写到本地缓存
                    if (cacheProperties.getLocal().isEnabled()) {
                        localCache.put(key, CompletableFuture.completedFuture(value));
                    }
                    return value;
                }
            }

            // 3. 缓存未命中，使用默认值提供者
            if (defaultValueSupplier != null) {
                log.debug("Cache miss for key: {}, loading from supplier", key);
                T value = defaultValueSupplier.get();
                if (value != null) {
                    put(key, value, ttl);
                }
                return value;
            }

            log.debug("Cache miss for key: {}, no default supplier", key);
            return null;

        } catch (Exception e) {
            log.error("Error getting cache for key: {}", key, e);
            // 缓存异常时尝试使用默认值提供者
            return defaultValueSupplier != null ? defaultValueSupplier.get() : null;
        }
    }

    @Override
    public <T> Map<String, T> multiGet(Collection<String> keys, Class<T> clazz) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new HashMap<>();

        try {
            // 1. 批量从本地缓存获取
            if (cacheProperties.getLocal().isEnabled()) {
                // AsyncCache没有getAllPresent方法，需要逐个获取
                for (String key : keys) {
                    CompletableFuture<Object> localValue = localCache.getIfPresent(key);
                    if (localValue != null) {
                        try {
                            Object value = localValue.get();
                            if (value != null) {
                                T convertedValue = convertValue(value, clazz);
                                if (convertedValue != null) {
                                    result.put(key, convertedValue);
                                }
                            }
                        } catch (ExecutionException | InterruptedException e) {
                            log.warn("Error getting from local cache for key: {}", key, e);
                        }
                    }
                }
            }

            // 2. 获取本地缓存中未命中的键
            Collection<String> missedKeys = keys.stream()
                    .filter(key -> !result.containsKey(key))
                    .collect(Collectors.toList());

            // 3. 批量从Redis获取未命中的键
            if (!missedKeys.isEmpty() && cacheProperties.getRedis().isEnabled()) {
                List<Object> redisValues = redisTemplate.opsForValue().multiGet(missedKeys);
                if (redisValues != null) {
                    Iterator<String> keyIterator = missedKeys.iterator();
                    Iterator<Object> valueIterator = redisValues.iterator();
                    
                    while (keyIterator.hasNext() && valueIterator.hasNext()) {
                        String key = keyIterator.next();
                        Object redisValue = valueIterator.next();
                        
                        if (redisValue != null) {
                            T value = convertValue(redisValue, clazz);
                            if (value != null) {
                                result.put(key, value);
                                // 回写到本地缓存
                                if (cacheProperties.getLocal().isEnabled()) {
                                    localCache.put(key, CompletableFuture.completedFuture(value));
                                }
                            }
                        }
                    }
                }
            }

            log.debug("Multi get completed for {} keys, found {} values", keys.size(), result.size());
            return result;

        } catch (Exception e) {
            log.error("Error in multi get for keys: {}", keys, e);
            return result;
        }
    }

    @Override
    public <T> T getWithCacheTypeConfig(String key, CacheType cacheType, Class<T> clazz) {
        try {
            // 1. 尝试从本地缓存获取
            if (cacheProperties.getLocal().isEnabled() && cacheType.isUseLocalCache()) {
                CompletableFuture<Object> localValue = localCache.getIfPresent(key);
                if (localValue != null) {
                    try {
                        Object value = localValue.get();
                        if (value != null) {
                            log.debug("Cache with cacheType config hit in local cache for key: {}", key);
                            return convertValue(value, clazz);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        log.warn("Error getting from local cache for key: {}", key, e);
                        return null;
                    }
                }
            }

            // 2. 尝试从Redis获取
            if (cacheProperties.getRedis().isEnabled() && cacheType.isUseDistributedCache()) {
                Object redisValue = redisTemplate.opsForValue().get(key);
                if (redisValue != null) {
                    log.debug("Cache with cacheType config hit in Redis for key: {}", key);
                    T value = convertValue(redisValue, clazz);

                    // 回写到本地缓存
                    if (cacheProperties.getLocal().isEnabled()) {
                        localCache.put(key, CompletableFuture.completedFuture(value));
                    }
                    return value;
                }
            }
        } catch (Exception e) {
            log.error("Error getting cache for key: {}", key, e);
            return null;
        }
        return null;
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, cacheProperties.getRedis().getDefaultTtl());
    }

    @Override
    public void putWithCacheTypeConfig(String key, Object value, CacheType cacheType, Duration customTtl) {
        try {
            Duration ttl = cacheType.getDefaultTtl();
            if (Objects.nonNull(customTtl)) ttl = customTtl;
            // 1. 存储到本地缓存
            if (cacheProperties.getLocal().isEnabled() && cacheType.isUseLocalCache()) {
                localCache.put(key, CompletableFuture.completedFuture(value));
            }

            // 2. 存储到Redis
            if (cacheProperties.getRedis().isEnabled() && cacheType.isUseDistributedCache()) {
                if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                    redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
            }

            log.debug("Cache with CacheType config put for key: {} with TTL: {}", key, ttl);

        } catch (Exception e) {
            log.error("Error putting cache with CacheType config for key: {}", key, e);
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            // 1. 存储到本地缓存
            if (cacheProperties.getLocal().isEnabled()) {
                localCache.put(key, CompletableFuture.completedFuture(value));
            }

            // 2. 存储到Redis
            if (cacheProperties.getRedis().isEnabled()) {
                if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                    redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
            }

            log.debug("Cache put for key: {} with TTL: {}", key, ttl);

        } catch (Exception e) {
            log.error("Error putting cache for key: {}", key, e);
        }
    }

    @Override
    public void multiPut(Map<String, Object> keyValueMap, Duration ttl) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            return;
        }

        try {
            // 1. 批量存储到本地缓存
            if (cacheProperties.getLocal().isEnabled()) {
                // AsyncCache没有putAll方法，需要逐个存储
                for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
                    localCache.put(entry.getKey(), CompletableFuture.completedFuture(entry.getValue()));
                }
            }

            // 2. 批量存储到Redis
            if (cacheProperties.getRedis().isEnabled()) {
                if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                    // Redis没有直接的multiSet with TTL，需要逐个设置
                    for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
                        redisTemplate.opsForValue().set(entry.getKey(), entry.getValue(), 
                                ttl.toMillis(), TimeUnit.MILLISECONDS);
                    }
                } else {
                    redisTemplate.opsForValue().multiSet(keyValueMap);
                }
            }

            log.debug("Multi put completed for {} keys with TTL: {}", keyValueMap.size(), ttl);

        } catch (Exception e) {
            log.error("Error in multi put for keys: {}", keyValueMap.keySet(), e);
        }
    }

    @Override
    public void evict(String key) {
        try {
            // 1. 从本地缓存删除
            if (cacheProperties.getLocal().isEnabled()) {
                localCache.synchronous().invalidate(key);
            }

            // 2. 从Redis删除
            if (cacheProperties.getRedis().isEnabled()) {
                redisTemplate.delete(key);
            }

            log.debug("Cache evicted for key: {}", key);

        } catch (Exception e) {
            log.error("Error evicting cache for key: {}", key, e);
        }
    }

    @Override
    public void multiEvict(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        try {
            // 1. 批量从本地缓存删除
            if (cacheProperties.getLocal().isEnabled()) {
                localCache.synchronous().invalidateAll(keys);
            }

            // 2. 批量从Redis删除
            if (cacheProperties.getRedis().isEnabled()) {
                redisTemplate.delete(keys);
            }

            log.debug("Multi evict completed for {} keys", keys.size());

        } catch (Exception e) {
            log.error("Error in multi evict for keys: {}", keys, e);
        }
    }

    @Override
    public void evictByPattern(String pattern) {
        try {
            // 1. 本地缓存不支持模式删除，需要全部清空
            if (cacheProperties.getLocal().isEnabled()) {
                log.warn("Local cache does not support pattern eviction, invalidating all");
                localCache.synchronous().invalidateAll();
            }

            // 2. Redis支持模式删除
            if (cacheProperties.getRedis().isEnabled()) {
                redisTemplate.execute((RedisCallback<Object>) connection -> {
                    String luaScript = "local keys = redis.call('keys', ARGV[1]) " +
                        "if #keys > 0 then " +
                        "  return redis.call('del', unpack(keys)) " +
                        "else " +
                        "  return 0 " +
                        "end";
                    
                    return connection.eval(
                        luaScript.getBytes(), 
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        0, 
                        pattern.getBytes()
                    );
                });
            }

            log.debug("Cache evicted by pattern: {}", pattern);

        } catch (Exception e) {
            log.error("Error evicting cache by pattern: {}", pattern, e);
        }
    }

    @Override
    public boolean hasKey(String key) {
        try {
            // 1. 检查本地缓存
            if (cacheProperties.getLocal().isEnabled()) {
                CompletableFuture<Object> localValue = localCache.getIfPresent(key);
                if (localValue != null) {
                    return true;
                }
            }

            // 2. 检查Redis
            if (cacheProperties.getRedis().isEnabled()) {
                return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking key existence: {}", key, e);
            return false;
        }
    }

    @Override
    public <T> CompletableFuture<T> getAsync(String key, Supplier<T> defaultValueSupplier, Class<T> clazz) {
        return localCache.get(key, (k, executor) -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // 如果本地缓存未命中，尝试从Redis获取
                    if (cacheProperties.getRedis().isEnabled()) {
                        Object redisValue = redisTemplate.opsForValue().get(key);
                        if (redisValue != null) {
                            return redisValue;
                        }
                    }
                    
                    // 如果都未命中，使用默认值提供者
                    if (defaultValueSupplier != null) {
                        T value = defaultValueSupplier.get();
                        if (value != null && cacheProperties.getRedis().isEnabled()) {
                            // 异步写入Redis
                            CompletableFuture.runAsync(() -> 
                                redisTemplate.opsForValue().set(key, value, 
                                    cacheProperties.getRedis().getDefaultTtl().toMillis(), 
                                    TimeUnit.MILLISECONDS)
                            );
                        }
                        return value;
                    }
                    
                    return null;
                } catch (Exception e) {
                    log.error("Error loading value for key: {}", key, e);
                    return defaultValueSupplier != null ? defaultValueSupplier.get() : null;
                }
            }, executor);
        }).thenApply(value -> convertValue(value, clazz));
    }

    @Override
    public CacheStatistics getStatistics() {
        return new CacheStatisticsImpl(localCache.synchronous().stats());
    }

    @Override
    public Set<String> zRange(String key, long start, long end) {
        try {
            if (!cacheProperties.getRedis().isEnabled()) {
                log.warn("Redis is disabled, cannot perform zRange operation");
                return Collections.emptySet();
            }
            
            Set<String> result = stringRedisTemplate.opsForZSet().range(key, start, end);
            log.debug("ZSet range for key: {}, start: {}, end: {}, result size: {}", 
                      key, start, end, result != null ? result.size() : 0);
            return result != null ? result : Collections.emptySet();
            
        } catch (Exception e) {
            log.error("Error getting zRange for key: {}", key, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> zRangeByScore(String key, double min, double max) {
        try {
            if (!cacheProperties.getRedis().isEnabled()) {
                log.warn("Redis is disabled, cannot perform zRangeByScore operation");
                return Collections.emptySet();
            }
            
            Set<String> result = stringRedisTemplate.opsForZSet().rangeByScore(key, min, max);
            log.debug("ZSet rangeByScore for key: {}, min: {}, max: {}, result size: {}", 
                      key, min, max, result != null ? result.size() : 0);
            return result != null ? result : Collections.emptySet();
            
        } catch (Exception e) {
            log.error("Error getting zRangeByScore for key: {}", key, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> zRangeWithScores(String key, long start, long end) {
        try {
            if (!cacheProperties.getRedis().isEnabled()) {
                log.warn("Redis is disabled, cannot perform zRangeWithScores operation");
                return Collections.emptySet();
            }
            
            // 获取带分数的结果，然后转换为字符串集合
            Set<String> result = stringRedisTemplate.opsForZSet().range(key, start, end);
            log.debug("ZSet rangeWithScores for key: {}, start: {}, end: {}, result size: {}", 
                      key, start, end, result != null ? result.size() : 0);
            return result != null ? result : Collections.emptySet();
            
        } catch (Exception e) {
            log.error("Error getting zRangeWithScores for key: {}", key, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Boolean zAdd(String key, String value, double score) {
        try {
            if (!cacheProperties.getRedis().isEnabled()) {
                log.warn("Redis is disabled, cannot perform zAdd operation");
                return false;
            }
            
            Boolean result = stringRedisTemplate.opsForZSet().add(key, value, score);
            log.debug("ZSet add for key: {}, value: {}, score: {}, result: {}", 
                      key, value, score, result);
            return result != null ? result : false;
            
        } catch (Exception e) {
            log.error("Error adding to zSet for key: {}", key, e);
            return false;
        }
    }

    @Override
    public Long zAdd(String key, Map<String, Double> scoreMembers) {
        try {
            if (!cacheProperties.getRedis().isEnabled()) {
                log.warn("Redis is disabled, cannot perform zAdd operation");
                return 0L;
            }
            
            if (scoreMembers == null || scoreMembers.isEmpty()) {
                return 0L;
            }
            
            // 转换为ZSetOperations.TypedTuple集合
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples = 
                scoreMembers.entrySet().stream()
                    .map(entry -> org.springframework.data.redis.core.ZSetOperations.TypedTuple
                        .of(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet());
            
            Long result = stringRedisTemplate.opsForZSet().add(key, tuples);
            log.debug("ZSet batch add for key: {}, members count: {}, result: {}", 
                      key, scoreMembers.size(), result);
            return result != null ? result : 0L;
            
        } catch (Exception e) {
            log.error("Error batch adding to zSet for key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Long zRemove(String key, Object... values) {
        try {
            if (!cacheProperties.getRedis().isEnabled()) {
                log.warn("Redis is disabled, cannot perform zRemove operation");
                return 0L;
            }
            
            if (values == null || values.length == 0) {
                return 0L;
            }
            
            Long result = stringRedisTemplate.opsForZSet().remove(key, values);
            log.debug("ZSet remove for key: {}, values count: {}, result: {}", 
                      key, values.length, result);
            return result != null ? result : 0L;
            
        } catch (Exception e) {
            log.error("Error removing from zSet for key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Long zCard(String key) {
        try {
            if (!cacheProperties.getRedis().isEnabled()) {
                log.warn("Redis is disabled, cannot perform zCard operation");
                return 0L;
            }
            
            Long result = stringRedisTemplate.opsForZSet().zCard(key);
            log.debug("ZSet card for key: {}, result: {}", key, result);
            return result != null ? result : 0L;
            
        } catch (Exception e) {
            log.error("Error getting zCard for key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Double zScore(String key, String value) {
        try {
            if (!cacheProperties.getRedis().isEnabled()) {
                log.warn("Redis is disabled, cannot perform zScore operation");
                return null;
            }
            
            Double result = stringRedisTemplate.opsForZSet().score(key, value);
            log.debug("ZSet score for key: {}, value: {}, result: {}", key, value, result);
            return result;
            
        } catch (Exception e) {
            log.error("Error getting zScore for key: {}", key, e);
            return null;
        }
    }

    /**
     * 类型转换工具方法
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
            // 使用ObjectMapper进行类型转换
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            log.error("Error converting value to class {}: {}", clazz.getName(), e.getMessage());
            return null;
        }
    }
}