package org.nan.cloud.message.infrastructure.websocket.dispatcher;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息分发结果
 * 
 * 记录消息分发的执行结果和统计信息。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Getter
public class DispatchResult {
    
    /**
     * 消息ID
     */
    private final String messageId;
    
    /**
     * 分发开始时间
     */
    private final LocalDateTime startTime = LocalDateTime.now();
    
    /**
     * 分发结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 成功分发数量
     */
    private int successCount = 0;
    
    /**
     * 失败分发数量
     */
    private int failureCount = 0;
    
    /**
     * 成功分发的主题列表
     */
    private final List<String> successfulTopics = new ArrayList<>();
    
    /**
     * 失败分发的主题及错误信息
     */
    private final Map<String, String> failedTopics = new HashMap<>();
    
    /**
     * 是否分发成功
     */
    private boolean success = true;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    public DispatchResult(String messageId) {
        this.messageId = messageId;
    }
    
    /**
     * 增加成功数量
     */
    public void incrementSuccessCount() {
        this.successCount++;
    }
    
    /**
     * 增加失败数量
     */
    public void incrementFailureCount() {
        this.failureCount++;
        this.success = false;
    }
    
    /**
     * 添加成功的主题
     */
    public void addSuccessfulTopic(String topic) {
        this.successfulTopics.add(topic);
    }
    
    /**
     * 添加失败的主题及错误信息
     */
    public void addFailedTopic(String topic, String error) {
        this.failedTopics.put(topic, error);
    }
    
    /**
     * 标记分发完成
     */
    public void markCompleted() {
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 获取分发耗时（毫秒）
     */
    public long getDurationMillis() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }
    
    /**
     * 获取总分发数量
     */
    public int getTotalCount() {
        return successCount + failureCount;
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        int total = getTotalCount();
        return total > 0 ? (double) successCount / total : 0.0;
    }
    
    /**
     * 创建失败结果
     */
    public static DispatchResult failure(String messageId, String errorMessage) {
        DispatchResult result = new DispatchResult(messageId);
        result.success = false;
        result.errorMessage = errorMessage;
        result.markCompleted();
        return result;
    }
    
    /**
     * 创建成功结果
     */
    public static DispatchResult success(String messageId, List<String> successfulTopics) {
        DispatchResult result = new DispatchResult(messageId);
        result.successfulTopics.addAll(successfulTopics);
        result.successCount = successfulTopics.size();
        result.markCompleted();
        return result;
    }
}