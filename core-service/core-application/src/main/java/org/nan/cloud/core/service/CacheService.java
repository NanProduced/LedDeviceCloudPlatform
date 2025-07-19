package org.nan.cloud.core.service;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 统一缓存服务接口
 * 提供多级缓存抽象，支持本地缓存和分布式缓存
 */
public interface CacheService {
    
    /**
     * 获取缓存值
     * @param key 缓存键
     * @param clazz 值类型
     * @return 缓存值，不存在返回null
     */
    <T> T get(String key, Class<T> clazz);
    
    /**
     * 获取缓存值，支持默认值
     * @param key 缓存键
     * @param defaultValueSupplier 默认值提供者
     * @param clazz 值类型
     * @return 缓存值或默认值
     */
    <T> T get(String key, Supplier<T> defaultValueSupplier, Class<T> clazz);
    
    /**
     * 获取缓存值，支持TTL设置
     * @param key 缓存键
     * @param defaultValueSupplier 默认值提供者
     * @param ttl 过期时间
     * @param clazz 值类型
     * @return 缓存值或默认值
     */
    <T> T get(String key, Supplier<T> defaultValueSupplier, Duration ttl, Class<T> clazz);
    
    /**
     * 批量获取缓存值
     * @param keys 缓存键集合
     * @param clazz 值类型
     * @return key-value映射
     */
    <T> Map<String, T> multiGet(Collection<String> keys, Class<T> clazz);
    
    /**
     * 设置缓存值
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);
    
    /**
     * 设置缓存值，指定TTL
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    void put(String key, Object value, Duration ttl);
    
    /**
     * 批量设置缓存值
     * @param keyValueMap key-value映射
     * @param ttl 过期时间
     */
    void multiPut(Map<String, Object> keyValueMap, Duration ttl);
    
    /**
     * 删除缓存
     * @param key 缓存键
     */
    void evict(String key);
    
    /**
     * 批量删除缓存
     * @param keys 缓存键集合
     */
    void multiEvict(Collection<String> keys);
    
    /**
     * 按模式删除缓存
     * @param pattern 匹配模式，支持通配符
     */
    void evictByPattern(String pattern);
    
    /**
     * 检查缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    boolean hasKey(String key);
    
    /**
     * 异步获取缓存值
     * @param key 缓存键
     * @param defaultValueSupplier 默认值提供者
     * @param clazz 值类型
     * @return CompletableFuture包装的缓存值
     */
    <T> CompletableFuture<T> getAsync(String key, Supplier<T> defaultValueSupplier, Class<T> clazz);
    
    /**
     * 获取缓存统计信息
     * @return 缓存统计
     */
    CacheStatistics getStatistics();
}