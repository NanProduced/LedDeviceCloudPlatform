package org.nan.cloud.message.infrastructure.redis.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 消息缓存管理器
 * 
 * 提供消息系统相关的Redis缓存操作，支持用户在线状态、会话管理、
 * 消息状态缓存、未读消息统计等功能。
 * 
 * 主要功能：
 * - 用户在线状态管理
 * - WebSocket会话映射
 * - 消息状态缓存
 * - 未读消息管理
 * - 消息去重处理
 * - 设备控制锁管理
 * - 模板缓存管理
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCacheManager {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // ==================== 用户在线状态管理 ====================
    
    /**
     * 标记用户在线
     * 
     * @param userId 用户ID
     * @param expireSeconds 过期时间（秒）
     */
    public void markUserOnline(String userId, int expireSeconds) {
        try {
            String key = getUserOnlineKey(userId);
            redisTemplate.opsForValue().set(key, "true", expireSeconds, TimeUnit.SECONDS);
            log.debug("用户标记为在线: userId={}, expireSeconds={}", userId, expireSeconds);
        } catch (Exception e) {
            log.error("标记用户在线失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(String userId) {
        try {
            String key = getUserOnlineKey(userId);
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("检查用户在线状态失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 标记用户离线
     * 
     * @param userId 用户ID
     */
    public void markUserOffline(String userId) {
        try {
            String key = getUserOnlineKey(userId);
            redisTemplate.delete(key);
            log.debug("用户标记为离线: userId={}", userId);
        } catch (Exception e) {
            log.error("标记用户离线失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    // ==================== WebSocket会话管理 ====================
    
    /**
     * 注册WebSocket会话
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param instanceId 服务实例ID
     * @param expireSeconds 过期时间（秒）
     */
    public void registerWebSocketSession(String userId, String sessionId, String instanceId, int expireSeconds) {
        try {
            String sessionKey = getSessionInstanceKey(userId, sessionId);
            redisTemplate.opsForValue().set(sessionKey, instanceId, expireSeconds, TimeUnit.SECONDS);
            
            // 维护用户的所有会话列表
            String userSessionsKey = getUserSessionsKey(userId);
            redisTemplate.opsForHash().put(userSessionsKey, sessionId, instanceId);
            redisTemplate.expire(userSessionsKey, Duration.ofSeconds(expireSeconds));
            
            log.debug("WebSocket会话注册成功: userId={}, sessionId={}, instanceId={}", 
                     userId, sessionId, instanceId);
        } catch (Exception e) {
            log.error("WebSocket会话注册失败: userId={}, sessionId={}, error={}", 
                     userId, sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取会话所在的服务实例
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 服务实例ID
     */
    public String getSessionInstance(String userId, String sessionId) {
        try {
            String sessionKey = getSessionInstanceKey(userId, sessionId);
            return redisTemplate.opsForValue().get(sessionKey);
        } catch (Exception e) {
            log.error("获取会话实例失败: userId={}, sessionId={}, error={}", 
                     userId, sessionId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 移除WebSocket会话
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void removeWebSocketSession(String userId, String sessionId) {
        try {
            String sessionKey = getSessionInstanceKey(userId, sessionId);
            redisTemplate.delete(sessionKey);
            
            // 从用户会话列表中移除
            String userSessionsKey = getUserSessionsKey(userId);
            redisTemplate.opsForHash().delete(userSessionsKey, sessionId);
            
            log.debug("WebSocket会话移除成功: userId={}, sessionId={}", userId, sessionId);
        } catch (Exception e) {
            log.error("WebSocket会话移除失败: userId={}, sessionId={}, error={}", 
                     userId, sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户的所有活跃会话
     * 
     * @param userId 用户ID
     * @return 会话ID和实例ID的映射
     */
    public Map<Object, Object> getUserActiveSessions(String userId) {
        try {
            String userSessionsKey = getUserSessionsKey(userId);
            return redisTemplate.opsForHash().entries(userSessionsKey);
        } catch (Exception e) {
            log.error("获取用户活跃会话失败: userId={}, error={}", userId, e.getMessage(), e);
            return Map.of();
        }
    }
    
    // ==================== 消息状态缓存 ====================
    
    /**
     * 缓存消息状态
     * 
     * @param messageId 消息ID
     * @param status 消息状态
     * @param expireHours 过期时间（小时）
     */
    public void cacheMessageStatus(String messageId, String status, int expireHours) {
        try {
            String key = getMessageStatusKey(messageId);
            redisTemplate.opsForHash().put(key, "status", status);
            redisTemplate.opsForHash().put(key, "timestamp", String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(key, Duration.ofHours(expireHours));
            
            log.debug("消息状态缓存成功: messageId={}, status={}", messageId, status);
        } catch (Exception e) {
            log.error("消息状态缓存失败: messageId={}, status={}, error={}", 
                     messageId, status, e.getMessage(), e);
        }
    }
    
    /**
     * 获取消息状态
     * 
     * @param messageId 消息ID
     * @return 消息状态
     */
    public String getMessageStatus(String messageId) {
        try {
            String key = getMessageStatusKey(messageId);
            return (String) redisTemplate.opsForHash().get(key, "status");
        } catch (Exception e) {
            log.error("获取消息状态失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 批量获取消息状态
     * 
     * @param messageIds 消息ID列表
     * @return 消息ID到状态的映射
     */
    public Map<String, String> batchGetMessageStatus(List<String> messageIds) {
        try {
            Map<String, String> statusMap = new java.util.HashMap<>();
            
            for (String messageId : messageIds) {
                String status = getMessageStatus(messageId);
                if (status != null) {
                    statusMap.put(messageId, status);
                }
            }
            
            return statusMap;
        } catch (Exception e) {
            log.error("批量获取消息状态失败: messageIds={}, error={}", messageIds, e.getMessage(), e);
            return Map.of();
        }
    }
    
    // ==================== 未读消息管理 ====================
    
    /**
     * 增加用户未读消息计数
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param messageId 消息ID
     * @return 当前未读消息数
     */
    public Long incrementUnreadCount(String userId, String organizationId, String messageId) {
        try {
            String countKey = getUnreadCountKey(userId, organizationId);
            String messagesKey = getUnreadMessagesKey(userId);
            
            // 增加计数
            Long count = redisTemplate.opsForValue().increment(countKey);
            
            // 添加到未读消息集合（使用时间戳作为分数）
            long timestamp = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(messagesKey, messageId, timestamp);
            
            // 设置过期时间
            redisTemplate.expire(countKey, Duration.ofDays(7));
            redisTemplate.expire(messagesKey, Duration.ofDays(7));
            
            log.debug("未读消息计数递增: userId={}, organizationId={}, count={}", 
                     userId, organizationId, count);
            return count;
        } catch (Exception e) {
            log.error("未读消息计数递增失败: userId={}, organizationId={}, error={}", 
                     userId, organizationId, e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 获取用户未读消息数
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 未读消息数
     */
    public Long getUnreadCount(String userId, String organizationId) {
        try {
            String countKey = getUnreadCountKey(userId, organizationId);
            String countStr = redisTemplate.opsForValue().get(countKey);
            return countStr != null ? Long.parseLong(countStr) : 0L;
        } catch (Exception e) {
            log.error("获取未读消息数失败: userId={}, organizationId={}, error={}", 
                     userId, organizationId, e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 批量标记消息为已读
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param messageIds 消息ID列表
     */
    public void markMessagesAsRead(String userId, String organizationId, List<String> messageIds) {
        try {
            String countKey = getUnreadCountKey(userId, organizationId);
            String messagesKey = getUnreadMessagesKey(userId);
            
            // 从未读消息集合中移除
            for (String messageId : messageIds) {
                redisTemplate.opsForZSet().remove(messagesKey, messageId);
            }
            
            // 减少未读计数
            Long currentCount = getUnreadCount(userId, organizationId);
            Long newCount = Math.max(0, currentCount - messageIds.size());
            
            if (newCount == 0) {
                redisTemplate.delete(countKey);
            } else {
                redisTemplate.opsForValue().set(countKey, String.valueOf(newCount), 
                                               Duration.ofDays(7));
            }
            
            log.debug("批量标记消息已读: userId={}, organizationId={}, messageCount={}", 
                     userId, organizationId, messageIds.size());
        } catch (Exception e) {
            log.error("批量标记消息已读失败: userId={}, organizationId={}, error={}", 
                     userId, organizationId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户最近的未读消息
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近的未读消息ID列表
     */
    public Set<String> getRecentUnreadMessages(String userId, int limit) {
        try {
            String messagesKey = getUnreadMessagesKey(userId);
            // 按分数（时间戳）倒序获取最近的消息
            return redisTemplate.opsForZSet().reverseRange(messagesKey, 0, limit - 1);
        } catch (Exception e) {
            log.error("获取最近未读消息失败: userId={}, limit={}, error={}", 
                     userId, limit, e.getMessage(), e);
            return Set.of();
        }
    }
    
    // ==================== 消息去重处理 ====================
    
    /**
     * 检查消息是否已处理
     * 
     * @param messageId 消息ID
     * @return 是否已处理
     */
    public boolean isMessageProcessed(String messageId) {
        try {
            String key = getProcessedMessageKey(messageId);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查消息处理状态失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 标记消息为已处理
     * 
     * @param messageId 消息ID
     * @param expireHours 过期时间（小时）
     */
    public void markMessageProcessed(String messageId, int expireHours) {
        try {
            String key = getProcessedMessageKey(messageId);
            redisTemplate.opsForValue().set(key, "1", expireHours, TimeUnit.HOURS);
            log.debug("消息标记为已处理: messageId={}, expireHours={}", messageId, expireHours);
        } catch (Exception e) {
            log.error("标记消息已处理失败: messageId={}, error={}", messageId, e.getMessage(), e);
        }
    }
    
    // ==================== 任务会话映射 ====================
    
    /**
     * 记录任务与会话的关系
     * 
     * @param taskId 任务ID
     * @param sessionId 会话ID
     * @param expireHours 过期时间（小时）
     */
    public void recordTaskSession(String taskId, String sessionId, int expireHours) {
        try {
            String key = getTaskSessionKey(taskId);
            redisTemplate.opsForValue().set(key, sessionId, expireHours, TimeUnit.HOURS);
            log.debug("任务会话关系记录成功: taskId={}, sessionId={}", taskId, sessionId);
        } catch (Exception e) {
            log.error("任务会话关系记录失败: taskId={}, sessionId={}, error={}", 
                     taskId, sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取任务对应的会话ID
     * 
     * @param taskId 任务ID
     * @return 会话ID
     */
    public String getTaskSession(String taskId) {
        try {
            String key = getTaskSessionKey(taskId);
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取任务会话失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 移除任务会话映射
     * 
     * @param taskId 任务ID
     */
    public void removeTaskSession(String taskId) {
        try {
            String key = getTaskSessionKey(taskId);
            redisTemplate.delete(key);
            log.debug("任务会话关系移除成功: taskId={}", taskId);
        } catch (Exception e) {
            log.error("任务会话关系移除失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }
    
    // ==================== 私有工具方法 ====================
    
    private String getUserOnlineKey(String userId) {
        return "user_online:" + userId;
    }
    
    private String getSessionInstanceKey(String userId, String sessionId) {
        return "websocket_instance:" + userId + ":" + sessionId;
    }
    
    private String getUserSessionsKey(String userId) {
        return "user_sessions:" + userId;
    }
    
    private String getMessageStatusKey(String messageId) {
        return "message_status:" + messageId;
    }
    
    private String getUnreadCountKey(String userId, String organizationId) {
        return "unread_count:" + userId + ":" + organizationId;
    }
    
    private String getUnreadMessagesKey(String userId) {
        return "unread_messages:" + userId;
    }
    
    private String getProcessedMessageKey(String messageId) {
        return "processed_message:" + messageId;
    }
    
    private String getTaskSessionKey(String taskId) {
        return "task_session_mapping:" + taskId;
    }
    
    // ==================== 通用缓存操作 ====================
    
    /**
     * 缓存数据
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param expireSeconds 过期时间（秒）
     */
    public void cacheData(String key, String value, int expireSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
            log.debug("数据缓存成功: key={}", key);
        } catch (Exception e) {
            log.error("数据缓存失败: key={}, error={}", key, e.getMessage(), e);
        }
    }
    
    /**
     * 获取缓存数据
     * 
     * @param key 缓存键
     * @return 缓存值，不存在时返回null
     */
    public String getCachedData(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取缓存数据失败: key={}, error={}", key, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 递增计数器
     * 
     * @param key 计数器键
     * @param increment 递增值
     * @param expireSeconds 过期时间（秒）
     * @return 递增后的值
     */
    public Long incrementCounter(String key, long increment, int expireSeconds) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, increment);
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
            log.debug("计数器递增成功: key={}, value={}", key, value);
            return value;
        } catch (Exception e) {
            log.error("计数器递增失败: key={}, error={}", key, e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 获取计数器值
     * 
     * @param key 计数器键
     * @return 计数器值，不存在时返回0
     */
    public Long getCounter(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            log.error("获取计数器值失败: key={}, error={}", key, e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 删除缓存数据
     * 
     * @param key 缓存键
     * @return 删除是否成功
     */
    public Boolean deleteData(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除缓存数据失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 批量删除缓存数据
     * 
     * @param keys 缓存键列表
     * @return 删除的数量
     */
    public Long batchDeleteData(List<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("批量删除缓存数据失败: keys={}, error={}", keys.size(), e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 检查缓存键是否存在
     * 
     * @param key 缓存键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("检查缓存键失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 设置缓存过期时间
     * 
     * @param key 缓存键
     * @param expireSeconds 过期时间（秒）
     * @return 设置是否成功
     */
    public Boolean expire(String key, int expireSeconds) {
        try {
            return redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("设置过期时间失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
}