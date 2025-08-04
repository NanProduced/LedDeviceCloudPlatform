package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TaskDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.TaskConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TaskMapper;
import org.nan.cloud.core.repository.TaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskMapper taskMapper;

    private final TaskConverter taskConverter;


    @Override
    public Task getTaskById(String taskId) {
        return taskConverter.toTask(taskMapper.selectById(taskId));
    }

    @Override
    public int insertTask(Task task) {
        return taskMapper.insert(taskConverter.toTaskDO(task));
    }

    @Override
    public int updateTask(Task task) {
        return taskMapper.updateById(taskConverter.toTaskDO(task));
    }

    @Override
    public int updateTaskStatus(String taskId, TaskStatusEnum taskStatus) {
        TaskDO updateTask = TaskDO.builder()
                .taskId(taskId)
                .taskStatus(taskStatus)
                .build();

        if (taskStatus == TaskStatusEnum.COMPLETED) {
            updateTask.setCompleteTime(LocalDateTime.now());
        }

        return taskMapper.updateById(updateTask);
    }

    @Override
    public int updateTaskError(String taskId, String errorMsg) {
        TaskDO updateTask = TaskDO.builder()
                .taskId(taskId)
                .errorMessage(errorMsg)
                .taskStatus(TaskStatusEnum.FAILED)
                .completeTime(LocalDateTime.now())
                .build();

        return taskMapper.updateById(updateTask);
    }

    @Override
    public void deleteTask(String taskId) {
        taskMapper.deleteById(taskId);
    }

    @Override
    public void deleteTasks(List<String> taskIds) {
        taskMapper.deleteByIds(taskIds);
    }
}
