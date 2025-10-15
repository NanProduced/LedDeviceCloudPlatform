package org.nan.cloud.message.infrastructure.websocket.dispatcher;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息分发结果 (Phase 2.4增强版)
 * 
 * 记录消息分发的执行结果和统计信息，支持聚合和动态路由统计。
 * 
 * Phase 2.4增强功能：
 * - 消息聚合统计
 * - 动态路由策略记录
 * - 分发类型标识
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
    
    // ==================== Phase 2.4 增强字段 ====================
    
    /**
     * 聚合消息数量（当消息被聚合时）
     * -- GETTER --
     *  获取聚合消息数量

     */
    private int aggregatedMessageCount = 0;
    
    /**
     * 分发类型
     * -- SETTER --
     *  设置分发类型
     * -- GETTER --
     *  获取分发类型


     */
    @Setter
    private DispatchType dispatchType = DispatchType.NORMAL;
    
    /**
     * 使用的路由策略
     * -- SETTER --
     *  设置路由策略
     * -- GETTER --
     *  获取路由策略


     */
    @Setter
    private String routingStrategy;
    
    /**
     * 是否使用了回退路由
     * -- GETTER --
     *  是否使用了回退路由

     */
    private boolean usedFallbackRouting = false;
    
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
    
    // ==================== Phase 2.4 增强方法 ====================
    
    /**
     * 设置聚合消息数量
     */
    public void setAggregatedMessageCount(int count) {
        this.aggregatedMessageCount = count;
        if (count > 1) {
            this.dispatchType = DispatchType.AGGREGATED;
        }
    }

    /**
     * 标记使用了回退路由
     */
    public void markUsedFallbackRouting() {
        this.usedFallbackRouting = true;
    }

    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建失败结果
     */
    public static DispatchResult failure(String messageId, String errorMessage) {
        DispatchResult result = new DispatchResult(messageId);
        result.success = false;
        result.errorMessage = errorMessage;
        result.dispatchType = DispatchType.FAILED;
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

    
    /**
     * 分发类型枚举
     */
    public enum DispatchType {
        NORMAL,      // 普通分发
        AGGREGATED,  // 聚合分发
        QUEUED,      // 已排队等待聚合
        FAILED       // 分发失败
    }
    
    @Override
    public String toString() {
        return String.format("DispatchResult{messageId='%s', dispatchType=%s, success=%s, successCount=%d, failureCount=%d, aggregatedMessageCount=%d, routingStrategy='%s', usedFallbackRouting=%s, duration=%dms}", 
                messageId, dispatchType, success, successCount, failureCount, aggregatedMessageCount, routingStrategy, usedFallbackRouting, getDurationMillis());
    }
}