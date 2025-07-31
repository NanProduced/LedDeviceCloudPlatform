package org.nan.cloud.message.infrastructure.websocket.reliability;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.sender.StompMessageSender;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可靠性STOMP消息发送服务
 * 
 * 核心功能：
 * 1. 集成消息投递跟踪和确认机制
 * 2. 自动重试失败或超时的消息
 * 3. 支持消息优先级和重试策略
 * 4. 提供消息投递状态监控
 * 
 * 特性：
 * - 消息唯一ID自动生成
 * - 基于消息类型的差异化重试策略
 * - 异步消息重试机制
 * - 完整的投递生命周期管理
 * - 投递统计和监控支持
 * 
 * @author Nan
 * @since 3.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReliableMessageSender {
    
    private final StompMessageSender stompMessageSender;
    private final MessageDeliveryTracker deliveryTracker;
    private final RetryConfiguration retryConfiguration;
    
    /**
     * 重试任务缓存
     * Key: messageId, Value: 重试任务Future
     */
    private final Map<String, CompletableFuture<Void>> retryTasks = new ConcurrentHashMap<>();
    
    /**
     * 发送可靠性消息到指定用户
     * 
     * @param userId 目标用户ID
     * @param destination 目标地址
     * @param message 消息内容
     * @param requiresAck 是否需要确认
     * @return 消息发送结果
     */
    public MessageSendResult sendReliableMessage(String userId, String destination, 
                                               CommonStompMessage message, boolean requiresAck) {
        try {
            // 确保消息有唯一ID
            if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
                message.setMessageId(deliveryTracker.generateMessageId());
            }
            
            // 设置确认要求和时间戳
            if (message.getMetadata() == null) {
                message.setMetadata(CommonStompMessage.Metadata.builder().build());
            }
            message.getMetadata().setRequireAck(requiresAck);
            message.setTimestamp(LocalDateTime.now());
            
            log.info("📤 发送可靠性消息: messageId={}, userId={}, destination={}, requiresAck={}", 
                     message.getMessageId(), userId, destination, requiresAck);
            
            // 如果需要确认，开始跟踪投递
            if (requiresAck) {
                // 提取消息内容（优先使用payload，其次使用message字段）
                Object messageContent = message.getPayload() != null ? message.getPayload() : message.getMessage();
                
                deliveryTracker.startTracking(
                    message.getMessageId(),
                    destination,
                    userId,
                    message.getMessageType().name(),
                    messageContent
                );
            }
            
            // 执行实际发送
            boolean sent = stompMessageSender.sendToUser(userId, destination, message);
            
            if (sent) {
                return MessageSendResult.builder()
                    .messageId(message.getMessageId())
                    .success(true)
                    .requiresAck(requiresAck)
                    .sentAt(LocalDateTime.now())
                    .build();
            } else {
                // 发送失败，如果需要确认则处理重试
                if (requiresAck) {
                    handleSendFailure(message.getMessageId(), userId, destination, message, "SEND_FAILED");
                }
                
                return MessageSendResult.builder()
                    .messageId(message.getMessageId())
                    .success(false)
                    .requiresAck(requiresAck)
                    .errorMessage("消息发送失败")
                    .build();
            }
            
        } catch (Exception e) {
            log.error("❌ 发送可靠性消息异常: messageId={}, error={}", 
                      message.getMessageId(), e.getMessage(), e);
            
            return MessageSendResult.builder()
                .messageId(message.getMessageId())
                .success(false)
                .requiresAck(requiresAck)
                .errorMessage("发送异常: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 发送可靠性消息到主题
     * 
     * @param topic 主题地址
     * @param message 消息内容
     * @param requiresAck 是否需要确认
     * @return 消息发送结果
     */
    public MessageSendResult sendReliableTopicMessage(String topic, CommonStompMessage message, 
                                                    boolean requiresAck) {
        try {
            // 确保消息有唯一ID
            if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
                message.setMessageId(deliveryTracker.generateMessageId());
            }
            
            // 设置确认要求和时间戳
            if (message.getMetadata() == null) {
                message.setMetadata(CommonStompMessage.Metadata.builder().build());
            }
            message.getMetadata().setRequireAck(requiresAck);
            message.setTimestamp(LocalDateTime.now());
            
            log.info("📡 发送主题可靠性消息: messageId={}, topic={}, requiresAck={}", 
                     message.getMessageId(), topic, requiresAck);
            
            // 主题消息通常不需要个人确认，但可以记录发送统计
            boolean sent = stompMessageSender.sendToTopic(topic, message);
            
            return MessageSendResult.builder()
                .messageId(message.getMessageId())
                .success(sent)
                .requiresAck(false) // 主题消息不支持个人确认
                .sentAt(LocalDateTime.now())
                .build();
            
        } catch (Exception e) {
            log.error("❌ 发送主题可靠性消息异常: messageId={}, error={}", 
                      message.getMessageId(), e.getMessage(), e);
            
            return MessageSendResult.builder()
                .messageId(message.getMessageId())
                .success(false)
                .requiresAck(false)
                .errorMessage("发送异常: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 处理发送失败
     */
    private void handleSendFailure(String messageId, String userId, String destination, 
                                 CommonStompMessage message, String reason) {
        RetryConfiguration.RetryPolicy policy = retryConfiguration.getRetryPolicy(message.getMessageType().name());
        
        if (policy.shouldRetry(0, reason)) {
            log.info("🔄 安排消息重试: messageId={}, reason={}", messageId, reason);
            scheduleRetry(messageId, userId, destination, message, 0);
        } else {
            log.warn("⚠️ 消息不满足重试条件: messageId={}, reason={}", messageId, reason);
        }
    }
    
    /**
     * 安排消息重试
     */
    private void scheduleRetry(String messageId, String userId, String destination, 
                             CommonStompMessage message, int attemptNumber) {
        RetryConfiguration.RetryPolicy policy = retryConfiguration.getRetryPolicy(message.getMessageType().name());
        long delaySeconds = policy.calculateDelay(attemptNumber);
        
        CompletableFuture<Void> retryTask = CompletableFuture
            .runAsync(() -> {
                try {
                    Thread.sleep(delaySeconds * 1000);
                    executeRetry(messageId, userId, destination, message, attemptNumber + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        
        retryTasks.put(messageId, retryTask);
        
        log.info("⏰ 消息重试已安排: messageId={}, attempt={}, delay={}s", 
                 messageId, attemptNumber + 1, delaySeconds);
    }
    
    /**
     * 执行消息重试
     */
    @Async
    public void executeRetry(String messageId, String userId, String destination, 
                           CommonStompMessage message, int attemptNumber) {
        try {
            log.info("🔄 执行消息重试: messageId={}, attempt={}", messageId, attemptNumber);
            
            // 重新发送消息
            boolean sent = stompMessageSender.sendToUser(userId, destination, message);
            
            if (sent) {
                log.info("✅ 消息重试成功: messageId={}, attempt={}", messageId, attemptNumber);
                // 移除重试任务
                retryTasks.remove(messageId);
            } else {
                // 重试失败，检查是否继续重试
                RetryConfiguration.RetryPolicy policy = retryConfiguration.getRetryPolicy(message.getMessageType().name());
                
                if (policy.shouldRetry(attemptNumber, "RETRY_FAILED")) {
                    scheduleRetry(messageId, userId, destination, message, attemptNumber);
                } else {
                    log.error("💀 消息重试最终失败: messageId={}, totalAttempts={}", 
                              messageId, attemptNumber);
                    retryTasks.remove(messageId);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ 执行消息重试异常: messageId={}, attempt={}, error={}", 
                      messageId, attemptNumber, e.getMessage(), e);
        }
    }
    
    /**
     * 监听确认事件，处理确认成功的消息
     */
    @EventListener
    public void handleAckEvent(StompAckHandler.AckEvent event) {
        if (event.getAckType() == StompAckHandler.AckType.MESSAGE_ACK && event.isSuccess()) {
            // 取消对应的重试任务
            CompletableFuture<Void> retryTask = retryTasks.remove(event.getMessageId());
            if (retryTask != null && !retryTask.isDone()) {
                retryTask.cancel(false);
                log.info("🚫 取消消息重试任务: messageId={}", event.getMessageId());
            }
        }
    }
    
    /**
     * 获取待重试消息数量
     */
    public int getPendingRetryCount() {
        return retryTasks.size();
    }
    
    /**
     * 取消指定消息的重试
     */
    public boolean cancelRetry(String messageId) {
        CompletableFuture<Void> retryTask = retryTasks.remove(messageId);
        if (retryTask != null && !retryTask.isDone()) {
            boolean cancelled = retryTask.cancel(false);
            log.info("🚫 手动取消消息重试: messageId={}, success={}", messageId, cancelled);
            return cancelled;
        }
        return false;
    }
    
    /**
     * 清理已完成的重试任务
     */
    public void cleanupCompletedRetryTasks() {
        retryTasks.entrySet().removeIf(entry -> {
            CompletableFuture<Void> task = entry.getValue();
            if (task.isDone()) {
                log.debug("🧹 清理已完成的重试任务: messageId={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 消息发送结果
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageSendResult {
        private String messageId;
        private boolean success;
        private boolean requiresAck;
        private LocalDateTime sentAt;
        private String errorMessage;
        private Map<String, Object> metadata;
    }
}