package org.nan.cloud.message.api.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.message.api.enums.MessageType;
import org.nan.cloud.message.api.enums.Priority;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息事件模型
 * 
 * 用于RabbitMQ消息传输的事件模型，包含完整的消息信息和元数据
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {
    
    /**
     * 事件唯一标识
     */
    private String eventId;
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型
     */
    private MessageType messageType;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息优先级
     */
    private Priority priority;
    
    /**
     * 发送者ID
     */
    private String senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 接收者ID
     */
    private String receiverId;
    
    /**
     * 接收者类型 (USER, GROUP, ORGANIZATION, ALL)
     */
    private String receiverType;
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 消息创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 扩展数据
     */
    private Map<String, Object> data;
    
    /**
     * 是否需要确认
     */
    private Boolean requireAck;
    
    /**
     * 消息过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;
    
    /**
     * 路由键
     */
    private String routingKey;
    
    /**
     * 事件来源
     */
    private String source;
    
    /**
     * 事件版本
     */
    private String version;

    /**
     * 消息扩展元数据
     *
     * 存储特定类型消息的详细信息，如：
     * - 设备告警：deviceId, deviceName, alertType, currentValue, threshold等
     * - 任务结果：taskType, resultData, exportUrl等
     * - 系统通知：noticeType, effectiveTime, expireTime等
     */
    private Map<String, Object> metadata;
    
    /**
     * 是否已处理
     */
    private Boolean processed;
    
    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedTime;
    
    /**
     * 处理结果
     */
    private String processResult;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建通知类型的消息事件
     */
    public static MessageEvent notification(String messageId, String title, String content, 
                                          String receiverId, String organizationId) {
        return MessageEvent.builder()
                .eventId(generateEventId())
                .eventType("MESSAGE_NOTIFICATION")
                .messageId(messageId)
                .messageType(MessageType.BUSINESS_REMINDER)  // 使用业务提醒类型作为通知
                .title(title)
                .content(content)
                .priority(Priority.NORMAL)
                .receiverId(receiverId)
                .receiverType("USER")
                .organizationId(organizationId)
                .timestamp(LocalDateTime.now())
                .requireAck(false)
                .retryCount(0)
                .maxRetryCount(3)
                .routingKey("message.notification")
                .source("message-service")
                .version("1.0")
                .processed(false)
                .build();
    }
    
    /**
     * 创建系统类型的消息事件
     */
    public static MessageEvent system(String messageId, String title, String content, 
                                    String organizationId) {
        return MessageEvent.builder()
                .eventId(generateEventId())
                .eventType("MESSAGE_SYSTEM")
                .messageId(messageId)
                .messageType(MessageType.SYSTEM_NOTIFICATION)
                .title(title)
                .content(content)
                .priority(Priority.HIGH)
                .receiverType("ORGANIZATION")
                .organizationId(organizationId)
                .timestamp(LocalDateTime.now())
                .requireAck(false)
                .retryCount(0)
                .maxRetryCount(3)
                .routingKey("message.system")
                .source("message-service")
                .version("1.0")
                .processed(false)
                .build();
    }
    
    /**
     * 创建用户消息事件
     */
    public static MessageEvent userMessage(String messageId, String title, String content,
                                         String senderId, String senderName, 
                                         String receiverId, String organizationId) {
        return MessageEvent.builder()
                .eventId(generateEventId())
                .eventType("MESSAGE_USER")
                .messageId(messageId)
                .messageType(MessageType.USER_MESSAGE)
                .title(title)
                .content(content)
                .priority(Priority.NORMAL)
                .senderId(senderId)
                .senderName(senderName)
                .receiverId(receiverId)
                .receiverType("USER")
                .organizationId(organizationId)
                .timestamp(LocalDateTime.now())
                .requireAck(true)
                .retryCount(0)
                .maxRetryCount(3)
                .routingKey("message.user")
                .source("message-service")
                .version("1.0")
                .processed(false)
                .build();
    }
    
    /**
     * 创建广播消息事件
     */
    public static MessageEvent broadcast(String messageId, String title, String content,
                                       String senderId, String senderName, String organizationId) {
        return MessageEvent.builder()
                .eventId(generateEventId())
                .eventType("MESSAGE_BROADCAST")
                .messageId(messageId)
                .messageType(MessageType.SYSTEM_NOTIFICATION)  // 广播消息使用系统通知类型
                .title(title)
                .content(content)
                .priority(Priority.NORMAL)
                .senderId(senderId)
                .senderName(senderName)
                .receiverType("ALL")
                .organizationId(organizationId)
                .timestamp(LocalDateTime.now())
                .requireAck(false)
                .retryCount(0)
                .maxRetryCount(3)
                .routingKey("message.broadcast")
                .source("message-service")
                .version("1.0")
                .processed(false)
                .build();
    }
    
    /**
     * 标记事件为已处理
     */
    public void markAsProcessed(String result) {
        this.processed = true;
        this.processedTime = LocalDateTime.now();
        this.processResult = result;
    }
    
    /**
     * 标记事件处理失败
     */
    public void markAsFailed(String errorMessage) {
        this.processed = false;
        this.processedTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount != null && this.maxRetryCount != null && 
               this.retryCount < this.maxRetryCount;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
    
    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return this.expireTime != null && LocalDateTime.now().isAfter(this.expireTime);
    }
    
    /**
     * 生成事件ID
     */
    private static String generateEventId() {
        return "event-" + System.currentTimeMillis() + "-" + 
               (int)(Math.random() * 1000);
    }

    public void putMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public void putMetadata(Map<String, Object> map) {
        if (this.metadata == null) {
            this.metadata = map;
        }
        else {
            this.metadata.putAll(map);
        }
    }

    public void clearMetadata() {
        if (this.metadata != null) {
            this.metadata.clear();
        }
    }
}