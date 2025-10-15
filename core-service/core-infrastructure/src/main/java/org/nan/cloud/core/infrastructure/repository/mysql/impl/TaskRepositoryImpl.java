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
import org.nan.cloud.core.infrastructure.repository.mysql.converter.TaskDomainConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.TaskMapper;
import org.nan.cloud.core.repository.TaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.nan.cloud.core.enums.TaskTypeEnum;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskMapper taskMapper;

    private final TaskDomainConverter taskConverter;

    @Override
    public PageVO<Task> listTasks(int pageNum, int pageSize, String taskType, String taskStatus, String keyword, Long orgId, Long userId) {
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
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(qw -> qw.like(TaskDO::getRef, keyword));
        }
        // keyword按ref模糊查询：为了保持接口兼容，这里可以扩展为从ThreadLocal携带，也可后续修改仓储签名。
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

    // ========== 转码任务查询相关方法实现 ==========

    @Override
    public List<Task> findTranscodingTasksByUser(Long uid, Long oid, TaskTypeEnum taskType, String status,
                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                 Integer page, Integer size, String sortBy, String sortDirection) {
        log.debug("🔍 查询用户转码任务列表 - uid: {}, oid: {}, taskType: {}", uid, oid, taskType);
        
        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getCreator, uid)
                .eq(TaskDO::getOid, oid)
                .eq(TaskDO::getTaskType, taskType.name());
        
        // 状态过滤
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(TaskDO::getTaskStatus, status);
        }
        
        // 时间范围过滤
        if (startTime != null) {
            wrapper.ge(TaskDO::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(TaskDO::getCreateTime, endTime);
        }
        
        // 排序处理
        if ("asc".equalsIgnoreCase(sortDirection)) {
            if ("createTime".equals(sortBy)) {
                wrapper.orderByAsc(TaskDO::getCreateTime);
            } else if ("completeTime".equals(sortBy)) {
                wrapper.orderByAsc(TaskDO::getCompleteTime);
            } else {
                wrapper.orderByAsc(TaskDO::getCreateTime); // 默认
            }
        } else {
            if ("createTime".equals(sortBy)) {
                wrapper.orderByDesc(TaskDO::getCreateTime);
            } else if ("completeTime".equals(sortBy)) {
                wrapper.orderByDesc(TaskDO::getCompleteTime);
            } else {
                wrapper.orderByDesc(TaskDO::getCreateTime); // 默认
            }
        }
        
        // 分页处理
        if (page != null && size != null && page > 0 && size > 0) {
            Page<TaskDO> pageObj = new Page<>(page, size);
            IPage<TaskDO> pageResult = taskMapper.selectPage(pageObj, wrapper);
            log.debug("📊 转码任务查询结果 - 当前页: {}, 总数: {}", page, pageResult.getTotal());
            return taskConverter.toTaskList(pageResult.getRecords());
        } else {
            // 无分页查询
            List<TaskDO> taskDOS = taskMapper.selectList(wrapper);
            log.debug("📊 转码任务查询结果 - 总数: {}", taskDOS.size());
            return taskConverter.toTaskList(taskDOS);
        }
    }

    @Override
    public Integer countTranscodingTasksByUser(Long uid, Long oid, TaskTypeEnum taskType, String status,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("🔢 统计用户转码任务数量 - uid: {}, oid: {}, taskType: {}", uid, oid, taskType);
        
        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getCreator, uid)
                .eq(TaskDO::getOid, oid)
                .eq(TaskDO::getTaskType, taskType.name());
        
        // 状态过滤
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(TaskDO::getTaskStatus, status);
        }
        
        // 时间范围过滤
        if (startTime != null) {
            wrapper.ge(TaskDO::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(TaskDO::getCreateTime, endTime);
        }
        
        Long count = taskMapper.selectCount(wrapper);
        log.debug("📊 转码任务数量统计结果: {}", count);
        return count.intValue();
    }

    @Override
    public Task findTranscodingTaskByIdAndUser(String taskId, Long uid, Long oid, TaskTypeEnum taskType) {
        log.debug("🔍 根据任务ID和用户查询转码任务 - taskId: {}, uid: {}, oid: {}", taskId, uid, oid);
        
        TaskDO taskDO = taskMapper.selectOne(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTaskId, taskId)
                .eq(TaskDO::getCreator, uid)
                .eq(TaskDO::getOid, oid)
                .eq(TaskDO::getTaskType, taskType.name())
        );
        
        if (taskDO == null) {
            log.warn("⚠️ 转码任务不存在或无权限访问 - taskId: {}, uid: {}", taskId, uid);
            return null;
        }
        
        return taskConverter.toTask(taskDO);
    }

    @Override
    public List<Task> findTranscodingTasksBySourceMaterial(Long sourceMaterialId, Long uid, Long oid, TaskTypeEnum taskType) {
        log.debug("🔍 根据源素材ID查询转码任务 - sourceMaterialId: {}, uid: {}, oid: {}", sourceMaterialId, uid, oid);
        
        // 通过ref字段查询，ref格式为 "material:{sourceMaterialId}"
        String refPattern = "material:" + sourceMaterialId;
        
        List<TaskDO> taskDOS = taskMapper.selectList(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getCreator, uid)
                .eq(TaskDO::getOid, oid)
                .eq(TaskDO::getTaskType, taskType.name())
                .eq(TaskDO::getRef, refPattern)
                .orderByDesc(TaskDO::getCreateTime) // 按创建时间倒序
        );
        
        log.debug("📊 找到转码任务数量: {} - sourceMaterialId: {}", taskDOS.size(), sourceMaterialId);
        
        return taskConverter.toTaskList(taskDOS);
    }
}
