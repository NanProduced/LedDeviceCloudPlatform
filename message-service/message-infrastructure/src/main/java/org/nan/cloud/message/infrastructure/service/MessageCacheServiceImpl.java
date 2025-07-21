package org.nan.cloud.message.infrastructure.service;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.message.domain.service.MessageCacheService;
import org.nan.cloud.message.infrastructure.redis.manager.MessageCacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 消息缓存服务实现类
 * 
 * 实现MessageCacheService领域接口，将领域层的缓存操作委托给
 * infrastructure层的具体实现，遵循DDD架构的依赖倒置原则。
 * 
 * 这是一个适配器模式的应用，将infrastructure层的具体实现
 * 适配为domain层定义的接口。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MessageCacheServiceImpl implements MessageCacheService {
    
    private final MessageCacheManager messageCacheManager;
    
    @Override
    public void markUserOnline(String userId, int expireSeconds) {
        messageCacheManager.markUserOnline(userId, expireSeconds);
    }
    
    @Override
    public boolean isUserOnline(String userId) {
        return messageCacheManager.isUserOnline(userId);
    }
    
    @Override
    public void markUserOffline(String userId) {
        messageCacheManager.markUserOffline(userId);
    }
    
    @Override
    public void registerWebSocketSession(String userId, String sessionId, String instanceId, int expireSeconds) {
        messageCacheManager.registerWebSocketSession(userId, sessionId, instanceId, expireSeconds);
    }
    
    @Override
    public String getSessionInstance(String userId, String sessionId) {
        return messageCacheManager.getSessionInstance(userId, sessionId);
    }
    
    @Override
    public void removeWebSocketSession(String userId, String sessionId) {
        messageCacheManager.removeWebSocketSession(userId, sessionId);
    }
    
    @Override
    public Map<Object, Object> getUserActiveSessions(String userId) {
        return messageCacheManager.getUserActiveSessions(userId);
    }
    
    @Override
    public void cacheMessageStatus(String messageId, String status, int expireHours) {
        messageCacheManager.cacheMessageStatus(messageId, status, expireHours);
    }
    
    @Override
    public String getMessageStatus(String messageId) {
        return messageCacheManager.getMessageStatus(messageId);
    }
    
    @Override
    public Map<String, String> batchGetMessageStatus(List<String> messageIds) {
        return messageCacheManager.batchGetMessageStatus(messageIds);
    }
    
    @Override
    public Long incrementUnreadCount(String userId, String organizationId, String messageId) {
        return messageCacheManager.incrementUnreadCount(userId, organizationId, messageId);
    }
    
    @Override
    public Long getUnreadCount(String userId, String organizationId) {
        return messageCacheManager.getUnreadCount(userId, organizationId);
    }
    
    @Override
    public void markMessagesAsRead(String userId, String organizationId, List<String> messageIds) {
        messageCacheManager.markMessagesAsRead(userId, organizationId, messageIds);
    }
    
    @Override
    public Set<String> getRecentUnreadMessages(String userId, int limit) {
        return messageCacheManager.getRecentUnreadMessages(userId, limit);
    }
    
    @Override
    public boolean isMessageProcessed(String messageId) {
        return messageCacheManager.isMessageProcessed(messageId);
    }
    
    @Override
    public void markMessageProcessed(String messageId, int expireHours) {
        messageCacheManager.markMessageProcessed(messageId, expireHours);
    }
    
    @Override
    public void recordTaskSession(String taskId, String sessionId, int expireHours) {
        messageCacheManager.recordTaskSession(taskId, sessionId, expireHours);
    }
    
    @Override
    public String getTaskSession(String taskId) {
        return messageCacheManager.getTaskSession(taskId);
    }
    
    @Override
    public void removeTaskSession(String taskId) {
        messageCacheManager.removeTaskSession(taskId);
    }
}