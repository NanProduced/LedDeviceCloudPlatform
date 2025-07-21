package org.nan.cloud.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.repository.MessageEventRepository;
import org.nan.cloud.message.service.MessageQueueService;
import org.nan.cloud.message.utils.MessageUtils;
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
    
    private final MessageEventRepository messageEventRepository;

    /**
     * 异步发送通知消息
     *
     * 通过RabbitMQ队列异步处理消息，提高系统响应性能，
     * 支持消息持久化、重试机制和故障恢复。
     *
     * @param receiverId 接收者ID
     * @param organizationId 组织ID
     * @param title 消息标题
     * @param content 消息内容
     */
    @Override
    public void sendNotificationAsync(String receiverId, String organizationId,
                                      String title, String content) {
        try {
            String messageId = MessageUtils.generateMessageId();
            log.info("异步发送通知消息 - 接收者: {}, 组织: {}, 消息ID: {}",
                    receiverId, organizationId, messageId);

            messageEventRepository.publishNotification(messageId, title, content, receiverId, organizationId);

            log.debug("通知消息已发布到队列: messageId={}", messageId);
        } catch (Exception e) {
            log.error("异步发送通知消息失败 - 接收者: {}, 错误: {}", receiverId, e.getMessage(), e);
            throw new RuntimeException("异步消息发送失败", e);
        }
    }

    /**
     * 异步发送系统消息
     *
     * @param organizationId 组织ID
     * @param title 消息标题
     * @param content 消息内容
     */
    @Override
    public void sendSystemMessageAsync(String organizationId, String title, String content) {
        try {
            String messageId = MessageUtils.generateMessageId();
            log.info("异步发送系统消息 - 组织: {}, 消息ID: {}", organizationId, messageId);

            messageEventRepository.publishSystemMessage(messageId, title, content, organizationId);

            log.debug("系统消息已发布到队列: messageId={}", messageId);
        } catch (Exception e) {
            log.error("异步发送系统消息失败 - 组织: {}, 错误: {}", organizationId, e.getMessage(), e);
            throw new RuntimeException("异步系统消息发送失败", e);
        }
    }

    /**
     * 异步发送用户消息
     *
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @param receiverId 接收者ID
     * @param organizationId 组织ID
     * @param title 消息标题
     * @param content 消息内容
     */
    @Override
    public void sendUserMessageAsync(String senderId, String senderName, String receiverId,
                                     String organizationId, String title, String content) {
        try {
            String messageId = MessageUtils.generateMessageId();
            log.info("异步发送用户消息 - 发送者: {}, 接收者: {}, 消息ID: {}",
                    senderId, receiverId, messageId);

            messageEventRepository.publishUserMessage(messageId, title, content,
                    senderId, senderName, receiverId, organizationId);

            log.debug("用户消息已发布到队列: messageId={}", messageId);
        } catch (Exception e) {
            log.error("异步发送用户消息失败 - 发送者: {}, 接收者: {}, 错误: {}",
                    senderId, receiverId, e.getMessage(), e);
            throw new RuntimeException("异步用户消息发送失败", e);
        }
    }

    /**
     * 异步发送广播消息
     *
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @param organizationId 组织ID
     * @param title 消息标题
     * @param content 消息内容
     */
    @Override
    public void sendBroadcastAsync(String senderId, String senderName, String organizationId,
                                   String title, String content) {
        try {
            String messageId = MessageUtils.generateMessageId();
            log.info("异步发送广播消息 - 发送者: {}, 组织: {}, 消息ID: {}",
                    senderId, organizationId, messageId);

            messageEventRepository.publishBroadcast(messageId, title, content,
                    senderId, senderName, organizationId);

            log.debug("广播消息已发布到队列: messageId={}", messageId);
        } catch (Exception e) {
            log.error("异步发送广播消息失败 - 发送者: {}, 组织: {}, 错误: {}",
                    senderId, organizationId, e.getMessage(), e);
            throw new RuntimeException("异步广播消息发送失败", e);
        }
    }


    /**
     * 异步发送设备告警
     *
     * @param userId 用户ID（可选，为空时发送给整个组织）
     * @param organizationId 组织ID
     * @param deviceId 设备ID
     * @param title 告警标题
     * @param content 告警内容
     */
    @Override
    public void sendDeviceAlertAsync(String userId, String organizationId, String deviceId,
                                     String title, String content) {
        try {
            String messageId = MessageUtils.generateMessageId();
            log.info("异步发送设备告警 - 用户: {}, 组织: {}, 设备: {}, 消息ID: {}",
                    userId, organizationId, deviceId, messageId);

            // 创建设备告警的完整标题和内容
            String alertTitle = "设备告警: " + title;
            String alertContent = String.format("设备ID: %s\n%s", deviceId, content);

            if (userId != null && !userId.trim().isEmpty()) {
                // 发送给特定用户
                messageEventRepository.publishNotification(messageId, alertTitle, alertContent, userId, organizationId);
            } else {
                // 发送给整个组织
                messageEventRepository.publishSystemMessage(messageId, alertTitle, alertContent, organizationId);
            }

            log.debug("设备告警已发布到队列: messageId={}, deviceId={}", messageId, deviceId);
        } catch (Exception e) {
            log.error("异步发送设备告警失败 - 设备: {}, 错误: {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("异步设备告警发送失败", e);
        }
    }
    
    @Override
    public void publishEvent(MessageEvent event) {
        try {
            log.debug("发布自定义消息事件: eventId={}, messageId={}", 
                    event.getEventId(), event.getMessageId());
            messageEventRepository.publishEvent(event);
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
            messageEventRepository.republishFailedEvent(event);
            log.info("✅ 失败事件重新发布成功: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("❌ 失败事件重新发布失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("失败事件重新发布失败", e);
        }
    }
}