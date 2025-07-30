package org.nan.cloud.message.infrastructure.websocket.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动订阅处理结果
 * 
 * 封装用户连接建立时自动订阅操作的处理结果。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutoSubscriptionResult {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 请求自动订阅的主题列表
     */
    private List<String> requestedTopics;
    
    /**
     * 权限验证通过的主题列表
     */
    private List<String> allowedTopics;
    
    /**
     * 成功订阅的主题列表
     */
    private List<String> successfulSubscriptions;
    
    /**
     * 订阅失败的主题列表
     */
    private List<String> failedSubscriptions;
    
    /**
     * 错误消息（处理异常时返回）
     */
    private String errorMessage;
    
    /**
     * 处理时间
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 是否处理成功
     */
    public boolean isSuccess() {
        return errorMessage == null && 
               (successfulSubscriptions != null && !successfulSubscriptions.isEmpty());
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (requestedTopics == null || requestedTopics.isEmpty()) {
            return 0.0;
        }
        
        int successCount = successfulSubscriptions != null ? successfulSubscriptions.size() : 0;
        return (double) successCount / requestedTopics.size();
    }
    
    /**
     * 获取处理摘要
     */
    public String getSummary() {
        if (errorMessage != null) {
            return String.format("自动订阅异常: %s", errorMessage);
        }
        
        int requestedCount = requestedTopics != null ? requestedTopics.size() : 0;
        int successCount = successfulSubscriptions != null ? successfulSubscriptions.size() : 0;
        int failedCount = failedSubscriptions != null ? failedSubscriptions.size() : 0;
        
        return String.format("自动订阅完成 - 请求: %d, 成功: %d, 失败: %d", 
                requestedCount, successCount, failedCount);
    }
}