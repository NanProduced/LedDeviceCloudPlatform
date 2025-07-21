package org.nan.cloud.message.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.infrastructure.mq.publisher.MessageEventPublisher;
import org.nan.cloud.message.repository.MessageEventRepository;
import org.springframework.stereotype.Repository;

/**
 * 消息事件仓储实现
 * 
 * 实现Application层定义的MessageEventRepository接口，
 * 作为Application层和Infrastructure层之间的桥接。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageEventRepositoryImpl implements MessageEventRepository {
    
    private final MessageEventPublisher messageEventPublisher;
    
    @Override
    public void publishEvent(MessageEvent event) {
        try {
            log.debug("通过仓储发布消息事件: eventId={}, messageId={}", 
                    event.getEventId(), event.getMessageId());
            messageEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("仓储发布消息事件失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("消息事件发布失败", e);
        }
    }
    
    @Override
    public void publishNotification(String messageId, String title, String content, 
                                  String receiverId, String organizationId) {
        try {
            log.debug("通过仓储发布通知消息: messageId={}, receiverId={}", messageId, receiverId);
            messageEventPublisher.publishNotification(messageId, title, content, receiverId, organizationId);
        } catch (Exception e) {
            log.error("仓储发布通知消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("通知消息发布失败", e);
        }
    }
    
    @Override
    public void publishSystemMessage(String messageId, String title, String content, 
                                   String organizationId) {
        try {
            log.debug("通过仓储发布系统消息: messageId={}, organizationId={}", messageId, organizationId);
            messageEventPublisher.publishSystemMessage(messageId, title, content, organizationId);
        } catch (Exception e) {
            log.error("仓储发布系统消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("系统消息发布失败", e);
        }
    }
    
    @Override
    public void publishUserMessage(String messageId, String title, String content,
                                 String senderId, String senderName, 
                                 String receiverId, String organizationId) {
        try {
            log.debug("通过仓储发布用户消息: messageId={}, senderId={}, receiverId={}", 
                    messageId, senderId, receiverId);
            messageEventPublisher.publishUserMessage(messageId, title, content, 
                    senderId, senderName, receiverId, organizationId);
        } catch (Exception e) {
            log.error("仓储发布用户消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("用户消息发布失败", e);
        }
    }
    
    @Override
    public void publishBroadcast(String messageId, String title, String content,
                               String senderId, String senderName, String organizationId) {
        try {
            log.debug("通过仓储发布广播消息: messageId={}, senderId={}, organizationId={}", 
                    messageId, senderId, organizationId);
            messageEventPublisher.publishBroadcast(messageId, title, content, 
                    senderId, senderName, organizationId);
        } catch (Exception e) {
            log.error("仓储发布广播消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("广播消息发布失败", e);
        }
    }
    
    @Override
    public void republishFailedEvent(MessageEvent event) {
        try {
            log.debug("通过仓储重新发布失败事件: eventId={}, retryCount={}", 
                    event.getEventId(), event.getRetryCount());
            messageEventPublisher.republishFailedEvent(event);
        } catch (Exception e) {
            log.error("仓储重新发布失败事件失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("失败事件重新发布失败", e);
        }
    }
}