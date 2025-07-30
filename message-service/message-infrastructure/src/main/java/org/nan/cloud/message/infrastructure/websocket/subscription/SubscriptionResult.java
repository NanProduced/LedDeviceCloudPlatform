package org.nan.cloud.message.infrastructure.websocket.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.message.infrastructure.websocket.routing.SubscriptionLevel;

import java.time.LocalDateTime;

/**
 * 订阅处理结果
 * 
 * 封装单个订阅/取消订阅操作的处理结果。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResult {
    
    /**
     * 处理是否成功
     */
    private boolean success;
    
    /**
     * 处理的主题路径
     */
    private String topicPath;
    
    /**
     * 订阅层次（订阅成功时返回）
     */
    private SubscriptionLevel subscriptionLevel;
    
    /**
     * 处理结果消息
     */
    private String message;
    
    /**
     * 错误代码（处理失败时返回）
     */
    private String errorCode;
    
    /**
     * 处理时间
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 创建成功结果
     */
    public static SubscriptionResult success(String topicPath, SubscriptionLevel subscriptionLevel) {
        return SubscriptionResult.builder()
                .success(true)
                .topicPath(topicPath)
                .subscriptionLevel(subscriptionLevel)
                .message("订阅处理成功")
                .build();
    }
    
    /**
     * 创建权限拒绝结果
     */
    public static SubscriptionResult denied(String reason) {
        return SubscriptionResult.builder()
                .success(false)
                .message(reason)
                .errorCode("PERMISSION_DENIED")
                .build();
    }
    
    /**
     * 创建错误结果
     */
    public static SubscriptionResult error(String errorMessage) {
        return SubscriptionResult.builder()
                .success(false)
                .message(errorMessage)
                .errorCode("PROCESSING_ERROR")
                .build();
    }
}