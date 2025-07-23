package org.nan.cloud.common.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.config.MqProperties;
import org.nan.cloud.common.mq.core.exception.MessageConsumeException;
import org.nan.cloud.common.mq.core.message.Message;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消息消费者管理器
 * 
 * 统一管理消息消费逻辑，处理消息确认、重试、错误处理等。
 * 自动发现和路由到相应的MessageConsumer实现。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumerManager {
    
    private final List<MessageConsumer> messageConsumers;
    private final MqProperties mqProperties;
    
    // 统计信息
    private final AtomicLong totalConsumedCount = new AtomicLong(0);
    private final AtomicLong successConsumedCount = new AtomicLong(0);
    private final AtomicLong failedConsumedCount = new AtomicLong(0);
    private final AtomicLong retryCount = new AtomicLong(0);
    private final LocalDateTime startTime = LocalDateTime.now();
    
    /**
     * 处理通知消息
     */
    @RabbitListener(queues = "${nan.mq.queues.notification:message.notification.queue}")
    @RabbitHandler
    public void handleNotificationMessage(Message message, Channel channel, 
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        handleMessage(message, channel, deliveryTag, "notification");
    }
    
    /**
     * 处理系统消息
     */
    @RabbitListener(queues = "${nan.mq.queues.system:message.system.queue}")
    @RabbitHandler
    public void handleSystemMessage(Message message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        handleMessage(message, channel, deliveryTag, "system");
    }
    
    /**
     * 处理用户消息
     */
    @RabbitListener(queues = "${nan.mq.queues.user:message.user.queue}")
    @RabbitHandler
    public void handleUserMessage(Message message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        handleMessage(message, channel, deliveryTag, "user");
    }
    
    /**
     * 处理事件消息
     */
    @RabbitListener(queues = "${nan.mq.queues.event:event.queue}")
    @RabbitHandler
    public void handleEventMessage(Message message, Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        handleMessage(message, channel, deliveryTag, "event");
    }
    
    /**
     * 通用消息处理逻辑
     */
    private void handleMessage(Message message, Channel channel, long deliveryTag, String queueType) {
        long startTime = System.currentTimeMillis();
        totalConsumedCount.incrementAndGet();
        
        log.debug("收到消息: messageId={}, type={}, queue={}", 
                message.getMessageId(), message.getMessageType(), queueType);
        
        try {
            // 检查消息是否过期
            if (message.isExpired()) {
                log.warn("⚠️ 消息已过期，丢弃处理: messageId={}, expireTime={}", 
                        message.getMessageId(), message.getExpireTime());
                acknowledgeMessage(channel, deliveryTag, true);
                return;
            }
            
            // 查找合适的消费者
            MessageConsumer consumer = findConsumer(message);
            if (consumer == null) {
                log.warn("⚠️ 未找到合适的消费者: messageId={}, messageType={}", 
                        message.getMessageId(), message.getMessageType());
                acknowledgeMessage(channel, deliveryTag, false); // 重新入队
                return;
            }
            
            // 执行消费前置处理
            if (!consumer.preConsume(message)) {
                log.info("消息被前置处理跳过: messageId={}, consumerId={}", 
                        message.getMessageId(), consumer.getConsumerId());
                acknowledgeMessage(channel, deliveryTag, true);
                return;
            }
            
            ConsumeResult result;
            try {
                // 执行消费逻辑
                result = consumer.consume(message);
                if (result == null) {
                    result = ConsumeResult.success(message.getMessageId(), consumer.getConsumerId(), 
                            System.currentTimeMillis() - startTime);
                }
            } catch (Exception e) {
                log.error("❌ 消息消费异常: messageId={}, consumerId={}, error={}", 
                        message.getMessageId(), consumer.getConsumerId(), e.getMessage(), e);
                result = consumer.onError(message, e);
            }
            
            // 设置消费耗时
            if (result.getDuration() == 0) {
                result.setDuration(System.currentTimeMillis() - startTime);
            }
            
            // 执行后置处理
            consumer.postConsume(message, result);
            
            // 处理消费结果
            handleConsumeResult(message, result, channel, deliveryTag);
            
            // 更新统计信息
            if (result.isSuccess()) {
                successConsumedCount.incrementAndGet();
                message.markAsConsumed();
                log.info("✅ 消息消费成功: {}", result.getDescription());
            } else {
                failedConsumedCount.incrementAndGet();
                log.error("❌ 消息消费失败: {}", result.getDescription());
            }
            
            // 慢消息检测
            if (mqProperties.getMonitor().isSlowMessageLoggingEnabled()) {
                long threshold = mqProperties.getMonitor().getSlowMessageThreshold().toMillis();
                if (result.getDuration() > threshold) {
                    log.warn("🐌 检测到慢消息: messageId={}, consumerId={}, duration={}ms, threshold={}ms",
                            message.getMessageId(), consumer.getConsumerId(), result.getDuration(), threshold);
                }
            }
            
        } catch (Exception e) {
            failedConsumedCount.incrementAndGet();
            log.error("❌ 消息处理异常: messageId={}, queue={}, error={}", 
                    message.getMessageId(), queueType, e.getMessage(), e);
            
            try {
                // 异常情况下拒绝消息，重新入队
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("消息确认失败", ioException);
            }
        }
    }
    
    /**
     * 查找合适的消费者
     */
    private MessageConsumer findConsumer(Message message) {
        String messageType = message.getMessageType();
        
        for (MessageConsumer consumer : messageConsumers) {
            if (consumer.supports(messageType)) {
                return consumer;
            }
        }
        
        // 如果没有找到特定类型的消费者，查找支持所有类型的消费者
        for (MessageConsumer consumer : messageConsumers) {
            if (consumer.getSupportedMessageTypes().length == 0) {
                return consumer;
            }
        }
        
        return null;
    }
    
    /**
     * 处理消费结果
     */
    private void handleConsumeResult(Message message, ConsumeResult result, 
                                   Channel channel, long deliveryTag) throws IOException {
        switch (result.getAction()) {
            case ACK:
                acknowledgeMessage(channel, deliveryTag, true);
                break;
                
            case NACK:
                acknowledgeMessage(channel, deliveryTag, false); // 重新入队
                break;
                
            case REJECT:
                channel.basicReject(deliveryTag, false); // 不重新入队，进入死信队列
                break;
                
            case RETRY:
                if (result.canRetry()) {
                    retryCount.incrementAndGet();
                    handleRetry(message, result, channel, deliveryTag);
                } else {
                    log.warn("⚠️ 消息重试次数已达上限，发送到死信队列: messageId={}, retryCount={}", 
                            message.getMessageId(), result.getRetryCount());
                    channel.basicReject(deliveryTag, false);
                }
                break;
                
            default:
                log.warn("⚠️ 未知的消费动作: {}, 默认确认消息", result.getAction());
                acknowledgeMessage(channel, deliveryTag, true);
                break;
        }
    }
    
    /**
     * 处理消息重试
     */
    private void handleRetry(Message message, ConsumeResult result, 
                           Channel channel, long deliveryTag) throws IOException {
        // 增加重试次数
        message.incrementRetryCount();
        result.incrementRetryCount();
        
        log.info("🔄 消息准备重试: messageId={}, retryCount={}/{}, delay={}ms", 
                message.getMessageId(), result.getRetryCount(), result.getMaxRetryCount(), 
                result.getRetryDelayMillis());
        
        // 这里可以实现延迟重试逻辑
        // 简单实现：拒绝消息重新入队（实际项目中可以使用延迟队列）
        channel.basicNack(deliveryTag, false, true);
    }
    
    /**
     * 确认消息
     */
    private void acknowledgeMessage(Channel channel, long deliveryTag, boolean ack) throws IOException {
        if (ack) {
            channel.basicAck(deliveryTag, false);
        } else {
            channel.basicNack(deliveryTag, false, true);
        }
    }
    
    /**
     * 获取消费者统计信息
     */
    public ConsumerStats getStats() {
        return ConsumerStats.builder()
                .totalConsumedCount(totalConsumedCount.get())
                .successConsumedCount(successConsumedCount.get())
                .failedConsumedCount(failedConsumedCount.get())
                .retryCount(retryCount.get())
                .registeredConsumerCount(messageConsumers.size())
                .startTime(startTime)
                .lastUpdateTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 获取注册的消费者信息
     */
    public List<String> getRegisteredConsumers() {
        return messageConsumers.stream()
                .map(consumer -> String.format("%s (supports: %s)", 
                        consumer.getConsumerId(),
                        consumer.getSupportedMessageTypes().length == 0 ? "ALL" : 
                        String.join(",", consumer.getSupportedMessageTypes())))
                .toList();
    }
}