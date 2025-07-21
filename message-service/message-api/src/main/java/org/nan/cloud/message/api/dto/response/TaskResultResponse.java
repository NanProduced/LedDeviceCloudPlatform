package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务结果响应
 * 
 * 包含任务执行的完整信息，结合MySQL基础数据和MongoDB详细数据。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultResponse {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 任务进度
     */
    private Integer progress;
    
    /**
     * 任务优先级
     */
    private Integer priority;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startedTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
    
    /**
     * 结果是否已查看
     */
    private Boolean isResultViewed;
    
    /**
     * 通知状态
     */
    private String notificationStatus;
    
    /**
     * 执行结果数据（支持任意类型）
     */
    private Object resultData;
    
    /**
     * 执行参数
     */
    private Map<String, Object> executionParams;
    
    /**
     * 执行日志
     */
    private List<String> executionLogs;
    
    /**
     * 性能指标
     */
    private Map<String, Object> performanceMetrics;
    
    /**
     * 输出文件
     */
    private List<String> outputFiles;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionDurationMs;
    
}