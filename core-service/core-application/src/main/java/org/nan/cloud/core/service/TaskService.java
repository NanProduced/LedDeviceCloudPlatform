package org.nan.cloud.core.service;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryTaskRequest;
import org.nan.cloud.core.api.DTO.res.QueryTaskResponse;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;

import java.util.List;

/**
 * 任务服务接口
 * 
 * @author Nan
 */
public interface TaskService {

    PageVO<Task> listTasks(PageRequestDTO<QueryTaskRequest> pageRequest,
                                        Long orgId, Long userId);

    /**
     * 根据任务ID查询任务
     * @param taskId 任务ID
     * @return 任务信息
     */
    Task getTaskById(String taskId);

    /**
     * 创建任务
     * @param task 任务信息
     * @return 创建的任务
     */
    Task createTask(Task task);

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 新状态
     */
    void updateTaskStatus(String taskId, TaskStatusEnum status);

    /**
     * 更新任务进度
     * @param taskId 任务ID
     * @param progress 进度百分比 (0-100)
     */
    void updateTaskProgress(String taskId, Integer progress);

    /**
     * 更新任务错误信息
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void updateTaskError(String taskId, String errorMessage);

    /**
     * 完成任务
     * @param taskId 任务ID
     */
    void completeTask(String taskId);

    /**
     * 完成任务并设置额外信息
     * @param taskId 任务ID
     * @param downloadedUrl 下载链接（文件导出任务）
     * @param thumbnailUrl 缩略图链接（素材任务）
     */
    void completeTask(String taskId, String downloadedUrl, String thumbnailUrl);

    /**
     * 失败任务
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void failTask(String taskId, String errorMessage);

    /**
     * 取消任务
     * @param taskId 任务ID
     */
    void cancelTask(String taskId);

    /**
     * 根据组织ID和用户ID查询任务列表
     * @param oid 组织ID
     * @param userId 用户ID
     * @param taskType 任务类型（可选）
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    List<Task> getUserTasks(Long oid, Long userId, TaskTypeEnum taskType, TaskStatusEnum status);

    /**
     * 根据组织ID查询任务列表
     * @param oid 组织ID
     * @param taskType 任务类型（可选）
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    List<Task> getOrgTasks(Long oid, TaskTypeEnum taskType, TaskStatusEnum status);

    /**
     * 删除任务
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);

    /**
     * 批量删除任务
     * @param taskIds 任务ID列表
     */
    void deleteTasks(List<String> taskIds);
}