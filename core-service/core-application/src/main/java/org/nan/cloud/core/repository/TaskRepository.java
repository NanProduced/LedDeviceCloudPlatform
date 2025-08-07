package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;

import java.util.List;

public interface TaskRepository {

    PageVO<Task> listTasks(int pageNum, int pageSize, String taskType, String taskStatus, Long orgId, Long userId);

    Task getTaskById(String taskId);

    int insertTask(Task task);

    int updateTask(Task task);

    int updateTaskStatus(String taskId, TaskStatusEnum taskStatus);

    int updateTaskError(String taskId, String errorMsg);

    void deleteTask(String taskId);

    void deleteTasks(List<String> taskIds);

}
