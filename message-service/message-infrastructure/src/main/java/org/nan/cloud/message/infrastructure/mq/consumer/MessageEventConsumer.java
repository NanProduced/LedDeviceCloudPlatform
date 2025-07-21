package org.nan.cloud.message.infrastructure.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.infrastructure.mq.config.RabbitMQConfig;
import org.nan.cloud.message.infrastructure.websocket.manager.WebSocketConnectionManager;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 消息事件消费者
 * 
 * 负责消费RabbitMQ队列中的消息事件，并通过WebSocket分发给客户端
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventConsumer {
    
    private final WebSocketConnectionManager connectionManager;
    
    /**
     * 处理通知消息
     */
    @RabbitListener(queues = RabbitMQConfig.MESSAGE_NOTIFICATION_QUEUE)
    public void handleNotificationMessage(@Payload MessageEvent event, 
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                        Channel channel,
                                        Message message) {
        try {
            log.info("📨 收到通知消息事件: eventId={}, messageId={}", 
                    event.getEventId(), event.getMessageId());
            
            // 处理通知消息
            processNotificationMessage(event);
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            
            log.info("✅ 通知消息处理成功: eventId={}", event.getEventId());
            
        } catch (Exception e) {
            log.error("❌ 通知消息处理失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            handleMessageProcessingError(event, deliveryTag, channel, e);
        }
    }
    
    /**
     * 处理系统消息
     */
    @RabbitListener(queues = RabbitMQConfig.MESSAGE_SYSTEM_QUEUE)
    public void handleSystemMessage(@Payload MessageEvent event,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                  Channel channel,
                                  Message message) {
        try {
            log.info("📨 收到系统消息事件: eventId={}, messageId={}", 
                    event.getEventId(), event.getMessageId());
            
            // 处理系统消息
            processSystemMessage(event);
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            
            log.info("✅ 系统消息处理成功: eventId={}", event.getEventId());
            
        } catch (Exception e) {
            log.error("❌ 系统消息处理失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            handleMessageProcessingError(event, deliveryTag, channel, e);
        }
    }
    
    /**
     * 处理用户消息
     */
    @RabbitListener(queues = RabbitMQConfig.MESSAGE_USER_QUEUE)
    public void handleUserMessage(@Payload MessageEvent event,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                Channel channel,
                                Message message) {
        try {
            log.info("📨 收到用户消息事件: eventId={}, messageId={}", 
                    event.getEventId(), event.getMessageId());
            
            // 处理用户消息
            processUserMessage(event);
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            
            log.info("✅ 用户消息处理成功: eventId={}", event.getEventId());
            
        } catch (Exception e) {
            log.error("❌ 用户消息处理失败: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            handleMessageProcessingError(event, deliveryTag, channel, e);
        }
    }
    
    /**
     * 处理通知消息的具体逻辑
     */
    private void processNotificationMessage(MessageEvent event) {
        // 创建WebSocket消息
        WebSocketMessage wsMessage = createWebSocketMessage(event);
        
        // 根据接收者类型分发消息
        String receiverType = event.getReceiverType();
        
        switch (receiverType) {
            case "USER":
                // 发送给特定用户
                if (event.getReceiverId() != null) {
                    int sentCount = connectionManager.sendMessageToUser(event.getReceiverId(), wsMessage);
                    log.debug("通知消息已发送给用户: userId={}, sentCount={}", 
                            event.getReceiverId(), sentCount);
                }
                break;
                
            case "ORGANIZATION":
                // 发送给组织内所有用户
                if (event.getOrganizationId() != null) {
                    int sentCount = connectionManager.broadcastToOrganization(event.getOrganizationId(), wsMessage);
                    log.debug("通知消息已发送给组织: orgId={}, sentCount={}", 
                            event.getOrganizationId(), sentCount);
                }
                break;
                
            case "ALL":
                // 发送给所有在线用户
                int sentCount = connectionManager.broadcastToAll(wsMessage);
                log.debug("通知消息已广播给所有用户: sentCount={}", sentCount);
                break;
                
            default:
                log.warn("⚠️ 未知的接收者类型: receiverType={}", receiverType);
        }
        
        // 标记事件为已处理
        event.markAsProcessed("WebSocket消息发送完成");
    }
    
    /**
     * 处理系统消息的具体逻辑
     */
    private void processSystemMessage(MessageEvent event) {
        // 创建WebSocket消息
        WebSocketMessage wsMessage = createWebSocketMessage(event);
        
        // 系统消息通常发送给组织内所有用户
        if (event.getOrganizationId() != null) {
            int sentCount = connectionManager.broadcastToOrganization(event.getOrganizationId(), wsMessage);
            log.debug("系统消息已发送给组织: orgId={}, sentCount={}", 
                    event.getOrganizationId(), sentCount);
        } else {
            // 如果没有指定组织，发送给所有用户
            int sentCount = connectionManager.broadcastToAll(wsMessage);
            log.debug("系统消息已广播给所有用户: sentCount={}", sentCount);
        }
        
        // 标记事件为已处理
        event.markAsProcessed("系统消息WebSocket发送完成");
    }
    
    /**
     * 处理用户消息的具体逻辑
     */
    private void processUserMessage(MessageEvent event) {
        // 创建WebSocket消息
        WebSocketMessage wsMessage = createWebSocketMessage(event);
        
        // 根据事件类型处理
        if ("MESSAGE_BROADCAST".equals(event.getEventType())) {
            // 广播消息
            if (event.getOrganizationId() != null) {
                int sentCount = connectionManager.broadcastToOrganization(event.getOrganizationId(), wsMessage);
                log.debug("广播消息已发送给组织: orgId={}, sentCount={}", 
                        event.getOrganizationId(), sentCount);
            } else {
                int sentCount = connectionManager.broadcastToAll(wsMessage);
                log.debug("广播消息已发送给所有用户: sentCount={}", sentCount);
            }
        } else {
            // 点对点用户消息
            if (event.getReceiverId() != null) {
                int sentCount = connectionManager.sendMessageToUser(event.getReceiverId(), wsMessage);
                log.debug("用户消息已发送: fromUserId={}, toUserId={}, sentCount={}", 
                        event.getSenderId(), event.getReceiverId(), sentCount);
            }
        }
        
        // 标记事件为已处理
        event.markAsProcessed("用户消息WebSocket发送完成");
    }
    
    /**
     * 创建WebSocket消息
     */
    private WebSocketMessage createWebSocketMessage(MessageEvent event) {
        return WebSocketMessage.builder()
                .messageId(event.getMessageId())
                .type(event.getMessageType())
                .title(event.getTitle())
                .content(event.getContent())
                .priority(event.getPriority())
                .senderId(event.getSenderId())
                .senderName(event.getSenderName())
                .receiverId(event.getReceiverId())
                .organizationId(event.getOrganizationId())
                .timestamp(event.getTimestamp() != null ? event.getTimestamp().toString() : null)
                .data(event.getData())
                .requireAck(event.getRequireAck())
                .expireTime(event.getExpireTime())
                .retryCount(event.getRetryCount())
                .build();
    }
    
    /**
     * 处理消息处理错误
     */
    private void handleMessageProcessingError(MessageEvent event, long deliveryTag, 
                                            Channel channel, Exception error) {
        try {
            // 标记事件处理失败
            event.markAsFailed(error.getMessage());
            
            // 检查是否可以重试
            if (event.canRetry()) {
                log.warn("⚠️ 消息处理失败，稍后重试: eventId={}, retryCount={}/{}", 
                        event.getEventId(), event.getRetryCount(), event.getMaxRetryCount());
                
                // 拒绝消息，让它重新入队
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("❌ 消息处理失败且已达到最大重试次数，丢弃消息: eventId={}", 
                        event.getEventId());
                
                // 拒绝消息，不重新入队（会进入死信队列）
                channel.basicNack(deliveryTag, false, false);
            }
            
        } catch (Exception e) {
            log.error("💥 处理消息错误时发生异常: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
        }
    }
}