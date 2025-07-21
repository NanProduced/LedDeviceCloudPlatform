package org.nan.cloud.message.infrastructure.mq.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.infrastructure.mq.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息事件发布者
 * 
 * 负责将消息事件发布到RabbitMQ队列
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 发布消息事件
     * 
     * @param event 消息事件
     */
    public void publishEvent(MessageEvent event) {
        try {
            log.debug("发布消息事件: eventId={}, messageId={}, type={}", 
                    event.getEventId(), event.getMessageId(), event.getEventType());
            
            // 根据事件类型确定路由键
            String routingKey = determineRoutingKey(event);
            
            // 发送到主交换机
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MESSAGE_EXCHANGE, 
                    routingKey, 
                    event
            );
            
            log.info("✅ 消息事件发布成功: eventId={}, routingKey={}", 
                    event.getEventId(), routingKey);
            
        } catch (Exception e) {
            log.error("❌ 消息事件发布失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("消息事件发布失败", e);
        }
    }
    
    /**
     * 发布通知消息
     */
    public void publishNotification(String messageId, String title, String content, 
                                  String receiverId, String organizationId) {
        MessageEvent event = MessageEvent.notification(messageId, title, content, receiverId, organizationId);
        publishEvent(event);
    }
    
    /**
     * 发布系统消息
     */
    public void publishSystemMessage(String messageId, String title, String content, 
                                   String organizationId) {
        MessageEvent event = MessageEvent.system(messageId, title, content, organizationId);
        publishEvent(event);
    }
    
    /**
     * 发布用户消息
     */
    public void publishUserMessage(String messageId, String title, String content,
                                 String senderId, String senderName, 
                                 String receiverId, String organizationId) {
        MessageEvent event = MessageEvent.userMessage(messageId, title, content, 
                senderId, senderName, receiverId, organizationId);
        publishEvent(event);
    }
    
    /**
     * 发布广播消息
     */
    public void publishBroadcast(String messageId, String title, String content,
                               String senderId, String senderName, String organizationId) {
        MessageEvent event = MessageEvent.broadcast(messageId, title, content, 
                senderId, senderName, organizationId);
        publishEvent(event);
    }
    
    /**
     * 重新发布失败的事件
     */
    public void republishFailedEvent(MessageEvent event) {
        try {
            if (!event.canRetry()) {
                log.warn("⚠️ 事件已达到最大重试次数，放弃重试: eventId={}", event.getEventId());
                return;
            }
            
            if (event.isExpired()) {
                log.warn("⚠️ 事件已过期，放弃重试: eventId={}", event.getEventId());
                return;
            }
            
            log.info("🔄 重新发布失败事件: eventId={}, retryCount={}", 
                    event.getEventId(), event.getRetryCount());
            
            // 增加重试计数
            event.setRetryCount(event.getRetryCount() + 1);
            
            publishEvent(event);
            
        } catch (Exception e) {
            log.error("❌ 重新发布事件失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            event.markAsFailed("重新发布失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据事件类型确定路由键
     */
    private String determineRoutingKey(MessageEvent event) {
        // 如果事件已经指定了路由键，使用指定的
        if (event.getRoutingKey() != null && !event.getRoutingKey().isEmpty()) {
            return event.getRoutingKey();
        }
        
        // 根据事件类型确定路由键
        switch (event.getEventType()) {
            case "MESSAGE_NOTIFICATION":
                return RabbitMQConfig.NOTIFICATION_ROUTING_KEY;
            case "MESSAGE_SYSTEM":
                return RabbitMQConfig.SYSTEM_ROUTING_KEY;
            case "MESSAGE_USER":
            case "MESSAGE_BROADCAST":
                return RabbitMQConfig.USER_ROUTING_KEY;
            default:
                log.warn("⚠️ 未知的事件类型，使用默认路由键: eventType={}", event.getEventType());
                return RabbitMQConfig.NOTIFICATION_ROUTING_KEY;
        }
    }
}