package org.nan.cloud.message.service;

import org.nan.cloud.message.api.dto.response.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务结果多重通知服务接口
 * 
 * 实现任务结果的多重通知机制，确保用户能够通过多种方式及时获取任务执行结果。
 * 这是LED设备云平台任务管理系统的关键组件，支持异步任务的结果推送。
 * 
 * 通知层级：
 * - 会话级通知：向特定会话推送，适用于实时任务监控
 * - 用户级通知：向用户所有会话推送，确保多设备同步
 * - 离线通知：用户离线时保存，上线后自动推送
 * - 持久化通知：长期保存，支持历史查询
 * 
 * 业务场景：
 * - 设备控制任务完成通知
 * - 批量操作任务结果通知
 * - 数据分析任务完成通知
 * - 系统维护任务状态通知
 * - 定时任务执行结果通知
 * 
 * 通知策略：
 * - 成功任务：标准通知
 * - 失败任务：高优先级通知
 * - 长时间任务：进度通知
 * - 关键任务：多重确认通知
 * 
 * @author Nan
 * @since 1.0.0
 */
public interface TaskResultNotificationService {
    
    /**
     * 发送任务结果通知
     * 
     * 主入口方法，根据任务信息和用户状态自动选择合适的通知策略。
     * 会依次尝试会话级、用户级和离线通知。
     * 
     * @param taskResult 任务结果信息
     * @return 通知发送结果统计
     */
    TaskNotificationResult notifyTaskResult(TaskResultResponse taskResult);
    
    /**
     * 批量发送任务结果通知
     * 
     * 批量处理多个任务结果的通知，提高处理效率。
     * 支持不同任务类型的差异化通知策略。
     * 
     * @param taskResults 任务结果列表
     * @return 批量通知发送结果
     */
    BatchTaskNotificationResult batchNotifyTaskResults(List<TaskResultResponse> taskResults);
    
    /**
     * 会话级任务结果通知
     * 
     * 向特定会话发送任务结果通知，适用于实时任务监控场景。
     * 如果会话不存在或断开，则自动降级到用户级通知。
     * 
     * @param taskResult 任务结果信息
     * @param sessionId 目标会话ID
     * @return 是否成功发送到会话
     */
    boolean notifyTaskResultToSession(TaskResultResponse taskResult, String sessionId);
    
    /**
     * 用户级任务结果通知
     * 
     * 向用户的所有活跃会话发送任务结果通知。
     * 确保用户在多个设备上都能收到通知。
     * 
     * @param taskResult 任务结果信息
     * @return 成功发送的会话数量
     */
    int notifyTaskResultToUser(TaskResultResponse taskResult);
    
    /**
     * 离线任务结果通知
     * 
     * 用户离线时保存任务结果通知，等用户上线后自动推送。
     * 支持通知优先级排序和数量限制。
     * 
     * @param taskResult 任务结果信息
     * @return 是否成功保存离线通知
     */
    boolean saveOfflineTaskNotification(TaskResultResponse taskResult);
    
    /**
     * 推送用户的离线任务通知
     * 
     * 用户上线时调用，推送所有离线期间的任务结果通知。
     * 通常由用户上线推送服务调用。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @param maxCount 最大推送数量
     * @return 推送的通知数量
     */
    int pushOfflineTaskNotifications(String userId, String sessionId, String organizationId, int maxCount);
    
    /**
     * 高优先级任务结果通知
     * 
     * 对于失败任务或关键任务，发送高优先级通知。
     * 会尝试多种通知方式，确保用户收到重要信息。
     * 
     * @param taskResult 任务结果信息
     * @param isUrgent 是否为紧急通知
     * @return 通知发送结果
     */
    TaskNotificationResult notifyHighPriorityTaskResult(TaskResultResponse taskResult, boolean isUrgent);
    
    /**
     * 任务进度通知
     * 
     * 对于长时间运行的任务，发送进度更新通知。
     * 避免用户等待过程中的焦虑，提升用户体验。
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param progress 当前进度（0-100）
     * @param message 进度描述消息
     * @return 是否成功发送进度通知
     */
    boolean notifyTaskProgress(String taskId, String userId, String organizationId, 
                             int progress, String message);
    
    /**
     * 批量任务汇总通知
     * 
     * 对于批量任务，发送执行汇总通知而不是每个任务单独通知。
     * 避免通知过多影响用户体验。
     * 
     * @param batchTaskId 批量任务ID
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param summary 批量任务执行摘要
     * @return 是否成功发送汇总通知
     */
    boolean notifyBatchTaskSummary(String batchTaskId, String userId, String organizationId, 
                                 BatchTaskSummary summary);
    
    /**
     * 检查任务通知状态
     * 
     * 检查指定任务的通知发送状态和用户接收状态。
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务通知状态信息
     */
    TaskNotificationStatus getTaskNotificationStatus(String taskId, String userId);
    
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
    TaskNotificationResult resendTaskNotification(String taskId, String userId, String organizationId, 
                                                boolean forceResend);
    
    /**
     * 清理过期的任务通知
     * 
     * 定期清理过期的离线任务通知和历史通知记录，释放存储空间。
     * 
     * @param organizationId 组织ID（可选，为空时清理所有组织）
     * @param expireDays 过期天数
     * @return 清理的通知数量
     */
    int cleanupExpiredTaskNotifications(String organizationId, int expireDays);
    
    /**
     * 获取任务通知配置
     * 
     * 获取指定组织或用户的任务通知配置，如通知方式、频率限制等。
     * 
     * @param organizationId 组织ID
     * @param userId 用户ID（可选）
     * @return 通知配置信息
     */
    TaskNotificationConfig getNotificationConfig(String organizationId, String userId);
    
    /**
     * 更新任务通知配置
     * 
     * 更新组织或用户的任务通知配置。
     * 
     * @param organizationId 组织ID
     * @param userId 用户ID（可选，为空时更新组织配置）
     * @param config 新的通知配置
     * @return 是否更新成功
     */
    boolean updateNotificationConfig(String organizationId, String userId, TaskNotificationConfig config);
    
    /**
     * 统计任务通知指标
     * 
     * 统计任务通知的发送量、成功率等指标，用于系统监控。
     * 
     * @param organizationId 组织ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 通知统计指标
     */
    TaskNotificationMetrics getNotificationMetrics(String organizationId, 
                                                  LocalDateTime startTime, 
                                                  LocalDateTime endTime);
}