package org.nan.cloud.file.application.service;

import org.nan.cloud.file.application.domain.TaskContext;

/**
 * 任务上下文服务接口
 * 
 * 职责：
 * 1. 管理上传任务的上下文信息（任务ID-用户ID-组织ID映射）
 * 2. 为异步处理提供用户信息获取能力
 * 3. 支持任务生命周期管理（创建、查询、清理）
 * 
 * 实现策略：
 * - 开发环境：使用内存缓存（ConcurrentHashMap）
 * - 生产环境：使用Redis缓存，支持TTL过期
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface TaskContextService {

    /**
     * 创建任务上下文
     * 
     * @param taskId 任务ID
     * @param fileId 文件ID  
     * @param uid 用户ID
     * @param oid 组织ID
     * @param originalFilename 原始文件名
     * @param fileSize 文件大小
     */
    void createTaskContext(String taskId, String fileId, Long uid, Long oid, 
                          String originalFilename, Long fileSize);

    /**
     * 获取任务上下文
     * 
     * @param taskId 任务ID
     * @return 任务上下文信息，如果不存在返回null
     */
    TaskContext getTaskContext(String taskId);

    /**
     * 更新任务状态
     * 
     * @param taskId 任务ID
     * @param status 新状态
     */
    void updateTaskStatus(String taskId, TaskContext.TaskStatus status);

    /**
     * 更新任务进度
     * 
     * @param taskId 任务ID
     * @param progress 进度百分比 (0-100)
     */
    void updateTaskProgress(String taskId, Integer progress);

    /**
     * 设置任务的素材ID（在素材创建完成后）
     * 
     * @param taskId 任务ID
     * @param materialId 素材ID
     */
    void setTaskMaterialId(String taskId, Long materialId);

    /**
     * 检查任务是否存在
     * 
     * @param taskId 任务ID
     * @return 是否存在
     */
    boolean existsTask(String taskId);

    /**
     * 清理任务上下文（任务完成或失败后）
     * 
     * @param taskId 任务ID
     */
    void clearTaskContext(String taskId);

    /**
     * 批量清理过期任务
     * 
     * @param expireMinutes 过期时间（分钟）
     * @return 清理的任务数量
     */
    int cleanupExpiredTasks(int expireMinutes);

    /**
     * 获取任务的用户ID
     * 
     * @param taskId 任务ID
     * @return 用户ID，不存在返回null
     */
    Long getTaskUserId(String taskId);

    /**
     * 获取任务的组织ID
     * 
     * @param taskId 任务ID
     * @return 组织ID，不存在返回null
     */
    Long getTaskOrganizationId(String taskId);
}