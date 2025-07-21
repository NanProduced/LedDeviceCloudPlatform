package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务通知指标统计
 * 
 * 提供任务通知系统的性能指标和统计信息，用于监控和分析。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotificationMetrics {
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 统计开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 统计结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 总通知数
     */
    private long totalNotifications;
    
    /**
     * 成功通知数
     */
    private long successfulNotifications;
    
    /**
     * 失败通知数
     */
    private long failedNotifications;
    
    /**
     * 离线保存数
     */
    private long offlineNotifications;
    
    /**
     * 会话级通知数
     */
    private long sessionLevelNotifications;
    
    /**
     * 用户级通知数
     */
    private long userLevelNotifications;
    
    /**
     * 平均响应时间（毫秒）
     */
    private double avgResponseTimeMs;
    
    /**
     * 最大响应时间（毫秒）
     */
    private long maxResponseTimeMs;
    
    /**
     * 通知成功率
     */
    private double successRate;
    
    /**
     * 按任务类型统计
     */
    private Map<String, Long> notificationsByTaskType;
    
    /**
     * 按小时统计
     */
    private Map<Integer, Long> notificationsByHour;
    
    /**
     * 扩展指标
     */
    private Map<String, Object> additionalMetrics;
}