package org.nan.cloud.message.infrastructure.aggregation;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批量指令聚合数据模型
 * 
 * 用于存储批量指令执行过程中的聚合信息，包括：
 * 1. 执行进度统计
 * 2. 设备执行状态汇总
 * 3. 错误信息收集
 * 4. 性能指标统计
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
public class BatchCommandAggregationData {
    
    /**
     * 批量任务ID
     */
    private String batchId;
    
    /**
     * 任务ID（可选，用于任务跟踪）
     */
    private String taskId;
    
    /**
     * 组织ID
     */
    private Long orgId;
    
    /**
     * 创建用户ID
     */
    private Long userId;
    
    /**
     * 批量任务创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;
    
    /**
     * 任务状态
     */
    private BatchStatus status;
    
    /**
     * 总设备数
     */
    private Integer totalCount;
    
    /**
     * 已完成设备数
     */
    private Integer completedCount;
    
    /**
     * 成功执行设备数
     */
    private Integer successCount;
    
    /**
     * 失败设备数
     */
    private Integer failureCount;
    
    /**
     * 超时设备数
     */
    private Integer timeoutCount;
    
    /**
     * 跳过设备数
     */
    private Integer skippedCount;
    
    /**
     * 完成百分比（0-100）
     */
    private Double completionPercentage;
    
    /**
     * 成功率（0-100）
     */
    private Double successRate;
    
    /**
     * 平均执行时间（毫秒）
     */
    private Long averageExecutionTime;
    
    /**
     * 预计剩余时间（毫秒）
     */
    private Long estimatedRemainingTime;
    
    /**
     * 设备执行详情列表
     */
    private List<DeviceExecutionDetail> deviceDetails;
    
    /**
     * 错误统计
     */
    private Map<String, Integer> errorStatistics;
    
    /**
     * 性能指标
     */
    private PerformanceMetrics performanceMetrics;
    
    /**
     * 扩展数据
     */
    private Map<String, Object> extensionData;
    
    /**
     * 批量任务状态枚举
     */
    public enum BatchStatus {
        PENDING("待执行"),
        RUNNING("执行中"),
        COMPLETED("已完成"),
        FAILED("执行失败"),
        CANCELLED("已取消"),
        TIMEOUT("执行超时"),
        PARTIAL("部分完成");
        
        private final String description;
        
        BatchStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 设备执行详情
     */
    @Data
    @Builder
    public static class DeviceExecutionDetail {
        private String deviceId;
        private String commandId;
        private String status;
        private String result;
        private String errorCode;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long executionTime;
        private Integer retryCount;
        private Map<String, Object> resultData;
    }
    
    /**
     * 性能指标
     */
    @Data
    @Builder
    public static class PerformanceMetrics {
        private Long totalExecutionTime;
        private Long minExecutionTime;
        private Long maxExecutionTime;
        private Double averageExecutionTime;
        private Integer throughputPerSecond;
        private Integer concurrentExecutions;
        private LocalDateTime firstCommandTime;
        private LocalDateTime lastCommandTime;
    }
    
    /**
     * 计算完成百分比
     */
    public void calculateCompletionPercentage() {
        if (totalCount != null && totalCount > 0 && completedCount != null) {
            this.completionPercentage = (double) completedCount / totalCount * 100.0;
        } else {
            this.completionPercentage = 0.0;
        }
    }
    
    /**
     * 计算成功率
     */
    public void calculateSuccessRate() {
        if (completedCount != null && completedCount > 0 && successCount != null) {
            this.successRate = (double) successCount / completedCount * 100.0;
        } else {
            this.successRate = 0.0;
        }
    }
    
    /**
     * 检查任务是否已完成
     */
    public boolean isCompleted() {
        return status == BatchStatus.COMPLETED || 
               status == BatchStatus.FAILED || 
               status == BatchStatus.CANCELLED ||
               status == BatchStatus.TIMEOUT ||
               (totalCount != null && completedCount != null && completedCount.equals(totalCount));
    }
    
    /**
     * 检查任务是否成功完成
     */
    public boolean isSuccessfullyCompleted() {
        return status == BatchStatus.COMPLETED && 
               totalCount != null && successCount != null && 
               totalCount.equals(successCount);
    }
    
    /**
     * 检查任务是否部分完成
     */
    public boolean isPartiallyCompleted() {
        return status == BatchStatus.PARTIAL || 
               (isCompleted() && successCount != null && totalCount != null && 
                successCount > 0 && successCount < totalCount);
    }
}