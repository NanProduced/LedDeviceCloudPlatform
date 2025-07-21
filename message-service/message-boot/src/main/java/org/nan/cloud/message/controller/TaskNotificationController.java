package org.nan.cloud.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.response.ApiResponse;
import org.nan.cloud.message.api.dto.response.*;
import org.nan.cloud.message.service.TaskResultNotificationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务结果通知控制器
 * 
 * 提供任务结果多重通知功能的REST API接口，支持手动触发通知、查询通知状态、
 * 管理通知配置等功能。主要用于管理员操作和系统集成。
 * 
 * 主要功能：
 * - 手动触发任务结果通知
 * - 批量任务通知管理
 * - 通知状态查询和统计
 * - 通知配置管理
 * - 离线通知管理
 * - 通知性能监控
 * 
 * 权限要求：
 * - 管理员权限：通知配置管理、批量操作
 * - 用户权限：个人任务通知查询
 * - 系统权限：自动通知触发、监控查询
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/task-notification")
@RequiredArgsConstructor
@Validated
@Tag(name = "任务结果通知", description = "任务结果多重通知管理API")
public class TaskNotificationController {
    
    private final TaskResultNotificationService taskResultNotificationService;
    
    // ==================== 通知触发接口 ====================
    
    /**
     * 手动触发单任务结果通知
     * 
     * 管理员或系统可以手动为指定任务触发结果通知，
     * 用于测试、故障恢复或重新通知场景。
     * 
     * @param taskResult 任务结果信息
     * @return 通知发送结果
     */
    @PostMapping("/trigger")
    @Operation(summary = "手动触发任务结果通知", 
               description = "为指定任务手动触发结果通知，支持多重通知机制")
    public ResponseEntity<ApiResponse<TaskNotificationResult>> triggerTaskNotification(
            @Parameter(description = "任务结果信息", required = true)
            @RequestBody TaskResultResponse taskResult) {
        
        try {
            log.info("手动触发任务结果通知: taskId={}, userId={}", 
                    taskResult.getTaskId(), taskResult.getUserId());
            
            TaskResultNotificationService.TaskNotificationResult result = 
                taskResultNotificationService.notifyTaskResult(taskResult);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result, "任务通知发送成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("任务通知发送失败: " + result.getErrorMessage(), result));
            }
            
        } catch (Exception e) {
            log.error("手动触发任务结果通知失败: taskId={}, error={}", 
                     taskResult.getTaskId(), e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("通知触发失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量触发任务结果通知
     * 
     * 批量处理多个任务结果的通知发送。
     * 
     * @param taskResults 任务结果列表
     * @return 批量通知发送结果
     */
    @PostMapping("/trigger/batch")
    @Operation(summary = "批量触发任务结果通知", 
               description = "批量处理多个任务结果的通知发送")
    public ResponseEntity<ApiResponse<BatchTaskNotificationResult>> batchTriggerTaskNotification(
            @Parameter(description = "任务结果列表", required = true)
            @RequestBody List<TaskResultResponse> taskResults) {
        
        try {
            log.info("批量触发任务结果通知: count={}", taskResults.size());
            
            BatchTaskNotificationResult result = 
                taskResultNotificationService.batchNotifyTaskResults(taskResults);
            
            return ResponseEntity.ok(ApiResponse.success(result, 
                String.format("批量通知完成: 总计%d个, 成功%d个, 失败%d个, 离线%d个", 
                    result.getTotalTasks(), result.getSuccessfulNotifications(), 
                    result.getFailedNotifications(), result.getOfflineNotifications())));
            
        } catch (Exception e) {
            log.error("批量触发任务结果通知失败: count={}, error={}", 
                     taskResults.size(), e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("批量通知触发失败: " + e.getMessage()));
        }
    }
    
    /**
     * 高优先级任务通知
     * 
     * 对于失败任务或关键任务，发送高优先级通知。
     * 
     * @param taskResult 任务结果信息
     * @param isUrgent 是否为紧急通知
     * @return 通知发送结果
     */
    @PostMapping("/trigger/high-priority")
    @Operation(summary = "高优先级任务通知", 
               description = "对于失败任务或关键任务，发送高优先级通知")
    public ResponseEntity<ApiResponse<TaskNotificationResult>> triggerHighPriorityNotification(
            @Parameter(description = "任务结果信息", required = true)
            @RequestBody TaskResultResponse taskResult,
            
            @Parameter(description = "是否为紧急通知", required = false)
            @RequestParam(defaultValue = "false") boolean isUrgent) {
        
        try {
            log.info("触发高优先级任务通知: taskId={}, userId={}, urgent={}", 
                    taskResult.getTaskId(), taskResult.getUserId(), isUrgent);
            
            TaskResultNotificationService.TaskNotificationResult result = 
                taskResultNotificationService.notifyHighPriorityTaskResult(taskResult, isUrgent);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result, "高优先级通知发送成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("高优先级通知发送失败: " + result.getErrorMessage(), result));
            }
            
        } catch (Exception e) {
            log.error("触发高优先级任务通知失败: taskId={}, error={}", 
                     taskResult.getTaskId(), e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("高优先级通知触发失败: " + e.getMessage()));
        }
    }
    
    // ==================== 进度和汇总通知接口 ====================
    
    /**
     * 发送任务进度通知
     * 
     * 对于长时间运行的任务，发送进度更新通知。
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param progress 当前进度（0-100）
     * @param message 进度描述消息
     * @return 是否发送成功
     */
    @PostMapping("/progress")
    @Operation(summary = "发送任务进度通知", 
               description = "对于长时间运行的任务，发送进度更新通知")
    public ResponseEntity<ApiResponse<Boolean>> notifyTaskProgress(
            @Parameter(description = "任务ID", required = true)
            @RequestParam @NotBlank String taskId,
            
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId,
            
            @Parameter(description = "当前进度（0-100）", required = true)
            @RequestParam @Min(0) @Max(100) int progress,
            
            @Parameter(description = "进度描述消息", required = true)
            @RequestParam @NotBlank String message) {
        
        try {
            log.info("发送任务进度通知: taskId={}, userId={}, progress={}", 
                    taskId, userId, progress);
            
            boolean success = taskResultNotificationService.notifyTaskProgress(
                taskId, userId, organizationId, progress, message);
            
            return ResponseEntity.ok(ApiResponse.success(success, 
                success ? "进度通知发送成功" : "进度通知发送失败"));
            
        } catch (Exception e) {
            log.error("发送任务进度通知失败: taskId={}, progress={}, error={}", 
                     taskId, progress, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("进度通知发送失败: " + e.getMessage()));
        }
    }
    
    /**
     * 发送批量任务汇总通知
     * 
     * 对于批量任务，发送执行汇总通知。
     * 
     * @param summary 批量任务摘要
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 是否发送成功
     */
    @PostMapping("/batch-summary")
    @Operation(summary = "发送批量任务汇总通知", 
               description = "对于批量任务，发送执行汇总通知")
    public ResponseEntity<ApiResponse<Boolean>> notifyBatchTaskSummary(
            @Parameter(description = "批量任务摘要", required = true)
            @RequestBody BatchTaskSummary summary,
            
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId) {
        
        try {
            log.info("发送批量任务汇总通知: batchTaskId={}, userId={}", 
                    summary.getBatchTaskId(), userId);
            
            boolean success = taskResultNotificationService.notifyBatchTaskSummary(
                summary.getBatchTaskId(), userId, organizationId, summary);
            
            return ResponseEntity.ok(ApiResponse.success(success, 
                success ? "汇总通知发送成功" : "汇总通知发送失败"));
            
        } catch (Exception e) {
            log.error("发送批量任务汇总通知失败: batchTaskId={}, error={}", 
                     summary.getBatchTaskId(), e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("汇总通知发送失败: " + e.getMessage()));
        }
    }
    
    // ==================== 查询和监控接口 ====================
    
    /**
     * 查询任务通知状态
     * 
     * 查询指定任务的通知发送状态和用户接收状态。
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务通知状态信息
     */
    @GetMapping("/status")
    @Operation(summary = "查询任务通知状态", 
               description = "查询指定任务的通知发送状态和用户接收状态")
    public ResponseEntity<ApiResponse<TaskResultNotificationService.TaskNotificationStatus>> getTaskNotificationStatus(
            @Parameter(description = "任务ID", required = true)
            @RequestParam @NotBlank String taskId,
            
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId) {
        
        try {
            log.debug("查询任务通知状态: taskId={}, userId={}", taskId, userId);
            
            TaskResultNotificationService.TaskNotificationStatus status = 
                taskResultNotificationService.getTaskNotificationStatus(taskId, userId);
            
            if (status != null) {
                return ResponseEntity.ok(ApiResponse.success(status, "查询成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("未找到指定的任务通知记录"));
            }
            
        } catch (Exception e) {
            log.error("查询任务通知状态失败: taskId={}, userId={}, error={}", 
                     taskId, userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("查询通知状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 重新发送任务通知
     * 
     * 对于发送失败或用户未读的重要任务通知，支持重新发送。
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param forceResend 是否强制重发（忽略已读状态）
     * @return 重新发送结果
     */
    @PostMapping("/resend")
    @Operation(summary = "重新发送任务通知", 
               description = "对于发送失败或用户未读的重要任务通知，支持重新发送")
    public ResponseEntity<ApiResponse<TaskNotificationResult>> resendTaskNotification(
            @Parameter(description = "任务ID", required = true)
            @RequestParam @NotBlank String taskId,
            
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId,
            
            @Parameter(description = "是否强制重发（忽略已读状态）")
            @RequestParam(defaultValue = "false") boolean forceResend) {
        
        try {
            log.info("重新发送任务通知: taskId={}, userId={}, forceResend={}", 
                    taskId, userId, forceResend);
            
            TaskResultNotificationService.TaskNotificationResult result = 
                taskResultNotificationService.resendTaskNotification(taskId, userId, organizationId, forceResend);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result, "任务通知重发成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("任务通知重发失败: " + result.getErrorMessage(), result));
            }
            
        } catch (Exception e) {
            log.error("重新发送任务通知失败: taskId={}, userId={}, error={}", 
                     taskId, userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("任务通知重发失败: " + e.getMessage()));
        }
    }
    
    // ==================== 离线通知管理接口 ====================
    
    /**
     * 推送离线任务通知
     * 
     * 手动触发推送用户的离线任务通知。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @param maxCount 最大推送数量
     * @return 推送的通知数量
     */
    @PostMapping("/push-offline")
    @Operation(summary = "推送离线任务通知", 
               description = "手动触发推送用户的离线任务通知")
    public ResponseEntity<ApiResponse<Integer>> pushOfflineTaskNotifications(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "会话ID，用于会话级推送")
            @RequestParam(required = false) String sessionId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId,
            
            @Parameter(description = "最大推送数量，范围1-100")
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int maxCount) {
        
        try {
            log.info("推送离线任务通知: userId={}, sessionId={}, maxCount={}", 
                    userId, sessionId, maxCount);
            
            int pushedCount = taskResultNotificationService.pushOfflineTaskNotifications(
                userId, sessionId, organizationId, maxCount);
            
            return ResponseEntity.ok(ApiResponse.success(pushedCount, 
                String.format("成功推送%d条离线任务通知", pushedCount)));
            
        } catch (Exception e) {
            log.error("推送离线任务通知失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("推送离线通知失败: " + e.getMessage()));
        }
    }
    
    /**
     * 清理过期任务通知
     * 
     * 定期清理过期的离线任务通知和历史通知记录。
     * 
     * @param organizationId 组织ID（可选）
     * @param expireDays 过期天数
     * @return 清理的通知数量
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "清理过期任务通知", 
               description = "定期清理过期的离线任务通知和历史通知记录")
    public ResponseEntity<ApiResponse<Integer>> cleanupExpiredTaskNotifications(
            @Parameter(description = "组织ID，为空时清理所有组织")
            @RequestParam(required = false) String organizationId,
            
            @Parameter(description = "过期天数，范围1-365")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int expireDays) {
        
        try {
            log.info("清理过期任务通知: organizationId={}, expireDays={}", organizationId, expireDays);
            
            int cleanedCount = taskResultNotificationService.cleanupExpiredTaskNotifications(
                organizationId, expireDays);
            
            return ResponseEntity.ok(ApiResponse.success(cleanedCount, 
                String.format("成功清理%d条过期任务通知", cleanedCount)));
            
        } catch (Exception e) {
            log.error("清理过期任务通知失败: organizationId={}, expireDays={}, error={}", 
                     organizationId, expireDays, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("清理过期通知失败: " + e.getMessage()));
        }
    }
    
    // ==================== 配置管理接口 ====================
    
    /**
     * 获取通知配置
     * 
     * 获取指定组织或用户的任务通知配置。
     * 
     * @param organizationId 组织ID
     * @param userId 用户ID（可选）
     * @return 通知配置信息
     */
    @GetMapping("/config")
    @Operation(summary = "获取通知配置", 
               description = "获取指定组织或用户的任务通知配置信息")
    public ResponseEntity<ApiResponse<TaskResultNotificationService.TaskNotificationConfig>> getNotificationConfig(
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId,
            
            @Parameter(description = "用户ID，为空时获取组织配置")
            @RequestParam(required = false) String userId) {
        
        try {
            log.debug("获取通知配置: organizationId={}, userId={}", organizationId, userId);
            
            TaskResultNotificationService.TaskNotificationConfig config = 
                taskResultNotificationService.getNotificationConfig(organizationId, userId);
            
            return ResponseEntity.ok(ApiResponse.success(config, "获取通知配置成功"));
            
        } catch (Exception e) {
            log.error("获取通知配置失败: organizationId={}, userId={}, error={}", 
                     organizationId, userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("获取通知配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新通知配置
     * 
     * 更新组织或用户的任务通知配置。
     * 
     * @param organizationId 组织ID
     * @param userId 用户ID（可选）
     * @param config 新的通知配置
     * @return 是否更新成功
     */
    @PutMapping("/config")
    @Operation(summary = "更新通知配置", 
               description = "更新组织或用户的任务通知配置")
    public ResponseEntity<ApiResponse<Boolean>> updateNotificationConfig(
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId,
            
            @Parameter(description = "用户ID，为空时更新组织配置")
            @RequestParam(required = false) String userId,
            
            @Parameter(description = "通知配置信息", required = true)
            @RequestBody TaskResultNotificationService.TaskNotificationConfig config) {
        
        try {
            log.info("更新通知配置: organizationId={}, userId={}", organizationId, userId);
            
            boolean success = taskResultNotificationService.updateNotificationConfig(
                organizationId, userId, config);
            
            return ResponseEntity.ok(ApiResponse.success(success, 
                success ? "通知配置更新成功" : "通知配置更新失败"));
            
        } catch (Exception e) {
            log.error("更新通知配置失败: organizationId={}, userId={}, error={}", 
                     organizationId, userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("更新通知配置失败: " + e.getMessage()));
        }
    }
    
    // ==================== 统计监控接口 ====================
    
    /**
     * 获取通知统计指标
     * 
     * 统计任务通知的发送量、成功率等指标。
     * 
     * @param organizationId 组织ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 通知统计指标
     */
    @GetMapping("/metrics")
    @Operation(summary = "获取通知统计指标", 
               description = "统计任务通知的发送量、成功率等指标")
    public ResponseEntity<ApiResponse<TaskResultNotificationService.TaskNotificationMetrics>> getNotificationMetrics(
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId,
            
            @Parameter(description = "开始时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            
            @Parameter(description = "结束时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            log.debug("获取通知统计指标: organizationId={}, startTime={}, endTime={}", 
                     organizationId, startTime, endTime);
            
            TaskResultNotificationService.TaskNotificationMetrics metrics = 
                taskResultNotificationService.getNotificationMetrics(organizationId, startTime, endTime);
            
            return ResponseEntity.ok(ApiResponse.success(metrics, "获取统计指标成功"));
            
        } catch (Exception e) {
            log.error("获取通知统计指标失败: organizationId={}, error={}", 
                     organizationId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("获取统计指标失败: " + e.getMessage()));
        }
    }
}