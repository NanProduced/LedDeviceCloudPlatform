package org.nan.cloud.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.infrastructure.mq.publisher.MessageEventPublisher;
import org.nan.cloud.message.service.MessageQueueService;
import org.springframework.stereotype.Service;

/**
 * 消息队列服务实现
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueServiceImpl implements MessageQueueService {
    
    private final MessageEventPublisher messageEventPublisher;
    
    @Override
    public void publishNotification(String messageId, String title, String content, 
                                  String receiverId, String organizationId) {
        try {
            log.debug("发布通知消息: messageId={}, receiverId={}", messageId, receiverId);
            messageEventPublisher.publishNotification(messageId, title, content, receiverId, organizationId);
            log.info("✅ 通知消息发布成功: messageId={}", messageId);
        } catch (Exception e) {
            log.error("❌ 通知消息发布失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("通知消息发布失败", e);
        }
    }
    
    @Override
    public void publishSystemMessage(String messageId, String title, String content, 
                                   String organizationId) {
        try {
            log.debug("发布系统消息: messageId={}, organizationId={}", messageId, organizationId);
            messageEventPublisher.publishSystemMessage(messageId, title, content, organizationId);
            log.info("✅ 系统消息发布成功: messageId={}", messageId);
        } catch (Exception e) {
            log.error("❌ 系统消息发布失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("系统消息发布失败", e);
        }
    }
    
    @Override
    public void publishUserMessage(String messageId, String title, String content,
                                 String senderId, String senderName, 
                                 String receiverId, String organizationId) {
        try {
            log.debug("发布用户消息: messageId={}, senderId={}, receiverId={}", 
                    messageId, senderId, receiverId);
            messageEventPublisher.publishUserMessage(messageId, title, content, 
                    senderId, senderName, receiverId, organizationId);
            log.info("✅ 用户消息发布成功: messageId={}", messageId);
        } catch (Exception e) {
            log.error("❌ 用户消息发布失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("用户消息发布失败", e);
        }
    }
    
    @Override
    public void publishBroadcast(String messageId, String title, String content,
                               String senderId, String senderName, String organizationId) {
        try {
            log.debug("发布广播消息: messageId={}, senderId={}, organizationId={}", 
                    messageId, senderId, organizationId);
            messageEventPublisher.publishBroadcast(messageId, title, content, 
                    senderId, senderName, organizationId);
            log.info("✅ 广播消息发布成功: messageId={}", messageId);
        } catch (Exception e) {
            log.error("❌ 广播消息发布失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("广播消息发布失败", e);
        }
    }
    
    @Override
    public void publishEvent(MessageEvent event) {
        try {
            log.debug("发布自定义消息事件: eventId={}, messageId={}", 
                    event.getEventId(), event.getMessageId());
            messageEventPublisher.publishEvent(event);
            log.info("✅ 自定义消息事件发布成功: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("❌ 自定义消息事件发布失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("自定义消息事件发布失败", e);
        }
    }
    
    @Override
    public void republishFailedEvent(MessageEvent event) {
        try {
            log.debug("重新发布失败事件: eventId={}, retryCount={}", 
                    event.getEventId(), event.getRetryCount());
            messageEventPublisher.republishFailedEvent(event);
            log.info("✅ 失败事件重新发布成功: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("❌ 失败事件重新发布失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("失败事件重新发布失败", e);
        }
    }
}