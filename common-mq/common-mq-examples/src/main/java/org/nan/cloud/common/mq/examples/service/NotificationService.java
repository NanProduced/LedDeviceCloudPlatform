package org.nan.cloud.common.mq.examples.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.BatchSendResult;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 通知服务示例
 * 
 * 演示如何使用MessageProducer发送各种类型的消息。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final MessageProducer messageProducer;
    
    /**
     * 发送用户通知
     */
    public void sendUserNotification(String userId, String title, String content) {
        log.info("发送用户通知: userId={}, title={}", userId, title);
        
        SendResult result = messageProducer.sendNotification(title, content, userId, "org001");
        
        if (result.isSuccess()) {
            log.info("✅ 用户通知发送成功: {}", result.getDescription());
        } else {
            log.error("❌ 用户通知发送失败: {}", result.getDescription());
        }
    }
    
    /**
     * 发送系统消息
     */
    public void sendSystemMessage(String title, String content) {
        log.info("发送系统消息: title={}", title);
        
        SendResult result = messageProducer.sendSystemMessage(title, content, "org001");
        
        if (result.isSuccess()) {
            log.info("✅ 系统消息发送成功: {}", result.getDescription());
        } else {
            log.error("❌ 系统消息发送失败: {}", result.getDescription());
        }
    }
    
    /**
     * 异步发送通知
     */
    public CompletableFuture<SendResult> sendNotificationAsync(String userId, String title, String content) {
        log.info("异步发送通知: userId={}, title={}", userId, title);
        
        Message message = Message.notification(title, content, userId, "org001");
        return messageProducer.sendAsync(message).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("❌ 异步通知发送异常", throwable);
            } else if (result.isSuccess()) {
                log.info("✅ 异步通知发送成功: {}", result.getDescription());
            } else {
                log.error("❌ 异步通知发送失败: {}", result.getDescription());
            }
        });
    }
    
    /**
     * 批量发送通知
     */
    public void sendBatchNotifications(List<String> userIds, String title, String content) {
        log.info("批量发送通知: userCount={}, title={}", userIds.size(), title);
        
        List<Message> messages = userIds.stream()
            .map(userId -> Message.notification(title, content, userId, "org001"))
            .collect(Collectors.toList());
        
        BatchSendResult result = messageProducer.sendBatch(messages);
        
        log.info("批量发送结果: {}", result.getSummary());
        
        if (!result.isAllSuccess()) {
            log.warn("部分消息发送失败: 总数={}, 成功={}, 失败={}", 
                    result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());
        }
    }
    
    /**
     * 发送业务事件
     */
    public void sendBusinessEvent(String eventType, Object eventData) {
        log.info("发送业务事件: eventType={}", eventType);
        
        SendResult result = messageProducer.sendEvent(eventType, eventData, "notification-service", "message-service");
        
        if (result.isSuccess()) {
            log.info("✅ 业务事件发送成功: {}", result.getDescription());
        } else {
            log.error("❌ 业务事件发送失败: {}", result.getDescription());
        }
    }
    
    /**
     * 发送延迟消息
     */
    public void sendDelayedMessage(String userId, String content, long delayMillis) {
        log.info("发送延迟消息: userId={}, delay={}ms", userId, delayMillis);
        
        Message message = Message.notification("延迟通知", content, userId, "org001");
        SendResult result = messageProducer.sendDelayed(message, delayMillis);
        
        if (result.isSuccess()) {
            log.info("✅ 延迟消息发送成功: {}", result.getDescription());
        } else {
            log.error("❌ 延迟消息发送失败: {}", result.getDescription());
        }
    }
}