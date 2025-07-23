package org.nan.cloud.common.mq.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息发送结果
 * 
 * 封装单条消息的发送结果信息。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 发送是否成功
     */
    private boolean success;
    
    /**
     * 交换机名称
     */
    private String exchange;
    
    /**
     * 路由键
     */
    private String routingKey;
    
    /**
     * 发送时间
     */
    @Builder.Default
    private LocalDateTime sendTime = LocalDateTime.now();
    
    /**
     * 耗时（毫秒）
     */
    private long duration;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 关联数据ID（用于发布确认）
     */
    private String correlationId;
    
    /**
     * 是否被路由到队列
     */
    private boolean routed;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 创建成功结果
     */
    public static SendResult success(String messageId, String exchange, String routingKey, long duration) {
        return SendResult.builder()
                .messageId(messageId)
                .success(true)
                .exchange(exchange)
                .routingKey(routingKey)
                .duration(duration)
                .routed(true)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static SendResult failure(String messageId, String exchange, String routingKey, 
                                   String errorCode, String errorMessage, long duration) {
        return SendResult.builder()
                .messageId(messageId)
                .success(false)
                .exchange(exchange)
                .routingKey(routingKey)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .duration(duration)
                .routed(false)
                .build();
    }
    
    /**
     * 创建未路由结果
     */
    public static SendResult unrouted(String messageId, String exchange, String routingKey, long duration) {
        return SendResult.builder()
                .messageId(messageId)
                .success(false)
                .exchange(exchange)
                .routingKey(routingKey)
                .errorCode("MESSAGE_UNROUTED")
                .errorMessage("消息未被路由到任何队列")
                .duration(duration)
                .routed(false)
                .build();
    }
    
    /**
     * 获取结果描述
     */
    public String getDescription() {
        if (success) {
            return String.format("发送成功: messageId=%s, exchange=%s, routingKey=%s, duration=%dms", 
                    messageId, exchange, routingKey, duration);
        } else {
            return String.format("发送失败: messageId=%s, exchange=%s, routingKey=%s, error=%s, duration=%dms", 
                    messageId, exchange, routingKey, errorMessage, duration);
        }
    }
}