package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 批量任务汇总信息
 * 
 * 当用户有大量任务完成时，提供汇总信息而不是逐个发送通知。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTaskSummary {
    
    /**
     * 批量任务ID
     */
    private String batchTaskId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 总任务数
     */
    private int totalTasks;
    
    /**
     * 成功任务数
     */
    private int successfulTasks;
    
    /**
     * 失败任务数
     */
    private int failedTasks;
    
    /**
     * 批量任务类型
     */
    private String batchType;
    
    /**
     * 批量任务描述
     */
    private String description;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
    
    /**
     * 总执行时长（毫秒）
     */
    private Long totalDurationMs;
    
    /**
     * 汇总数据
     */
    private Map<String, Object> summaryData;
    
    /**
     * 汇总描述信息
     */
    private String summary;
}