package org.nan.cloud.common.mq.consumer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息消费结果
 * 
 * 封装消息消费的结果信息，用于确定消息的后续处理方式。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeResult {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消费者ID
     */
    private String consumerId;
    
    /**
     * 消费是否成功
     */
    private boolean success;
    
    /**
     * 消费时间
     */
    @Builder.Default
    private LocalDateTime consumeTime = LocalDateTime.now();
    
    /**
     * 消费耗时（毫秒）
     */
    private long duration;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 异常对象
     */
    private Exception exception;
    
    /**
     * 消费动作
     */
    @Builder.Default
    private ConsumeAction action = ConsumeAction.ACK;
    
    /**
     * 是否需要重试
     */
    private boolean needRetry;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetryCount = 3;
    
    /**
     * 重试延迟时间（毫秒）
     */
    @Builder.Default  
    private long retryDelayMillis = 1000;
    
    /**
     * 处理结果数据
     */
    private Object resultData;
    
    /**
     * 消费动作枚举
     */
    public enum ConsumeAction {
        ACK,        // 确认消息
        NACK,       // 拒绝消息，重新入队
        REJECT,     // 拒绝消息，不重新入队
        RETRY       // 重试消费
    }
    
    /**
     * 创建成功结果
     */
    public static ConsumeResult success(String messageId, String consumerId, long duration) {
        return ConsumeResult.builder()
                .messageId(messageId)
                .consumerId(consumerId)
                .success(true)
                .duration(duration)
                .action(ConsumeAction.ACK)
                .build();
    }
    
    /**
     * 创建成功结果（带结果数据）
     */
    public static ConsumeResult success(String messageId, String consumerId, long duration, Object resultData) {
        return ConsumeResult.builder()
                .messageId(messageId)
                .consumerId(consumerId)
                .success(true)
                .duration(duration)
                .action(ConsumeAction.ACK)
                .resultData(resultData)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static ConsumeResult failure(String messageId, String consumerId, 
                                      String errorCode, String errorMessage, Exception exception) {
        return ConsumeResult.builder()
                .messageId(messageId)
                .consumerId(consumerId)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .exception(exception)
                .action(ConsumeAction.NACK)
                .needRetry(true)
                .build();
    }
    
    /**
     * 创建重试结果
     */
    public static ConsumeResult retry(String messageId, String consumerId, 
                                    String errorMessage, int retryCount, int maxRetryCount) {
        return ConsumeResult.builder()
                .messageId(messageId)
                .consumerId(consumerId)
                .success(false)
                .errorMessage(errorMessage)
                .action(ConsumeAction.RETRY)
                .needRetry(true)
                .retryCount(retryCount)
                .maxRetryCount(maxRetryCount)
                .build();
    }
    
    /**
     * 创建拒绝结果（不重新入队）
     */
    public static ConsumeResult reject(String messageId, String consumerId, 
                                     String errorCode, String errorMessage) {
        return ConsumeResult.builder()
                .messageId(messageId)
                .consumerId(consumerId)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .action(ConsumeAction.REJECT)
                .needRetry(false)
                .build();
    }
    
    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return needRetry && retryCount < maxRetryCount;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        if (this.retryCount >= this.maxRetryCount) {
            this.needRetry = false;
            this.action = ConsumeAction.REJECT;
        }
    }
    
    /**
     * 获取结果描述
     */
    public String getDescription() {
        if (success) {
            return String.format("消费成功: messageId=%s, consumerId=%s, duration=%dms", 
                    messageId, consumerId, duration);
        } else {
            return String.format("消费失败: messageId=%s, consumerId=%s, error=%s, action=%s", 
                    messageId, consumerId, errorMessage, action);
        }
    }
}