package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository {

    PageVO<Task> listTasks(int pageNum, int pageSize, String taskType, String taskStatus, String keyword, Long orgId, Long userId);

    Task getTaskById(String taskId);

    int insertTask(Task task);

    int updateTask(Task task);

    int updateTaskStatus(String taskId, TaskStatusEnum taskStatus);

    int updateTaskError(String taskId, String errorMsg);

    void deleteTask(String taskId);

    void deleteTasks(List<String> taskIds);

    // ========== 转码任务查询相关方法 ==========

    /**
     * 查询用户转码任务列表
     * @param uid 用户ID
     * @param oid 组织ID  
     * @param taskType 任务类型
     * @param status 任务状态过滤
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param page 页码
     * @param size 每页大小
     * @param sortBy 排序字段
     * @param sortDirection 排序方向
     * @return 转码任务列表
     */
    List<Task> findTranscodingTasksByUser(Long uid, Long oid, TaskTypeEnum taskType, String status, 
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         Integer page, Integer size, String sortBy, String sortDirection);

    /**
     * 统计用户转码任务数量
     */
    Integer countTranscodingTasksByUser(Long uid, Long oid, TaskTypeEnum taskType, String status,
                                       LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据任务ID和用户查询转码任务（权限控制）
     */
    Task findTranscodingTaskByIdAndUser(String taskId, Long uid, Long oid, TaskTypeEnum taskType);

    /**
     * 根据源素材ID查询转码任务
     */
    List<Task> findTranscodingTasksBySourceMaterial(Long sourceMaterialId, Long uid, Long oid, TaskTypeEnum taskType);

}
