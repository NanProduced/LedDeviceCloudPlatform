package org.nan.cloud.message.domain.repository;

import org.nan.cloud.message.api.dto.response.TaskResultResponse;
import org.nan.cloud.message.api.dto.response.TaskHistoryResponse;
import org.nan.cloud.message.domain.model.TaskResultData;

import java.util.List;
import java.util.Optional;

/**
 * 任务结果持久化仓储接口
 * 
 * 定义任务结果持久化的领域接口，application层依赖此抽象接口，
 * infrastructure层提供具体实现，遵循DDD架构的依赖倒置原则。
 * 
 * 核心功能：
 * - 任务结果的完整保存和查询
 * - 任务会话关系管理
 * - 多重通知机制支持
 * - 任务历史记录管理
 * 
 * @author Nan
 * @since 1.0.0
 */
public interface TaskResultPersistenceRepository {
    
    /**
     * 创建新任务记录
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @param taskType 任务类型
     * @param taskName 任务名称
     */
    void createTaskRecord(String taskId, String userId, String sessionId, 
                         String organizationId, String taskType, String taskName);
    
    /**
     * 更新任务状态和进度
     * 
     * @param taskId 任务ID
     * @param status 新状态
     * @param progress 进度百分比
     * @param errorMessage 错误消息（可选）
     */
    void updateTaskStatus(String taskId, String status, Integer progress, String errorMessage);
    
    /**
     * 保存任务完整结果
     * 
     * @param taskResult 任务结果详情
     */
    void saveTaskResult(TaskResultData taskResult);
    
    /**
     * 获取完整任务结果
     * 
     * @param taskId 任务ID
     * @return 完整任务结果
     */
    Optional<TaskResultResponse> getCompleteTaskResult(String taskId);
    
    /**
     * 查询用户未查看的完成任务
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 未查看的完成任务列表
     */
    List<TaskResultResponse> getUnviewedCompletedTasks(String userId, String organizationId);
    
    /**
     * 分页查询用户任务历史
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param taskType 任务类型（可选）
     * @param status 任务状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页任务历史
     */
    TaskHistoryResponse getTaskHistoryByPage(String userId, String organizationId, 
                                           String taskType, String status, 
                                           int page, int size);
    
    /**
     * 获取任务对应的会话ID
     * 
     * @param taskId 任务ID
     * @return 会话ID
     */
    String getTaskSessionId(String taskId);
    
    /**
     * 更新任务通知状态
     * 
     * @param taskId 任务ID
     * @param notificationStatus 通知状态
     */
    void updateNotificationStatus(String taskId, String notificationStatus);
    
    /**
     * 标记任务结果为已查看
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（安全检查）
     */
    void markTaskResultAsViewed(String taskId, String userId);
    
    /**
     * 清理会话相关的任务映射
     * 
     * @param sessionId 会话ID
     */
    void cleanupSessionTasks(String sessionId);
}