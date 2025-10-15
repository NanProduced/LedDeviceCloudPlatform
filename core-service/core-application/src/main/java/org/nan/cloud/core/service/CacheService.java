package org.nan.cloud.core.service;

import org.nan.cloud.core.enums.CacheType;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
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
     * 使用cacheType配置获取缓存
     * @param key
     * @param cacheType
     * @param clazz
     * @return
     * @param <T>
     */
    <T> T getWithCacheTypeConfig(String key, CacheType cacheType, Class<T> clazz);
    
    /**
     * 设置缓存值
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 根据CacheType配置缓存
     * @param key
     * @param value
     * @param cacheType
     * @param customTtl
     */
    void putWithCacheTypeConfig(String key, Object value, CacheType cacheType, Duration customTtl);
    
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
     * 获取有序集合指定范围的元素及其分数
     * @param key 缓存键
     * @param start 开始位置
     * @param end 结束位置
     * @return 有序集合元素及分数的映射
     */
    Set<String> zRangeWithScores(String key, long start, long end);
    
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