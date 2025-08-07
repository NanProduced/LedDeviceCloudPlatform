package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.basic.utils.StringUtils;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.TaskDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.TaskConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TaskMapper;
import org.nan.cloud.core.repository.TaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskMapper taskMapper;

    private final TaskConverter taskConverter;

    @Override
    public PageVO<Task> listTasks(int pageNum, int pageSize, String taskType, String taskStatus, Long orgId, Long userId) {
        Page<TaskDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getOid, orgId)
                .eq(TaskDO::getCreator, userId);
        if (StringUtils.isNotBlank(taskType)) {
            wrapper.and(qw -> qw.eq(TaskDO::getTaskType, taskType));
        }
        if (StringUtils.isNotBlank(taskStatus)) {
            wrapper.and(qw -> qw.eq(TaskDO::getTaskStatus, taskStatus));
        }
        wrapper.orderByDesc(TaskDO::getCreateTime);
        IPage<TaskDO> pageResult = taskMapper.selectPage(page, wrapper);
        PageVO<Task> pageVO = PageVO.<Task>builder()
                .pageNum((int) pageResult.getCurrent())
                .pageSize((int) pageResult.getSize())
                .total(pageResult.getTotal())
                .records(taskConverter.toTaskList(pageResult.getRecords()))
                .build();
        pageVO.calculate();
        return pageVO;
    }

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
