package org.nan.cloud.terminal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static org.nan.cloud.terminal.config.database.RedisConfig.RedisKeys.*;

/**
 * Redis缓存服务
 * 
 * 统一管理Redis缓存操作，支持两种数据类型：
 * 1. StringRedisTemplate: 存储简单字符串、计数器等
 * 2. RedisTemplate<String, Object>: 存储Java对象
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存设备状态对象
     * 
     * @param deviceId 设备ID
     * @param status 状态对象
     * @param ttlSeconds TTL(秒)
     */
    public void cacheDeviceStatus(String deviceId, Object status, long ttlSeconds) {
        try {
            String key = DEVICE_STATUS_PREFIX + deviceId;
            redisTemplate.opsForValue().set(key, status, ttlSeconds, TimeUnit.SECONDS);
            log.debug("设备状态已缓存: deviceId={}, ttl={}s", deviceId, ttlSeconds);
        } catch (Exception e) {
            log.error("缓存设备状态失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 获取缓存的设备状态对象
     * 
     * @param deviceId 设备ID
     * @param statusClass 状态类型
     * @return 设备状态对象，不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getDeviceStatus(String deviceId, Class<T> statusClass) {
        try {
            String key = DEVICE_STATUS_PREFIX + deviceId;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null && statusClass.isInstance(cached)) {
                return (T) cached;
            }
            return null;
        } catch (Exception e) {
            log.error("获取设备状态缓存失败: deviceId={}", deviceId, e);
            return null;
        }
    }

    /**
     * 缓存配置对象
     * 
     * @param configKey 配置键
     * @param configValue 配置对象
     * @param ttlSeconds TTL(秒)
     */
    public void cacheConfig(String configKey, Object configValue, long ttlSeconds) {
        try {
            String key = CONFIG_CACHE_PREFIX + configKey;
            redisTemplate.opsForValue().set(key, configValue, ttlSeconds, TimeUnit.SECONDS);
            log.debug("配置已缓存: key={}, ttl={}s", configKey, ttlSeconds);
        } catch (Exception e) {
            log.error("缓存配置失败: key={}", configKey, e);
        }
    }

    /**
     * 获取缓存的配置对象
     * 
     * @param configKey 配置键
     * @param valueClass 值类型
     * @return 配置对象，不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(String configKey, Class<T> valueClass) {
        try {
            String key = CONFIG_CACHE_PREFIX + configKey;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null && valueClass.isInstance(cached)) {
                return (T) cached;
            }
            return null;
        } catch (Exception e) {
            log.error("获取配置缓存失败: key={}", configKey, e);
            return null;
        }
    }

    /**
     * 设置设备在线状态（使用StringRedisTemplate存储时间戳）
     * 
     * @param deviceId 设备ID
     * @param ttlSeconds TTL(秒)
     */
    public void setDeviceOnline(String deviceId, long ttlSeconds) {
        try {
            String key = DEVICE_ONLINE_PREFIX + deviceId;
            String timestamp = String.valueOf(System.currentTimeMillis());
            stringRedisTemplate.opsForValue().set(key, timestamp, ttlSeconds, TimeUnit.SECONDS);
            log.debug("设备在线状态已设置: deviceId={}, ttl={}s", deviceId, ttlSeconds);
        } catch (Exception e) {
            log.error("设置设备在线状态失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 检查设备是否在线
     * 
     * @param deviceId 设备ID
     * @return 是否在线
     */
    public boolean isDeviceOnline(String deviceId) {
        try {
            String key = DEVICE_ONLINE_PREFIX + deviceId;
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查设备在线状态失败: deviceId={}", deviceId, e);
            return false;
        }
    }

    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    public void deleteCache(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("缓存已删除: key={}", key);
        } catch (Exception e) {
            log.error("删除缓存失败: key={}", key, e);
        }
    }
}