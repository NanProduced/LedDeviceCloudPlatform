package org.nan.cloud.message.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.message.api.enums.MessageType;
import org.nan.cloud.message.api.enums.Priority;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket消息传输对象
 * 
 * 这个类定义了通过WebSocket传输的消息格式，包含了消息的所有必要信息
 * 如消息类型、内容、发送者、接收者等。使用Builder模式方便创建消息对象。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    /**
     * 消息唯一标识符
     * 用于消息去重、追踪和确认机制
     */
    private String messageId;
    
    /**
     * 消息类型
     * 定义消息的业务类型，如系统通知、设备告警、用户消息等
     * @see MessageType
     */
    private MessageType type;
    
    /**
     * 消息标题
     * 显示在通知栏的简短标题
     */
    private String title;
    
    /**
     * 消息内容
     * 具体的消息正文内容
     */
    private String content;
    
    /**
     * 消息优先级
     * 决定消息的显示顺序和处理优先级
     * @see Priority
     */
    private Priority priority;
    
    /**
     * 发送者用户ID
     * 消息的发送方，系统消息时为null
     */
    private String senderId;
    
    /**
     * 发送者姓名
     * 用于在界面上显示发送者信息
     */
    private String senderName;
    
    /**
     * 接收者用户ID
     * 目标接收用户，广播消息时为null
     */
    private String receiverId;
    
    /**
     * 组织ID
     * 用于多租户隔离，确保消息只在对应组织内传播
     */
    private String organizationId;
    
    /**
     * 消息创建时间
     * 使用ISO 8601格式进行JSON序列化
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 扩展数据
     * 存储业务相关的额外信息，如设备ID、任务ID等
     * 可以根据不同的消息类型存储不同的业务数据
     */
    private Map<String, Object> data;
    
    /**
     * 消息是否需要确认
     * true: 需要客户端确认收到消息
     * false: 不需要确认，发送即可
     */
    private Boolean requireAck;
    
    /**
     * 消息过期时间
     * 超过此时间的消息将被丢弃，避免发送过期信息
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    
    /**
     * 重试次数
     * 消息发送失败时的重试计数
     */
    private Integer retryCount;
    
    /**
     * 创建系统通知消息的便捷方法
     * 
     * @param title 消息标题
     * @param content 消息内容
     * @param organizationId 组织ID
     * @return 系统通知消息对象
     */
    public static WebSocketMessage createSystemNotification(String title, String content, String organizationId) {
        return WebSocketMessage.builder()
                .messageId(generateMessageId())
                .type(MessageType.SYSTEM_NOTIFICATION)
                .title(title)
                .content(content)
                .priority(Priority.NORMAL)
                .organizationId(organizationId)
                .timestamp(LocalDateTime.now())
                .requireAck(false)
                .retryCount(0)
                .build();
    }
    
    /**
     * 创建设备告警消息的便捷方法
     * 
     * @param title 告警标题
     * @param content 告警内容
     * @param deviceId 设备ID
     * @param organizationId 组织ID
     * @return 设备告警消息对象
     */
    public static WebSocketMessage createDeviceAlert(String title, String content, String deviceId, String organizationId) {
        return WebSocketMessage.builder()
                .messageId(generateMessageId())
                .type(MessageType.DEVICE_ALERT)
                .title(title)
                .content(content)
                .priority(Priority.HIGH)
                .organizationId(organizationId)
                .timestamp(LocalDateTime.now())
                .data(Map.of("deviceId", deviceId))
                .requireAck(true)
                .retryCount(0)
                .build();
    }
    
    /**
     * 生成消息唯一ID
     * 使用时间戳和随机数确保唯一性
     * 
     * @return 消息ID
     */
    private static String generateMessageId() {
        return System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }
}