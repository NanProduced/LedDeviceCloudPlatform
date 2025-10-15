package org.nan.cloud.file.application.service;

import org.nan.cloud.file.application.enums.FileCacheType;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * File Service统一缓存服务接口
 * 参照core-service标准，提供多级缓存抽象，支持本地缓存和分布式缓存
 * 
 * 设计原则：
 * 1. 统一缓存键前缀策略
 * 2. Redis + Caffeine双层缓存
 * 3. TTL策略配置化
 * 4. 批量操作优化
 * 5. 异步访问支持
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface CacheService {
    
    // ==================== 基础缓存操作 ====================
    
    /**
     * 获取缓存值
     * @param key 缓存键
     * @param clazz 值类型
     * @return 缓存值，不存在返回null
     */
    <T> T get(String key, Class<T> clazz);
    
    /**
     * 获取缓存值，支持默认值提供者
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
     * 使用缓存类型配置获取缓存
     * @param key 缓存键
     * @param cacheType 缓存类型
     * @param clazz 值类型
     * @return 缓存值
     */
    <T> T getWithCacheTypeConfig(String key, FileCacheType cacheType, Class<T> clazz);
    
    // ==================== 缓存存储操作 ====================
    
    /**
     * 设置缓存值
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 根据缓存类型配置缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param cacheType 缓存类型
     * @param customTtl 自定义TTL（可选）
     */
    void putWithCacheTypeConfig(String key, Object value, FileCacheType cacheType, Duration customTtl);
    
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
    
    // ==================== 缓存删除操作 ====================
    
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
    
    // ==================== 异步操作 ====================
    
    /**
     * 异步获取缓存值
     * @param key 缓存键
     * @param defaultValueSupplier 默认值提供者
     * @param clazz 值类型
     * @return CompletableFuture包装的缓存值
     */
    <T> CompletableFuture<T> getAsync(String key, Supplier<T> defaultValueSupplier, Class<T> clazz);
    
    /**
     * 异步设置缓存值
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> putAsync(String key, Object value, Duration ttl);
    
    // ==================== 缓存统计与监控 ====================
    
    /**
     * 获取缓存统计信息
     * @return 缓存统计
     */
    CacheStatistics getStatistics();
    
    /**
     * 清理过期缓存
     * @return 清理的缓存条目数
     */
    long cleanupExpiredCache();
    
    // ==================== 分布式锁支持 ====================
    
    /**
     * 获取分布式锁
     * @param lockKey 锁键
     * @param expireTime 锁过期时间
     * @return 是否获取成功
     */
    boolean acquireLock(String lockKey, Duration expireTime);
    
    /**
     * 释放分布式锁
     * @param lockKey 锁键
     * @return 是否释放成功
     */
    boolean releaseLock(String lockKey);
    
    /**
     * 带锁执行操作
     * @param lockKey 锁键
     * @param expireTime 锁过期时间
     * @param operation 操作
     * @return 操作结果
     */
    <T> T withLock(String lockKey, Duration expireTime, Supplier<T> operation);
    
    // ==================== 有序集合操作（用于排行榜等场景）====================
    
    /**
     * 获取有序集合指定范围的元素
     * @param key 缓存键
     * @param start 开始位置
     * @param end 结束位置
     * @return 有序集合元素列表
     */
    Set<String> zRange(String key, long start, long end);
    
    /**
     * 获取有序集合指定分数范围的元素
     * @param key 缓存键
     * @param min 最小分数
     * @param max 最大分数
     * @return 有序集合元素列表
     */
    Set<String> zRangeByScore(String key, double min, double max);
    
    /**
     * 添加元素到有序集合
     * @param key 缓存键
     * @param value 元素值
     * @param score 分数
     * @return 是否成功添加
     */
    Boolean zAdd(String key, String value, double score);
    
    /**
     * 批量添加元素到有序集合
     * @param key 缓存键
     * @param scoreMembers 分数和元素的映射
     * @return 添加成功的元素数量
     */
    Long zAdd(String key, Map<String, Double> scoreMembers);
    
    /**
     * 从有序集合中移除元素
     * @param key 缓存键
     * @param values 要移除的元素
     * @return 移除的元素数量
     */
    Long zRemove(String key, Object... values);
    
    /**
     * 获取有序集合的元素数量
     * @param key 缓存键
     * @return 元素数量
     */
    Long zCard(String key);
    
    /**
     * 获取指定元素在有序集合中的分数
     * @param key 缓存键
     * @param value 元素值
     * @return 分数
     */
    Double zScore(String key, String value);
}