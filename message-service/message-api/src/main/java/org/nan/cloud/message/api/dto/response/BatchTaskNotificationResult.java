package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量任务通知结果
 * 
 * 表示批量任务结果通知的执行结果，包含统计信息和详细结果列表。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTaskNotificationResult {
    
    /**
     * 总任务数
     */
    private int totalTasks;
    
    /**
     * 成功通知数
     */
    private int successfulNotifications;
    
    /**
     * 失败通知数
     */
    private int failedNotifications;
    
    /**
     * 离线保存数
     */
    private int offlineNotifications;
    
    /**
     * 处理时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 详细结果列表
     */
    private List<TaskNotificationResult> details;
}