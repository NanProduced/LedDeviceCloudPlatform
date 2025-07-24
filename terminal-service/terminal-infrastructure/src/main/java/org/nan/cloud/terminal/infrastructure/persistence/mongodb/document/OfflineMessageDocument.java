package org.nan.cloud.terminal.infrastructure.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * 离线消息存储文档
 * 
 * 当设备离线时，将消息持久化到MongoDB中，设备上线后推送。
 * 支持消息优先级排序和自动过期清理。
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "offline_messages")
public class OfflineMessageDocument {
    
    /**
     * 消息ID（主键）
     */
    @Id
    private String messageId;
    
    /**
     * 终端ID（索引）
     */
    @Indexed
    private String did;
    
    /**
     * 组织ID（索引）
     */
    @Indexed
    private String oid;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 消息内容（JSON格式）
     */
    private String messageContent;
    
    /**
     * 发送者ID
     */
    private String senderId;
    
    /**
     * 消息优先级（1-10，10最高）
     */
    @Indexed
    private Integer priority;
    
    /**
     * 消息状态
     */
    @Indexed
    private MessageStatus status;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;
    
    /**
     * 创建时间（索引，用于排序）
     */
    @Indexed
    private LocalDateTime createTime;
    
    /**
     * 过期时间（TTL索引）
     */
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expireTime;
    
    /**
     * 最后推送时间
     */
    private LocalDateTime lastPushTime;
    
    /**
     * 推送成功时间
     */
    private LocalDateTime deliveredTime;
    
    /**
     * 业务跟踪ID
     */
    private String traceId;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 消息状态枚举
     */
    public enum MessageStatus {
        PENDING,        // 待推送
        PUSHING,        // 推送中
        DELIVERED,      // 已投递
        FAILED,         // 推送失败
        EXPIRED         // 已过期
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
     * 标记为推送中状态
     */
    public void markAsPushing() {
        this.status = MessageStatus.PUSHING;
        this.lastPushTime = LocalDateTime.now();
    }
    
    /**
     * 标记为已投递状态
     */
    public void markAsDelivered() {
        this.status = MessageStatus.DELIVERED;
        this.deliveredTime = LocalDateTime.now();
    }
    
    /**
     * 标记为失败状态
     */
    public void markAsFailed(String errorMessage) {
        this.status = MessageStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}