package org.nan.cloud.message.domain.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 消息缓存领域服务接口
 * 
 * 定义消息系统相关的缓存操作领域接口，application层依赖此抽象接口，
 * infrastructure层提供具体实现，遵循DDD架构的依赖倒置原则。
 * 
 * 主要功能：
 * - 用户在线状态管理
 * - WebSocket会话管理
 * - 消息状态缓存
 * - 未读消息管理
 * - 消息去重处理
 * 
 * @author Nan
 * @since 1.0.0
 */
public interface MessageCacheService {
    
    // ==================== 用户在线状态管理 ====================
    
    /**
     * 标记用户在线
     * 
     * @param userId 用户ID
     * @param expireSeconds 过期时间（秒）
     */
    void markUserOnline(String userId, int expireSeconds);
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean isUserOnline(String userId);
    
    /**
     * 标记用户离线
     * 
     * @param userId 用户ID
     */
    void markUserOffline(String userId);
    
    // ==================== WebSocket会话管理 ====================
    
    /**
     * 注册WebSocket会话
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param instanceId 服务实例ID
     * @param expireSeconds 过期时间（秒）
     */
    void registerWebSocketSession(String userId, String sessionId, String instanceId, int expireSeconds);
    
    /**
     * 获取会话所在的服务实例
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 服务实例ID
     */
    String getSessionInstance(String userId, String sessionId);
    
    /**
     * 移除WebSocket会话
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void removeWebSocketSession(String userId, String sessionId);
    
    /**
     * 获取用户的所有活跃会话
     * 
     * @param userId 用户ID
     * @return 会话ID和实例ID的映射
     */
    Map<Object, Object> getUserActiveSessions(String userId);
    
    // ==================== 消息状态缓存 ====================
    
    /**
     * 缓存消息状态
     * 
     * @param messageId 消息ID
     * @param status 消息状态
     * @param expireHours 过期时间（小时）
     */
    void cacheMessageStatus(String messageId, String status, int expireHours);
    
    /**
     * 获取消息状态
     * 
     * @param messageId 消息ID
     * @return 消息状态
     */
    String getMessageStatus(String messageId);
    
    /**
     * 批量获取消息状态
     * 
     * @param messageIds 消息ID列表
     * @return 消息ID到状态的映射
     */
    Map<String, String> batchGetMessageStatus(List<String> messageIds);
    
    // ==================== 未读消息管理 ====================
    
    /**
     * 增加用户未读消息计数
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param messageId 消息ID
     * @return 当前未读消息数
     */
    Long incrementUnreadCount(String userId, String organizationId, String messageId);
    
    /**
     * 获取用户未读消息数
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 未读消息数
     */
    Long getUnreadCount(String userId, String organizationId);
    
    /**
     * 批量标记消息为已读
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param messageIds 消息ID列表
     */
    void markMessagesAsRead(String userId, String organizationId, List<String> messageIds);
    
    /**
     * 获取用户最近的未读消息
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近的未读消息ID列表
     */
    Set<String> getRecentUnreadMessages(String userId, int limit);
    
    // ==================== 消息去重处理 ====================
    
    /**
     * 检查消息是否已处理
     * 
     * @param messageId 消息ID
     * @return 是否已处理
     */
    boolean isMessageProcessed(String messageId);
    
    /**
     * 标记消息为已处理
     * 
     * @param messageId 消息ID
     * @param expireHours 过期时间（小时）
     */
    void markMessageProcessed(String messageId, int expireHours);
    
    // ==================== 任务会话映射 ====================
    
    /**
     * 记录任务与会话的关系
     * 
     * @param taskId 任务ID
     * @param sessionId 会话ID
     * @param expireHours 过期时间（小时）
     */
    void recordTaskSession(String taskId, String sessionId, int expireHours);
    
    /**
     * 获取任务对应的会话ID
     * 
     * @param taskId 任务ID
     * @return 会话ID
     */
    String getTaskSession(String taskId);
    
    /**
     * 移除任务会话映射
     * 
     * @param taskId 任务ID
     */
    void removeTaskSession(String taskId);
}