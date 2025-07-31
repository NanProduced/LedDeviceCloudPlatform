package org.nan.cloud.message.infrastructure.websocket.reliability;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.nan.cloud.message.infrastructure.websocket.sender.StompMessageSender;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes.SUBSCRIPTION_STATUS;
import static org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes.TOPIC_SUBSCRIBE_FEEDBACK;

/**
 * STOMP消息确认处理器
 * 
 * 核心职责：
 * 1. 处理客户端的ACK/NACK消息
 * 2. 管理消息确认的生命周期
 * 3. 集成消息投递跟踪器
 * 4. 提供客户端确认状态查询
 * 
 * 支持的确认类型：
 * - MESSAGE_ACK: 消息确认（成功接收）
 * - MESSAGE_NACK: 消息拒绝（接收失败，需要重试）
 * - HEARTBEAT: 客户端心跳确认
 * - SUBSCRIPTION_ACK: 订阅确认
 * 
 * @author Nan
 * @since 3.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAckHandler {
    
    private final MessageDeliveryTracker deliveryTracker;
    private final StompConnectionManager connectionManager;
    private final StompMessageSender messageSender;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 客户端连接状态缓存
     * Key: userId, Value: 连接状态信息
     */
    private final Map<String, ClientConnectionStatus> connectionStatusCache = new ConcurrentHashMap<>();
    
    /**
     * 处理消息确认（ACK）
     */
    @MessageMapping("/ack")
    public void handleMessageAck(@Payload AckMessage ackMessage, 
                                SimpMessageHeaderAccessor headerAccessor,
                                Principal principal) {
        try {
            String userId = extractUserId(principal);
            String messageId = ackMessage.getMessageId();
            
            log.debug("🎯 收到消息确认: messageId={}, userId={}, type={}", 
                      messageId, userId, ackMessage.getAckType());
            
            // 处理不同类型的确认
            switch (ackMessage.getAckType()) {
                case MESSAGE_ACK:
                    handleMessageAcknowledge(messageId, userId, ackMessage);
                    break;
                case MESSAGE_NACK:
                    handleMessageReject(messageId, userId, ackMessage);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(userId, ackMessage);
                    break;
                case SUBSCRIPTION_ACK:
                    handleSubscriptionAck(userId, ackMessage);
                    break;
                default:
                    log.warn("⚠️ 未知的确认类型: {}", ackMessage.getAckType());
            }
            
        } catch (Exception e) {
            log.error("❌ 处理消息确认失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理订阅确认
     */
    @SubscribeMapping("/queue/ack")
    public void handleSubscription(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        try {
            String userId = extractUserId(principal);
            log.info("📬 用户订阅确认队列: userId={}", userId);
            
            // 更新连接状态
            updateConnectionStatus(userId, true);
            
            // 发送欢迎消息
            sendWelcomeMessage(userId);
            
        } catch (Exception e) {
            log.error("❌ 处理订阅确认失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理消息确认
     */
    private void handleMessageAcknowledge(String messageId, String userId, AckMessage ackMessage) {
        boolean success = deliveryTracker.acknowledgeMessage(messageId, userId);
        
        if (success) {
            // 更新连接状态
            updateConnectionStatus(userId, true);
            
            // 发布确认事件
            publishAckEvent(messageId, userId, AckType.MESSAGE_ACK, true, null);
            
            log.info("✅ 消息确认处理成功: messageId={}, userId={}", messageId, userId);
        } else {
            log.warn("⚠️ 消息确认处理失败: messageId={}, userId={} - 消息不存在或权限不足", 
                     messageId, userId);
        }
    }
    
    /**
     * 处理消息拒绝
     */
    private void handleMessageReject(String messageId, String userId, AckMessage ackMessage) {
        String reason = ackMessage.getReason();
        boolean success = deliveryTracker.rejectMessage(messageId, userId, reason);
        
        if (success) {
            // 发布拒绝事件
            publishAckEvent(messageId, userId, AckType.MESSAGE_NACK, false, reason);
            
            log.warn("⚠️ 消息被拒绝: messageId={}, userId={}, reason={}", 
                     messageId, userId, reason);
        } else {
            log.warn("⚠️ 消息拒绝处理失败: messageId={}, userId={} - 消息不存在或权限不足", 
                     messageId, userId);
        }
    }
    
    /**
     * 处理客户端心跳
     */
    private void handleHeartbeat(String userId, AckMessage ackMessage) {
        updateConnectionStatus(userId, true);
        
        // 记录心跳时间
        ClientConnectionStatus status = connectionStatusCache.get(userId);
        if (status != null) {
            status.setLastHeartbeatAt(LocalDateTime.now());
        }
        
        log.debug("💓 收到客户端心跳: userId={}, timestamp={}", 
                  userId, ackMessage.getTimestamp());
    }
    
    /**
     * 处理订阅确认
     */
    private void handleSubscriptionAck(String userId, AckMessage ackMessage) {
        String destination = ackMessage.getDestination();
        
        // 更新订阅状态
        updateConnectionStatus(userId, true);
        
        log.info("📡 订阅确认: userId={}, destination={}", userId, destination);
    }
    
    /**
     * 更新客户端连接状态
     */
    private void updateConnectionStatus(String userId, boolean isActive) {
        connectionStatusCache.compute(userId, (key, existingStatus) -> {
            if (existingStatus == null) {
                return ClientConnectionStatus.builder()
                    .userId(userId)
                    .isActive(isActive)
                    .lastActivityAt(LocalDateTime.now())
                    .connectedAt(LocalDateTime.now())
                    .build();
            } else {
                existingStatus.setActive(isActive);
                existingStatus.setLastActivityAt(LocalDateTime.now());
                return existingStatus;
            }
        });
    }
    
    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(String userId) {
        try {
            CommonStompMessage welcomeMessage = CommonStompMessage.builder()
                .messageId(deliveryTracker.generateMessageId())
                .messageType(TOPIC_SUBSCRIBE_FEEDBACK)
                .message("欢迎使用消息服务，确认机制已启用")
                .timestamp(LocalDateTime.now())
                .metadata(CommonStompMessage.Metadata.builder()
                    .requireAck(false) // 欢迎消息不需要确认
                    .priority(Priority.NORMAL)
                    .build())
                .build();
            
            String destination = "/user/" + userId + "/queue/notifications";
            messageSender.sendToUser(userId, destination, welcomeMessage);
            
            log.info("👋 发送欢迎消息: userId={}", userId);
            
        } catch (Exception e) {
            log.error("❌ 发送欢迎消息失败: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    /**
     * 发布确认事件
     */
    private void publishAckEvent(String messageId, String userId, AckType ackType, 
                                boolean success, String reason) {
        try {
            AckEvent event = AckEvent.builder()
                .messageId(messageId)
                .userId(userId)
                .ackType(ackType)
                .success(success)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
            
            eventPublisher.publishEvent(event);
            
        } catch (Exception e) {
            log.error("❌ 发布确认事件失败: messageId={}, error={}", messageId, e.getMessage());
        }
    }
    
    /**
     * 提取用户ID
     */
    private String extractUserId(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("用户身份信息不能为空");
        }
        return principal.getName();
    }
    
    /**
     * 获取客户端连接状态
     */
    public ClientConnectionStatus getConnectionStatus(String userId) {
        return connectionStatusCache.get(userId);
    }
    
    /**
     * 获取所有活跃连接数量
     */
    public long getActiveConnectionCount() {
        return connectionStatusCache.values().stream()
            .filter(ClientConnectionStatus::isActive)
            .count();
    }
    
    /**
     * 清理过期的连接状态
     */
    public void cleanupExpiredConnections(int maxIdleMinutes) {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(maxIdleMinutes);
        
        connectionStatusCache.entrySet().removeIf(entry -> {
            ClientConnectionStatus status = entry.getValue();
            if (status.getLastActivityAt().isBefore(expireTime)) {
                log.info("🧹 清理过期连接状态: userId={}, lastActivity={}", 
                         entry.getKey(), status.getLastActivityAt());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 确认消息模型
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AckMessage {
        private String messageId;
        private AckType ackType;
        private String reason;
        private String destination;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
    }
    
    /**
     * 确认类型枚举
     */
    public enum AckType {
        MESSAGE_ACK("消息确认"),
        MESSAGE_NACK("消息拒绝"),
        HEARTBEAT("心跳确认"),
        SUBSCRIPTION_ACK("订阅确认");
        
        private final String description;
        
        AckType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 客户端连接状态
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClientConnectionStatus {
        private String userId;
        private boolean isActive;
        private LocalDateTime connectedAt;
        private LocalDateTime lastActivityAt;
        private LocalDateTime lastHeartbeatAt;
        private int totalMessages;
        private int acknowledgedMessages;
        private int rejectedMessages;
        
        public double getAckRate() {
            return totalMessages > 0 ? (double) acknowledgedMessages / totalMessages * 100 : 0.0;
        }
    }
    
    /**
     * 确认事件
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AckEvent {
        private String messageId;
        private String userId;
        private AckType ackType;
        private boolean success;
        private String reason;
        private LocalDateTime timestamp;
    }
}