package org.nan.cloud.message.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.response.*;
import org.nan.cloud.message.service.TaskResultNotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务结果事件处理器
 * 
 * 监听任务执行完成事件，自动触发多重通知机制。
 * 这是LED设备云平台任务系统与消息中心的关键集成点。
 * 
 * 处理的事件类型：
 * - 单任务完成事件：设备控制、数据查询等单个任务完成
 * - 批量任务完成事件：批量操作、数据同步等批量任务完成
 * - 任务失败事件：需要高优先级通知的失败任务
 * - 长时间任务事件：需要进度通知的耗时任务
 * - 定时任务事件：系统定时任务的执行结果
 * 
 * 通知策略：
 * - 成功任务：标准通知流程
 * - 失败任务：高优先级通知 + 多重保障
 * - 批量任务：汇总通知，避免消息轰炸
 * - 进度任务：定期发送进度更新
 * - 关键任务：多通道通知确认
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "message.task-notification.enable", havingValue = "true", matchIfMissing = true)
public class TaskResultEventHandler {
    
    private final TaskResultNotificationService taskResultNotificationService;
    
    /**
     * 处理单任务完成事件
     * 
     * 单个任务执行完成时触发，是最常见的任务结果通知场景。
     * 根据任务状态和类型选择合适的通知策略。
     * 
     * @param event 单任务完成事件
     */
    @EventListener
    @Async("messageTaskExecutor")
    public void handleTaskCompletedEvent(TaskCompletedEvent event) {
        try {
            log.info("处理任务完成事件: taskId={}, userId={}, status={}, taskType={}", 
                    event.getTaskId(), event.getUserId(), event.getStatus(), event.getTaskType());
            
            // 1. 构建任务结果响应对象
            TaskResultResponse taskResult = buildTaskResultResponse(event);
            
            // 2. 根据任务状态选择通知策略
            if ("FAILED".equals(event.getStatus()) || "ERROR".equals(event.getStatus())) {
                // 失败任务：高优先级通知
                log.debug("任务失败，发送高优先级通知: taskId={}", event.getTaskId());
                taskResultNotificationService.notifyHighPriorityTaskResult(taskResult, true);
                
            } else if ("SUCCESS".equals(event.getStatus()) || "COMPLETED".equals(event.getStatus())) {
                // 成功任务：标准通知
                log.debug("任务成功完成，发送标准通知: taskId={}", event.getTaskId());
                taskResultNotificationService.notifyTaskResult(taskResult);
                
            } else {
                // 其他状态：标准通知
                log.debug("任务状态变更，发送标准通知: taskId={}, status={}", 
                         event.getTaskId(), event.getStatus());
                taskResultNotificationService.notifyTaskResult(taskResult);
            }
            
            log.debug("任务完成事件处理完毕: taskId={}", event.getTaskId());
            
        } catch (Exception e) {
            log.error("处理任务完成事件失败: taskId={}, error={}", 
                     event.getTaskId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理批量任务完成事件
     * 
     * 批量任务执行完成时触发，采用汇总通知策略避免消息过多。
     * 对于大量任务的批量操作，提供统一的执行结果汇总。
     * 
     * @param event 批量任务完成事件
     */
    @EventListener
    @Async("dataTaskExecutor")
    public void handleBatchTaskCompletedEvent(BatchTaskCompletedEvent event) {
        try {
            log.info("处理批量任务完成事件: batchTaskId={}, userId={}, totalTasks={}, successTasks={}, failedTasks={}", 
                    event.getBatchTaskId(), event.getUserId(), event.getTotalTasks(), 
                    event.getSuccessTasks(), event.getFailedTasks());
            
            // 1. 检查是否需要发送汇总通知
            if (event.shouldSendSummary()) {
                // 构建批量任务摘要
                BatchTaskSummary summary = 
                    new BatchTaskSummary();
                summary.setBatchTaskId(event.getBatchTaskId());
                summary.setTotalTasks(event.getTotalTasks());
                summary.setSuccessfulTasks(event.getSuccessTasks());
                summary.setFailedTasks(event.getFailedTasks());
                summary.setTotalDurationMs(event.getTotalDurationMs());
                summary.setSummary(buildBatchSummaryMessage(event));
                summary.setCompletedTime(event.getCompletedTime());
                
                // 发送汇总通知
                boolean sent = taskResultNotificationService.notifyBatchTaskSummary(
                    event.getBatchTaskId(), event.getUserId(), event.getOrganizationId(), summary);
                
                if (sent) {
                    log.info("批量任务汇总通知发送成功: batchTaskId={}", event.getBatchTaskId());
                } else {
                    log.warn("批量任务汇总通知发送失败: batchTaskId={}", event.getBatchTaskId());
                }
            } else {
                // 2. 发送个别任务通知
                if (event.getTaskResults() != null && !event.getTaskResults().isEmpty()) {
                    log.debug("批量任务不发送汇总，逐个通知: batchTaskId={}, count={}", 
                             event.getBatchTaskId(), event.getTaskResults().size());
                    
                    taskResultNotificationService.batchNotifyTaskResults(event.getTaskResults());
                }
            }
            
            log.debug("批量任务完成事件处理完毕: batchTaskId={}", event.getBatchTaskId());
            
        } catch (Exception e) {
            log.error("处理批量任务完成事件失败: batchTaskId={}, error={}", 
                     event.getBatchTaskId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理任务进度更新事件
     * 
     * 长时间运行的任务发送进度更新时触发，让用户了解任务执行状态。
     * 避免用户在等待过程中的焦虑，提升用户体验。
     * 
     * @param event 任务进度更新事件
     */
    @EventListener
    @Async("messageTaskExecutor")
    public void handleTaskProgressEvent(TaskProgressEvent event) {
        try {
            log.debug("处理任务进度事件: taskId={}, userId={}, progress={}, message={}", 
                     event.getTaskId(), event.getUserId(), event.getProgress(), event.getMessage());
            
            // 1. 检查进度通知配置
            if (!shouldSendProgressNotification(event)) {
                log.debug("跳过任务进度通知: taskId={}, progress={}", event.getTaskId(), event.getProgress());
                return;
            }
            
            // 2. 发送进度通知
            boolean sent = taskResultNotificationService.notifyTaskProgress(
                event.getTaskId(), event.getUserId(), event.getOrganizationId(), 
                event.getProgress(), event.getMessage());
            
            if (sent) {
                log.debug("任务进度通知发送成功: taskId={}, progress={}", 
                         event.getTaskId(), event.getProgress());
            } else {
                log.debug("任务进度通知发送失败: taskId={}, progress={}", 
                         event.getTaskId(), event.getProgress());
            }
            
        } catch (Exception e) {
            log.error("处理任务进度事件失败: taskId={}, progress={}, error={}", 
                     event.getTaskId(), event.getProgress(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理定时任务完成事件
     * 
     * 系统定时任务执行完成时触发，如数据备份、清理任务等。
     * 通常发送给系统管理员或相关运维人员。
     * 
     * @param event 定时任务完成事件
     */
    @EventListener
    @Async("dataTaskExecutor")
    public void handleScheduledTaskCompletedEvent(ScheduledTaskCompletedEvent event) {
        try {
            log.info("处理定时任务完成事件: taskName={}, status={}, executionTime={}", 
                    event.getTaskName(), event.getStatus(), event.getExecutionTime());
            
            // 1. 构建定时任务结果
            TaskResultResponse taskResult = buildScheduledTaskResultResponse(event);
            
            // 2. 定时任务通常发送给管理员
            if (event.getNotificationUserIds() != null && !event.getNotificationUserIds().isEmpty()) {
                for (String userId : event.getNotificationUserIds()) {
                    try {
                        taskResult.setUserId(userId);
                        
                        if ("FAILED".equals(event.getStatus())) {
                            // 定时任务失败：高优先级通知
                            taskResultNotificationService.notifyHighPriorityTaskResult(taskResult, true);
                        } else {
                            // 定时任务成功：标准通知
                            taskResultNotificationService.notifyTaskResult(taskResult);
                        }
                    } catch (Exception e) {
                        log.error("通知定时任务结果失败: taskName={}, userId={}, error={}", 
                                 event.getTaskName(), userId, e.getMessage(), e);
                    }
                }
            }
            
            log.debug("定时任务完成事件处理完毕: taskName={}", event.getTaskName());
            
        } catch (Exception e) {
            log.error("处理定时任务完成事件失败: taskName={}, error={}", 
                     event.getTaskName(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理关键任务完成事件
     * 
     * 系统关键任务完成时触发，需要确保通知送达。
     * 采用多重通知机制，必要时使用多种通知渠道。
     * 
     * @param event 关键任务完成事件
     */
    @EventListener
    @Async("messageTaskExecutor")
    public void handleCriticalTaskCompletedEvent(CriticalTaskCompletedEvent event) {
        try {
            log.info("处理关键任务完成事件: taskId={}, userId={}, taskType={}, status={}", 
                    event.getTaskId(), event.getUserId(), event.getTaskType(), event.getStatus());
            
            // 1. 构建任务结果响应对象
            TaskResultResponse taskResult = buildTaskResultResponse(event);
            taskResult.setPriority(5); // 最高优先级
            
            // 2. 关键任务始终使用高优先级通知
            TaskNotificationResult result =
                taskResultNotificationService.notifyHighPriorityTaskResult(taskResult, true);
            
            // 3. 如果通知失败，记录告警日志
            if (!result.isSuccess()) {
                log.error("关键任务通知失败，需要人工介入: taskId={}, userId={}, error={}", 
                         event.getTaskId(), event.getUserId(), result.getErrorMessage());
                
                // 可以在这里添加额外的告警机制，如：
                // - 发送告警邮件给管理员
                // - 记录到监控系统
                // - 触发紧急通知流程
            }
            
            log.info("关键任务完成事件处理完毕: taskId={}, notificationResult={}", 
                    event.getTaskId(), result.isSuccess());
            
        } catch (Exception e) {
            log.error("处理关键任务完成事件失败: taskId={}, error={}", 
                     event.getTaskId(), e.getMessage(), e);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建任务结果响应对象
     */
    private TaskResultResponse buildTaskResultResponse(TaskCompletedEvent event) {
        TaskResultResponse taskResult = new TaskResultResponse();
        taskResult.setTaskId(event.getTaskId());
        taskResult.setUserId(event.getUserId());
        taskResult.setOrganizationId(event.getOrganizationId());
        taskResult.setTaskType(event.getTaskType());
        taskResult.setTaskName(event.getTaskName());
        taskResult.setStatus(event.getStatus());
        taskResult.setProgress(event.getProgress());
        taskResult.setErrorMessage(event.getErrorMessage());
        taskResult.setCreatedTime(event.getCreatedTime());
        taskResult.setStartedTime(event.getStartedTime());
        taskResult.setCompletedTime(event.getCompletedTime());
        taskResult.setExecutionDurationMs(event.getExecutionDurationMs());
        
        // 类型安全转换 - 直接设置Object类型
        taskResult.setResultData(event.getResultData());
        
        taskResult.setPriority(event.getPriority());
        return taskResult;
    }
    
    /**
     * 构建定时任务结果响应对象
     */
    private TaskResultResponse buildScheduledTaskResultResponse(ScheduledTaskCompletedEvent event) {
        TaskResultResponse taskResult = new TaskResultResponse();
        taskResult.setTaskId(event.getTaskId());
        taskResult.setTaskType("SCHEDULED");
        taskResult.setTaskName(event.getTaskName());
        taskResult.setStatus(event.getStatus());
        taskResult.setProgress(100);
        taskResult.setErrorMessage(event.getErrorMessage());
        taskResult.setStartedTime(event.getStartTime());
        taskResult.setCompletedTime(event.getEndTime());
        taskResult.setExecutionDurationMs(event.getExecutionTime());
        taskResult.setResultData(event.getResultData());
        taskResult.setOrganizationId(event.getOrganizationId());
        return taskResult;
    }
    
    /**
     * 构建批量任务摘要消息
     */
    private String buildBatchSummaryMessage(BatchTaskCompletedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("批量任务执行完成：");
        sb.append("总任务数 ").append(event.getTotalTasks());
        sb.append("，成功 ").append(event.getSuccessTasks());
        sb.append("，失败 ").append(event.getFailedTasks());
        
        if (event.getTotalDurationMs() > 0) {
            sb.append("，耗时 ").append(event.getTotalDurationMs()).append("ms");
        }
        
        return sb.toString();
    }
    
    /**
     * 判断是否应该发送进度通知
     */
    private boolean shouldSendProgressNotification(TaskProgressEvent event) {
        // 1. 检查进度间隔（避免过于频繁）
        if (event.getProgress() % 10 != 0 && event.getProgress() != 100) {
            return false;
        }
        
        // 2. 检查任务类型（某些类型的任务不需要进度通知）
        if ("QUICK_TASK".equals(event.getTaskType())) {
            return false;
        }
        
        // 3. 检查用户在线状态（离线用户不需要进度通知）
        // 这里可以调用MessageService检查用户在线状态
        
        return true;
    }
    
    // ==================== 事件类定义 ====================
    
    /**
     * 任务完成事件
     */
    public static class TaskCompletedEvent {
        private String taskId;
        private String userId;
        private String organizationId;
        private String taskType;
        private String taskName;
        private String status;
        private Integer progress;
        private String errorMessage;
        private LocalDateTime createdTime;
        private LocalDateTime startedTime;
        private LocalDateTime completedTime;
        private Long executionDurationMs;
        private Object resultData;
        private Integer priority;
        
        // 构造方法和getter/setter
        public TaskCompletedEvent() {}
        
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
        public LocalDateTime getStartedTime() { return startedTime; }
        public void setStartedTime(LocalDateTime startedTime) { this.startedTime = startedTime; }
        public LocalDateTime getCompletedTime() { return completedTime; }
        public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
        public Long getExecutionDurationMs() { return executionDurationMs; }
        public void setExecutionDurationMs(Long executionDurationMs) { this.executionDurationMs = executionDurationMs; }
        public Object getResultData() { return resultData; }
        public void setResultData(Object resultData) { this.resultData = resultData; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }
    
    /**
     * 批量任务完成事件
     */
    public static class BatchTaskCompletedEvent {
        private String batchTaskId;
        private String userId;
        private String organizationId;
        private int totalTasks;
        private int successTasks;
        private int failedTasks;
        private long totalDurationMs;
        private LocalDateTime completedTime;
        private boolean sendSummary;
        private List<TaskResultResponse> taskResults;
        
        // 构造方法和getter/setter
        public BatchTaskCompletedEvent() {}
        
        public String getBatchTaskId() { return batchTaskId; }
        public void setBatchTaskId(String batchTaskId) { this.batchTaskId = batchTaskId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
        public int getSuccessTasks() { return successTasks; }
        public void setSuccessTasks(int successTasks) { this.successTasks = successTasks; }
        public int getFailedTasks() { return failedTasks; }
        public void setFailedTasks(int failedTasks) { this.failedTasks = failedTasks; }
        public long getTotalDurationMs() { return totalDurationMs; }
        public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
        public LocalDateTime getCompletedTime() { return completedTime; }
        public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
        public boolean shouldSendSummary() { return sendSummary; }
        public void setSendSummary(boolean sendSummary) { this.sendSummary = sendSummary; }
        public List<TaskResultResponse> getTaskResults() { return taskResults; }
        public void setTaskResults(List<TaskResultResponse> taskResults) { this.taskResults = taskResults; }
    }
    
    /**
     * 任务进度事件
     */
    public static class TaskProgressEvent {
        private String taskId;
        private String userId;
        private String organizationId;
        private String taskType;
        private int progress;
        private String message;
        private LocalDateTime timestamp;
        
        // 构造方法和getter/setter
        public TaskProgressEvent() {}
        
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 定时任务完成事件
     */
    public static class ScheduledTaskCompletedEvent {
        private String taskId;
        private String taskName;
        private String organizationId;
        private String status;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long executionTime;
        private Object resultData;
        private List<String> notificationUserIds;
        
        // 构造方法和getter/setter
        public ScheduledTaskCompletedEvent() {}
        
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
        public Object getResultData() { return resultData; }
        public void setResultData(Object resultData) { this.resultData = resultData; }
        public List<String> getNotificationUserIds() { return notificationUserIds; }
        public void setNotificationUserIds(List<String> notificationUserIds) { this.notificationUserIds = notificationUserIds; }
    }
    
    /**
     * 关键任务完成事件
     */
    public static class CriticalTaskCompletedEvent extends TaskCompletedEvent {
        private String criticalReason;
        private List<String> escalationUserIds;
        
        public String getCriticalReason() { return criticalReason; }
        public void setCriticalReason(String criticalReason) { this.criticalReason = criticalReason; }
        public List<String> getEscalationUserIds() { return escalationUserIds; }
        public void setEscalationUserIds(List<String> escalationUserIds) { this.escalationUserIds = escalationUserIds; }
    }
}