package org.nan.cloud.common.mq.producer.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.config.MqProperties;
import org.nan.cloud.common.mq.core.exception.MessageSendException;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.core.serializer.MessageSerializer;
import org.nan.cloud.common.mq.producer.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 默认消息生产者实现
 * 
 * 基于RabbitTemplate实现的消息生产者，提供完整的消息发送功能。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMessageProducer implements MessageProducer {
    
    private final RabbitTemplate rabbitTemplate;
    private final MessageSerializer messageSerializer; 
    private final MqProperties mqProperties;
    
    @Value("${spring.application.name:unknown}")
    private String applicationName;
    
    // 统计信息
    private final AtomicLong totalSentCount = new AtomicLong(0);
    private final AtomicLong successSentCount = new AtomicLong(0);
    private final AtomicLong failedSentCount = new AtomicLong(0);
    private final AtomicLong totalRetryCount = new AtomicLong(0);
    private volatile long maxSendDuration = 0;
    private volatile long minSendDuration = Long.MAX_VALUE;
    private volatile double averageSendDuration = 0.0;
    private final LocalDateTime startTime = LocalDateTime.now();
    
    @Override
    public SendResult send(Message message) {
        return send(
            message.getExchange() != null ? message.getExchange() : mqProperties.getDefaultExchange().getName(),
            message.getRoutingKey() != null ? message.getRoutingKey() : determineRoutingKey(message),
            message
        );
    }
    
    @Override
    public SendResult send(String exchange, String routingKey, Message message) {
        long startTime = System.currentTimeMillis();
        totalSentCount.incrementAndGet();
        
        try {
            log.debug("发送消息: messageId={}, exchange={}, routingKey={}", 
                    message.getMessageId(), exchange, routingKey);
            
            // 设置消息属性
            message.setExchange(exchange);
            message.setRoutingKey(routingKey);
            message.setSourceSystem(applicationName);
            
            // 创建关联数据
            String correlationId = "msg-" + message.getMessageId() + "-" + UUID.randomUUID().toString().substring(0, 8);
            CorrelationData correlationData = new CorrelationData(correlationId);
            
            // 发送消息
            rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
            
            long duration = System.currentTimeMillis() - startTime;
            updateStats(duration, true);
            
            message.markAsSent();
            successSentCount.incrementAndGet();
            
            SendResult result = SendResult.success(message.getMessageId(), exchange, routingKey, duration);
            result.setCorrelationId(correlationId);
            
            log.info("✅ 消息发送成功: {}", result.getDescription());
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            updateStats(duration, false);
            
            failedSentCount.incrementAndGet();
            message.markAsFailed(e.getMessage());
            
            log.error("❌ 消息发送失败: messageId={}, exchange={}, routingKey={}, error={}", 
                    message.getMessageId(), exchange, routingKey, e.getMessage(), e);
            
            SendResult result = SendResult.failure(message.getMessageId(), exchange, routingKey, 
                    "MESSAGE_SEND_FAILED", e.getMessage(), duration);
            
            // 如果启用重试且消息可以重试，进行重试
            if (mqProperties.getProducer().isRetryEnabled() && message.canRetry()) {
                return retryMessage(message, e, result);
            }
            
            return result;
        }
    }
    
    @Override
    public CompletableFuture<SendResult> sendAsync(Message message) {
        return CompletableFuture.supplyAsync(() -> send(message));
    }
    
    @Override
    public CompletableFuture<SendResult> sendAsync(String exchange, String routingKey, Message message) {
        return CompletableFuture.supplyAsync(() -> send(exchange, routingKey, message));
    }
    
    @Override
    public BatchSendResult sendBatch(List<Message> messages) {
        return sendBatch(
            mqProperties.getDefaultExchange().getName(),
            null, // 使用消息自己的路由键
            messages
        );
    }
    
    @Override
    public BatchSendResult sendBatch(String exchange, String routingKey, List<Message> messages) {
        long startTime = System.currentTimeMillis();
        String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("开始批量发送消息: batchId={}, count={}, exchange={}", batchId, messages.size(), exchange);
        
        List<SendResult> results = messages.stream()
            .map(message -> {
                String actualRoutingKey = routingKey != null ? routingKey : 
                    (message.getRoutingKey() != null ? message.getRoutingKey() : determineRoutingKey(message));
                return send(exchange, actualRoutingKey, message);
            })
            .collect(Collectors.toList());
        
        long totalDuration = System.currentTimeMillis() - startTime;
        
        int successCount = (int) results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        int totalCount = messages.size();
        
        BatchSendResult batchResult;
        if (successCount == totalCount) {
            batchResult = BatchSendResult.success(batchId, totalCount, totalDuration, results);
        } else {
            batchResult = BatchSendResult.partial(batchId, totalCount, successCount, totalDuration, results);
        }
        
        batchResult.setExchange(exchange);
        batchResult.setRoutingKey(routingKey);
        
        log.info("批量发送完成: {}", batchResult.getSummary());
        return batchResult;
    }
    
    @Override
    public CompletableFuture<BatchSendResult> sendBatchAsync(List<Message> messages) {
        return CompletableFuture.supplyAsync(() -> sendBatch(messages));
    }
    
    @Override
    public SendResult sendNotification(String subject, Object payload, String receiverId, String organizationId) {
        Message message = Message.notification(subject, payload, receiverId, organizationId);
        return send(message);
    }
    
    @Override
    public SendResult sendSystemMessage(String subject, Object payload, String organizationId) {
        Message message = Message.system(subject, payload, organizationId);
        return send(message);
    }
    
    @Override
    public SendResult sendUserMessage(String subject, Object payload, String senderId, String receiverId, String organizationId) {
        Message message = Message.userMessage(subject, payload, senderId, receiverId, organizationId);
        return send(message);
    }
    
    @Override
    public SendResult sendBroadcast(String subject, Object payload, String senderId, String organizationId) {
        Message message = Message.broadcast(subject, payload, senderId, organizationId);
        return send(message);
    }
    
    @Override
    public SendResult sendEvent(String eventType, Object eventData, String sourceSystem, String targetSystem) {
        Message message = Message.event(eventType, eventData, sourceSystem, targetSystem);
        return send(message);
    }
    
    @Override
    public SendResult sendDelayed(Message message, long delayMillis) {
        // 设置延迟时间（通过x-delay头实现，需要延迟插件支持）
        message.getProperties().put("x-delay", delayMillis);
        
        // 使用延迟交换机
        String delayExchange = mqProperties.getDefaultExchange().getName() + ".delay";
        return send(delayExchange, message.getRoutingKey(), message);
    }
    
    @Override
    public ProducerStats getStats() {
        return ProducerStats.builder()
                .totalSentCount(totalSentCount.get())
                .successSentCount(successSentCount.get())
                .failedSentCount(failedSentCount.get())
                .averageSendDuration(averageSendDuration)
                .maxSendDuration(maxSendDuration)
                .minSendDuration(minSendDuration == Long.MAX_VALUE ? 0 : minSendDuration)
                .totalRetryCount(totalRetryCount.get())
                .startTime(startTime)
                .lastUpdateTime(LocalDateTime.now())
                .build();
    }
    
    @Override
    public HealthStatus getHealth() {
        try {
            // 尝试获取连接信息来检查健康状态
            rabbitTemplate.getConnectionFactory().createConnection();
            
            double failureRate = getStats().getFailureRate();
            
            if (failureRate > 10.0) { // 失败率超过10%认为不健康
                return HealthStatus.unhealthy("高失败率", 
                    String.format("当前失败率: %.2f%%", failureRate));
            } else if (failureRate > 5.0) { // 失败率超过5%认为降级
                return HealthStatus.degraded("中等失败率", 
                    java.util.Map.of("failureRate", failureRate + "%"));
            } else {
                return HealthStatus.healthy("连接正常，发送成功率良好");
            }
            
        } catch (Exception e) {
            return HealthStatus.unhealthy("连接异常", e.getMessage());
        }
    }
    
    /**
     * 重试消息发送
     */
    private SendResult retryMessage(Message message, Exception originalException, SendResult originalResult) {
        int maxRetryAttempts = mqProperties.getProducer().getMaxRetryAttempts();
        long retryInterval = mqProperties.getProducer().getRetryInterval().toMillis();
        
        for (int attempt = 1; attempt <= maxRetryAttempts && message.canRetry(); attempt++) {
            try {
                log.info("🔄 重试发送消息: messageId={}, attempt={}/{}", 
                        message.getMessageId(), attempt, maxRetryAttempts);
                
                // 等待重试间隔
                Thread.sleep(retryInterval);
                
                message.incrementRetryCount();
                totalRetryCount.incrementAndGet();
                
                // 重新发送
                SendResult retryResult = send(message.getExchange(), message.getRoutingKey(), message);
                if (retryResult.isSuccess()) {
                    log.info("✅ 消息重试发送成功: messageId={}, attempt={}", 
                            message.getMessageId(), attempt);
                    retryResult.setRetryCount(attempt);
                    return retryResult;
                }
                
            } catch (Exception e) {
                log.warn("❌ 消息重试发送失败: messageId={}, attempt={}, error={}", 
                        message.getMessageId(), attempt, e.getMessage());
            }
        }
        
        log.error("❌ 消息重试全部失败: messageId={}, maxAttempts={}", 
                message.getMessageId(), maxRetryAttempts);
        
        originalResult.setRetryCount(maxRetryAttempts);
        return originalResult;
    }
    
    /**
     * 根据消息类型确定路由键
     */
    private String determineRoutingKey(Message message) {
        String messageType = message.getMessageType();
        if (messageType == null) {
            return "message.default";
        }
        
        switch (messageType.toLowerCase()) {
            case "notification":
                return "message.notification";
            case "system":
                return "message.system";
            case "user_message":
                return "message.user";
            case "broadcast":
                return "message.broadcast";
            case "event":
                return "event." + message.getSubject().toLowerCase();
            default:
                return "message.default";
        }
    }
    
    /**
     * 更新统计信息
     */
    private synchronized void updateStats(long duration, boolean success) {
        // 更新最大最小耗时
        if (duration > maxSendDuration) {
            maxSendDuration = duration;
        }
        if (duration < minSendDuration) {
            minSendDuration = duration;
        }
        
        // 更新平均耗时
        long totalCount = totalSentCount.get();
        if (totalCount > 0) {
            averageSendDuration = (averageSendDuration * (totalCount - 1) + duration) / totalCount;
        }
    }
}