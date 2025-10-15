package org.nan.cloud.common.mq.core.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 通用消息抽象
 * 
 * 封装消息的基本信息，提供统一的消息格式。
 * 适用于各种业务场景的消息传递。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    /**
     * 消息唯一标识
     */
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 消息主题/标题
     */
    private String subject;
    
    /**
     * 消息内容
     */
    private Object payload;
    
    /**
     * 发送者标识
     */
    private String senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 接收者标识（可为空，表示广播消息）
     */
    private String receiverId;
    
    /**
     * 组织标识
     */
    private String organizationId;
    
    /**
     * 消息优先级（1-10，10为最高优先级）
     */
    @Builder.Default
    private Integer priority = 5;
    
    /**
     * 消息创建时间
     */
    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime = LocalDateTime.now();
    
    /**
     * 消息过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    
    /**
     * 路由键
     */
    private String routingKey;
    
    /**
     * 交换机名称
     */
    private String exchange;
    
    /**
     * 队列名称
     */
    private String queue;
    
    /**
     * 消息属性/标签
     */
    private Map<String, Object> properties;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Builder.Default
    private Integer maxRetryCount = 3;
    
    /**
     * 消息状态
     */
    @Builder.Default
    private MessageStatus status = MessageStatus.PENDING;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 消息来源系统
     */
    private String sourceSystem;
    
    /**
     * 消息目标系统
     */
    private String targetSystem;
    
    /**
     * 业务跟踪ID
     */
    private String traceId;
    
    /**
     * 消息状态枚举
     */
    public enum MessageStatus {
        PENDING,    // 待发送
        SENT,       // 已发送
        DELIVERED,  // 已投递
        CONSUMED,   // 已消费
        FAILED,     // 失败
        EXPIRED     // 已过期
    }
    
    /**
     * 检查消息是否过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetryCount && !isExpired();
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        if (this.retryCount >= this.maxRetryCount) {
            this.status = MessageStatus.FAILED;
        }
    }
    
    /**
     * 标记消息为失败状态
     */
    public void markAsFailed(String errorMessage) {
        this.status = MessageStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    /**
     * 标记消息为已发送状态
     */
    public void markAsSent() {
        this.status = MessageStatus.SENT;
    }
    
    /**
     * 标记消息为已消费状态
     */
    public void markAsConsumed() {
        this.status = MessageStatus.CONSUMED;
    }
    
    /**
     * 获取消息的简要描述
     */
    public String getDescription() {
        return String.format("Message[id=%s, type=%s, from=%s, to=%s, status=%s]", 
                messageId, messageType, senderId, receiverId, status);
    }
    
    /**
     * 创建通知消息
     */
    public static Message notification(String subject, Object payload, String receiverId, String organizationId) {
        return Message.builder()
                .messageType("NOTIFICATION")
                .subject(subject)
                .payload(payload)
                .receiverId(receiverId)
                .organizationId(organizationId)
                .priority(7)
                .build();
    }
    
    /**
     * 创建系统消息
     */
    public static Message system(String subject, Object payload, String organizationId) {
        return Message.builder()
                .messageType("SYSTEM")
                .subject(subject)
                .payload(payload)
                .organizationId(organizationId)
                .priority(8)
                .build();
    }
    
    /**
     * 创建用户消息
     */
    public static Message userMessage(String subject, Object payload, String senderId, String receiverId, String organizationId) {
        return Message.builder()
                .messageType("USER_MESSAGE")
                .subject(subject)
                .payload(payload)
                .senderId(senderId)
                .receiverId(receiverId)
                .organizationId(organizationId)
                .priority(5)
                .build();
    }
    
    /**
     * 创建广播消息
     */
    public static Message broadcast(String subject, Object payload, String senderId, String organizationId) {
        return Message.builder()
                .messageType("BROADCAST")
                .subject(subject)
                .payload(payload)
                .senderId(senderId)
                .organizationId(organizationId)
                .priority(6)
                .build();
    }
    
    /**
     * 创建事件消息
     */
    public static Message event(String eventType, Object eventData, String sourceSystem, String targetSystem) {
        return Message.builder()
                .messageType("EVENT")
                .subject(eventType)
                .payload(eventData)
                .sourceSystem(sourceSystem)
                .targetSystem(targetSystem)
                .priority(8)
                .build();
    }
}