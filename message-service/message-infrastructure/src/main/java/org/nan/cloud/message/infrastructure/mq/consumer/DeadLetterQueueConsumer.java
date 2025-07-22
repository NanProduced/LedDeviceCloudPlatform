package org.nan.cloud.message.infrastructure.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.infrastructure.redis.manager.MessageCacheManager;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 死信队列消费者
 * 
 * 处理所有进入死信队列的失败消息，进行最终的错误处理、告警通知和数据记录。
 * 这是消息可靠性保障的最后一道防线，确保失败消息不会丢失且能得到适当处理。
 * 
 * 主要职责：
 * 1. 消费死信队列中的失败消息
 * 2. 记录详细的失败原因和处理过程
 * 3. 发送告警通知给运维人员
 * 4. 统计失败消息的类型和频率
 * 5. 支持失败消息的手动重试机制
 * 
 * 死信队列处理策略：
 * - 记录详细的失败日志
 * - 发送邮件/钉钉告警
 * - 更新Redis中的失败统计
 * - 为关键消息提供手动重试入口
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueueConsumer {
    
    private final MessageCacheManager messageCacheManager;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 处理消息死信队列
     * 
     * 所有消息相关的死信都会被路由到这个队列进行最终处理。
     * 包括通知消息、系统消息、用户消息的失败情况。
     * 
     * @param event 失败的消息事件
     * @param message RabbitMQ原始消息对象
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "message.failed.queue",
        ackMode = "MANUAL"
    )
    public void handleMessageFailedQueue(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.error("=== 消息死信队列处理开始 ===");
            log.error("死信消息ID: {}", event.getMessageId());
            log.error("事件类型: {}", event.getEventType());
            log.error("发送者: {}, 接收者: {}", event.getSenderId(), event.getReceiverId());
            log.error("消息标题: {}", event.getTitle());
            log.error("重试次数: {}, 最后错误: {}", event.getRetryCount(), event.getErrorMessage());
            log.error("消息内容: {}", truncateContent(event.getContent(), 200));
            
            // 1. 记录死信统计信息
            recordDeadLetterStats("MESSAGE", event);
            
            // 2. 发送告警通知
            sendFailureAlert("消息处理失败", event, "message.failed.queue");
            
            // 3. 存储失败消息详情（用于后续人工处理）
            storeFailedMessageDetails(event, message, "message.failed.queue");
            
            // 4. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.error("消息死信处理完成: messageId={}", event.getMessageId());
            log.error("=== 消息死信队列处理结束 ===");
            
        } catch (Exception e) {
            log.error("处理消息死信队列异常: messageId={}, error={}", 
                     event.getMessageId(), e.getMessage(), e);
            
            try {
                // 拒绝消息，避免无限循环
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝死信消息失败: {}", ioException.getMessage());
            }
        }
    }
    
    /**
     * 处理设备死信队列
     * 
     * 设备相关的死信，包括设备状态变更、告警推送等失败情况。
     * 
     * @param event 失败的设备事件
     * @param message RabbitMQ原始消息对象
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "device.failed.queue", 
        ackMode = "MANUAL"
    )
    public void handleDeviceFailedQueue(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.error("=== 设备死信队列处理开始 ===");
            log.error("死信消息ID: {}", event.getMessageId());
            log.error("设备事件类型: {}", event.getEventType());
            log.error("设备相关信息: {}", event.getMetadata());
            log.error("重试次数: {}, 最后错误: {}", event.getRetryCount(), event.getErrorMessage());
            
            // 1. 记录设备失败统计
            recordDeadLetterStats("DEVICE", event);
            
            // 2. 发送设备告警通知（高优先级）
            sendFailureAlert("设备事件处理失败", event, "device.failed.queue");
            
            // 3. 存储失败设备事件详情
            storeFailedMessageDetails(event, message, "device.failed.queue");
            
            // 4. 特殊处理：设备告警失败需要通过其他方式通知
            if (isDeviceAlertEvent(event)) {
                handleFailedDeviceAlert(event);
            }
            
            channel.basicAck(deliveryTag, false);
            log.error("设备死信处理完成: messageId={}", event.getMessageId());
            log.error("=== 设备死信队列处理结束 ===");
            
        } catch (Exception e) {
            log.error("处理设备死信队列异常: messageId={}, error={}", 
                     event.getMessageId(), e.getMessage(), e);
            
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝设备死信消息失败: {}", ioException.getMessage());
            }
        }
    }
    
    /**
     * 处理用户死信队列
     * 
     * 用户相关的死信，包括用户行为事件、权限变更等失败情况。
     * 
     * @param event 失败的用户事件
     * @param message RabbitMQ原始消息对象  
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "user.failed.queue",
        ackMode = "MANUAL"
    )
    public void handleUserFailedQueue(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.error("=== 用户死信队列处理开始 ===");
            log.error("死信消息ID: {}", event.getMessageId());
            log.error("用户事件类型: {}", event.getEventType());
            log.error("用户ID: {}, 组织ID: {}", event.getReceiverId(), event.getOrganizationId());
            log.error("重试次数: {}, 最后错误: {}", event.getRetryCount(), event.getErrorMessage());
            
            // 1. 记录用户失败统计
            recordDeadLetterStats("USER", event);
            
            // 2. 发送用户事件告警
            sendFailureAlert("用户事件处理失败", event, "user.failed.queue");
            
            // 3. 存储失败用户事件详情
            storeFailedMessageDetails(event, message, "user.failed.queue");
            
            channel.basicAck(deliveryTag, false);
            log.error("用户死信处理完成: messageId={}", event.getMessageId());
            log.error("=== 用户死信队列处理结束 ===");
            
        } catch (Exception e) {
            log.error("处理用户死信队列异常: messageId={}, error={}", 
                     event.getMessageId(), e.getMessage(), e);
            
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝用户死信消息失败: {}", ioException.getMessage());
            }
        }
    }
    
    /**
     * 处理业务死信队列
     * 
     * 业务相关的死信，包括任务处理、数据同步等失败情况。
     * 
     * @param event 失败的业务事件
     * @param message RabbitMQ原始消息对象
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "business.failed.queue",
        ackMode = "MANUAL"
    )
    public void handleBusinessFailedQueue(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.error("=== 业务死信队列处理开始 ===");
            log.error("死信消息ID: {}", event.getMessageId());
            log.error("业务事件类型: {}", event.getEventType());
            log.error("业务数据: {}", event.getMetadata());
            log.error("重试次数: {}, 最后错误: {}", event.getRetryCount(), event.getErrorMessage());
            
            // 1. 记录业务失败统计
            recordDeadLetterStats("BUSINESS", event);
            
            // 2. 发送业务事件告警（可能影响业务流程）
            sendFailureAlert("业务事件处理失败", event, "business.failed.queue");
            
            // 3. 存储失败业务事件详情
            storeFailedMessageDetails(event, message, "business.failed.queue");
            
            // 4. 特殊处理：关键业务事件失败需要立即人工介入
            if (isCriticalBusinessEvent(event)) {
                handleCriticalBusinessFailure(event);
            }
            
            channel.basicAck(deliveryTag, false);
            log.error("业务死信处理完成: messageId={}", event.getMessageId());
            log.error("=== 业务死信队列处理结束 ===");
            
        } catch (Exception e) {
            log.error("处理业务死信队列异常: messageId={}, error={}", 
                     event.getMessageId(), e.getMessage(), e);
            
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝业务死信消息失败: {}", ioException.getMessage());
            }
        }
    }
    
    /**
     * 处理系统死信队列
     * 
     * 系统级死信，包括系统通知、监控事件等失败情况。
     * 
     * @param event 失败的系统事件
     * @param message RabbitMQ原始消息对象
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "system.failed.queue",
        ackMode = "MANUAL"
    )
    public void handleSystemFailedQueue(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.error("=== 系统死信队列处理开始 ===");
            log.error("死信消息ID: {}", event.getMessageId());
            log.error("系统事件类型: {}", event.getEventType());
            log.error("系统通知内容: {}", truncateContent(event.getContent(), 200));
            log.error("重试次数: {}, 最后错误: {}", event.getRetryCount(), event.getErrorMessage());
            
            // 1. 记录系统失败统计
            recordDeadLetterStats("SYSTEM", event);
            
            // 2. 发送系统事件告警（最高优先级）
            sendFailureAlert("系统事件处理失败", event, "system.failed.queue");
            
            // 3. 存储失败系统事件详情
            storeFailedMessageDetails(event, message, "system.failed.queue");
            
            channel.basicAck(deliveryTag, false);
            log.error("系统死信处理完成: messageId={}", event.getMessageId());
            log.error("=== 系统死信队列处理结束 ===");
            
        } catch (Exception e) {
            log.error("处理系统死信队列异常: messageId={}, error={}", 
                     event.getMessageId(), e.getMessage(), e);
            
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝系统死信消息失败: {}", ioException.getMessage());
            }
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 记录死信统计信息
     * 
     * @param category 事件分类
     * @param event 失败事件
     */
    private void recordDeadLetterStats(String category, MessageEvent event) {
        try {
            String statsKey = "dead_letter_stats:" + category;
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 按日期统计死信数量
            messageCacheManager.incrementCounter(statsKey + ":" + today, 1, 24 * 60 * 60);
            
            // 按事件类型统计
            String eventTypeKey = statsKey + ":type:" + event.getEventType() + ":" + today;
            messageCacheManager.incrementCounter(eventTypeKey, 1, 24 * 60 * 60);
            
            // 按组织统计
            if (event.getOrganizationId() != null) {
                String orgKey = statsKey + ":org:" + event.getOrganizationId() + ":" + today;
                messageCacheManager.incrementCounter(orgKey, 1, 24 * 60 * 60);
            }
            
            log.info("死信统计记录完成: category={}, eventType={}, orgId={}", 
                    category, event.getEventType(), event.getOrganizationId());
                    
        } catch (Exception e) {
            log.error("记录死信统计失败: category={}, error={}", category, e.getMessage());
        }
    }
    
    /**
     * 发送失败告警通知
     * 
     * @param alertTitle 告警标题
     * @param event 失败事件
     * @param queueName 队列名称
     */
    private void sendFailureAlert(String alertTitle, MessageEvent event, String queueName) {
        try {
            String alertContent = String.format(
                "【消息队列告警】%s\n" +
                "队列: %s\n" +
                "消息ID: %s\n" +
                "事件类型: %s\n" +
                "发送者: %s\n" +
                "接收者: %s\n" +
                "组织ID: %s\n" +
                "重试次数: %d\n" +
                "最后错误: %s\n" +
                "时间: %s\n" +
                "消息内容: %s",
                alertTitle,
                queueName,
                event.getMessageId(),
                event.getEventType(),
                event.getSenderId(),
                event.getReceiverId(),
                event.getOrganizationId(),
                event.getRetryCount(),
                truncateContent(event.getErrorMessage(), 100),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                truncateContent(event.getContent(), 100)
            );
            
            // TODO: 集成邮件/钉钉/企微告警系统
            log.error("告警通知: {}", alertContent);
            
            // 暂时记录到Redis告警队列，由告警服务处理
            String alertKey = "alert:queue_failure:" + System.currentTimeMillis();
            messageCacheManager.cacheData(alertKey, alertContent, 24 * 60 * 60);
            
        } catch (Exception e) {
            log.error("发送失败告警异常: {}", e.getMessage());
        }
    }
    
    /**
     * 存储失败消息详情
     * 
     * @param event 失败事件
     * @param message RabbitMQ消息
     * @param queueName 队列名称
     */
    private void storeFailedMessageDetails(MessageEvent event, Message message, String queueName) {
        try {
            String detailsKey = "failed_message:" + event.getMessageId();
            String details = String.format(
                "{\n" +
                "  \"messageId\": \"%s\",\n" +
                "  \"queueName\": \"%s\",\n" +
                "  \"eventType\": \"%s\",\n" +
                "  \"senderId\": \"%s\",\n" +
                "  \"receiverId\": \"%s\",\n" +
                "  \"organizationId\": \"%s\",\n" +
                "  \"title\": \"%s\",\n" +
                "  \"content\": \"%s\",\n" +
                "  \"retryCount\": %d,\n" +
                "  \"errorMessage\": \"%s\",\n" +
                "  \"failedTime\": \"%s\",\n" +
                "  \"routingKey\": \"%s\",\n" +
                "  \"exchange\": \"%s\"\n" +
                "}",
                event.getMessageId(),
                queueName,
                event.getEventType(),
                event.getSenderId(),
                event.getReceiverId(),
                event.getOrganizationId(),
                escapeJsonString(event.getTitle()),
                escapeJsonString(truncateContent(event.getContent(), 500)),
                event.getRetryCount(),
                escapeJsonString(event.getErrorMessage()),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                message.getMessageProperties().getReceivedRoutingKey(),
                message.getMessageProperties().getReceivedExchange()
            );
            
            // 存储到Redis，保存30天用于后续人工处理
            messageCacheManager.cacheData(detailsKey, details, 30 * 24 * 60 * 60);
            
            log.info("失败消息详情已存储: messageId={}, detailsKey={}", event.getMessageId(), detailsKey);
            
        } catch (Exception e) {
            log.error("存储失败消息详情异常: messageId={}, error={}", event.getMessageId(), e.getMessage());
        }
    }
    
    /**
     * 判断是否为设备告警事件
     */
    private boolean isDeviceAlertEvent(MessageEvent event) {
        return event.getEventType() != null && 
               (event.getEventType().contains("DEVICE_ALERT") || 
                event.getEventType().contains("DEVICE_OFFLINE") ||
                event.getEventType().contains("DEVICE_ERROR"));
    }
    
    /**
     * 处理失败的设备告警
     */
    private void handleFailedDeviceAlert(MessageEvent event) {
        try {
            // 设备告警失败时，通过其他渠道（如短信、电话）通知
            log.error("设备告警失败，需要人工介入: messageId={}, deviceInfo={}", 
                     event.getMessageId(), event.getMetadata());
            
            // 存储到高优先级处理队列
            String urgentKey = "urgent:device_alert_failed:" + event.getMessageId();
            messageCacheManager.cacheData(urgentKey, event.toString(), 7 * 24 * 60 * 60);
            
        } catch (Exception e) {
            log.error("处理失败设备告警异常: {}", e.getMessage());
        }
    }
    
    /**
     * 判断是否为关键业务事件
     */
    private boolean isCriticalBusinessEvent(MessageEvent event) {
        return event.getEventType() != null &&
               (event.getEventType().contains("PAYMENT") ||
                event.getEventType().contains("ORDER") ||
                event.getEventType().contains("CRITICAL"));
    }
    
    /**
     * 处理关键业务失败
     */
    private void handleCriticalBusinessFailure(MessageEvent event) {
        try {
            log.error("关键业务事件处理失败，需要立即人工介入: messageId={}, businessData={}", 
                     event.getMessageId(), event.getMetadata());
            
            // 存储到紧急处理队列
            String criticalKey = "critical:business_failed:" + event.getMessageId();
            messageCacheManager.cacheData(criticalKey, event.toString(), 7 * 24 * 60 * 60);
            
        } catch (Exception e) {
            log.error("处理关键业务失败异常: {}", e.getMessage());
        }
    }
    
    /**
     * 截断内容长度
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}