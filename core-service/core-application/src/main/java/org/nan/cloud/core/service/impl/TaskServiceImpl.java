package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryTaskRequest;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;
import org.nan.cloud.core.repository.TaskRepository;
import org.nan.cloud.core.service.BusinessCacheService;
import org.nan.cloud.core.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务服务实现类
 * 
 * @author Nan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final BusinessCacheService businessCacheService;

    @Override
    public PageVO<Task> listTasks(PageRequestDTO<QueryTaskRequest> pageRequest, Long orgId, Long userId) {
        PageVO<Task> page = taskRepository.listTasks(pageRequest.getPageNum(),
                pageRequest.getPageSize(),
                pageRequest.getParams() == null ? null : pageRequest.getParams().getTaskType(),
                pageRequest.getParams() == null ? null : pageRequest.getParams().getTaskStatus(),
                pageRequest.getParams() == null ? null : pageRequest.getParams().getKeyword(),
                orgId, userId);

        // 补充领域字段：progress（从缓存），以及为完成任务补充100%
        if (page.getRecords() != null) {
            for (Task t : page.getRecords()) {
                Task cached = businessCacheService.getTaskProgress(t.getTaskId());
                if (cached != null && cached.getProgress() != null) {
                    t.setProgress(cached.getProgress());
                } else if (t.getTaskStatus() == TaskStatusEnum.COMPLETED) {
                    t.setProgress(100);
                } else if (t.getProgress() == null) {
                    t.setProgress(0);
                }
            }
        }

        return page;
    }

    @Override
    public Task getTaskById(String taskId) {
        // 先从缓存获取
        Task cachedTask = businessCacheService.getTaskProgress(taskId);
        if (cachedTask != null) {
            return cachedTask;
        }
        
        Task task = taskRepository.getTaskById(taskId);
        
        // 缓存任务信息
        businessCacheService.cacheTaskProgress(taskId, task, null);
        
        return task;
    }

    @Override
    @Transactional
    public Task createTask(Task task) {
        if (task.getTaskId() == null) {
            task.setTaskId(java.util.UUID.randomUUID().toString());
        }
        
        if (task.getCreateTime() == null) {
            task.setCreateTime(LocalDateTime.now());
        }
        
        if (task.getTaskStatus() == null) {
            task.setTaskStatus(TaskStatusEnum.PENDING);
        }
        
        if (task.getProgress() == null) {
            task.setProgress(0);
        }

        int inserted = taskRepository.insertTask(task);
        
        if (inserted > 0) {
            log.info("✅ 任务创建成功: taskId={}, type={}, oid={}", 
                    task.getTaskId(), task.getTaskType(), task.getOid());
            
            // 缓存任务
            businessCacheService.cacheTaskProgress(task.getTaskId(), task, null);
            
            return task;
        } else {
            throw new RuntimeException("任务创建失败: " + task.getTaskId());
        }
    }

    @Override
    @Transactional
    public void updateTaskStatus(String taskId, TaskStatusEnum status) {

        int updated = taskRepository.updateTaskStatus(taskId, status);
        
        if (updated > 0) {
            log.info("📝 任务状态更新: taskId={}, status={}", taskId, status);
            
            // 更新缓存
            Task cachedTask = businessCacheService.getTaskProgress(taskId);
            if (cachedTask != null) {
                cachedTask.setTaskStatus(status);
                if (status == TaskStatusEnum.COMPLETED) {
                    cachedTask.setCompleteTime(LocalDateTime.now());
                }
                businessCacheService.cacheTaskProgress(taskId, cachedTask, null);
            }
        }
    }

    @Override
    @Transactional
    public void updateTaskProgress(String taskId, Integer progress) {
        // 更新缓存中的进度
        Task cachedTask = businessCacheService.getTaskProgress(taskId);
        if (cachedTask != null) {
            cachedTask.setProgress(progress);
            businessCacheService.cacheTaskProgress(taskId, cachedTask, null);
            log.debug("📊 任务进度更新: taskId={}, progress={}%", taskId, progress);
        } else {
            log.warn("⚠️ 更新任务进度失败，任务不存在: taskId={}", taskId);
        }
    }

    @Override
    @Transactional
    public void updateTaskError(String taskId, String errorMessage) {
        
        int updated = taskRepository.updateTaskError(taskId, errorMessage);
        
        if (updated > 0) {
            log.error("❌ 任务错误更新: taskId={}, error={}", taskId, errorMessage);
            
            // 更新缓存
            Task cachedTask = businessCacheService.getTaskProgress(taskId);
            if (cachedTask != null) {
                cachedTask.setErrorMessage(errorMessage);
                cachedTask.setTaskStatus(TaskStatusEnum.FAILED);
                cachedTask.setCompleteTime(LocalDateTime.now());
                businessCacheService.cacheTaskProgress(taskId, cachedTask, null);
            }
        }
    }

    @Override
    public void completeTask(String taskId) {
        completeTask(taskId, null, null);
    }

    @Override
    @Transactional
    public void completeTask(String taskId, String downloadedUrl, String thumbnailUrl) {
        Task updateTask = Task.builder()
                .taskId(taskId)
                .taskStatus(TaskStatusEnum.COMPLETED)
                .completeTime(LocalDateTime.now())
                .downloadedUrl(downloadedUrl)
                .thumbnailUrl(thumbnailUrl)
                .build();
        
        int updated = taskRepository.updateTask(updateTask);
        
        if (updated > 0) {
            log.info("✅ 任务完成: taskId={}", taskId);
            
            // 更新缓存
            Task cachedTask = businessCacheService.getTaskProgress(taskId);
            if (cachedTask != null) {
                cachedTask.setTaskStatus(TaskStatusEnum.COMPLETED);
                cachedTask.setCompleteTime(LocalDateTime.now());
                cachedTask.setProgress(100);
                if (downloadedUrl != null) {
                    cachedTask.setDownloadedUrl(downloadedUrl);
                }
                if (thumbnailUrl != null) {
                    cachedTask.setThumbnailUrl(thumbnailUrl);
                }
                businessCacheService.cacheTaskProgress(taskId, cachedTask, null);
            }
        }
    }

    @Override
    public void failTask(String taskId, String errorMessage) {
        updateTaskError(taskId, errorMessage);
    }

    @Override
    @Transactional
    public void cancelTask(String taskId) {
        updateTaskStatus(taskId, TaskStatusEnum.CANCELED);
    }

    @Override
    public List<Task> getUserTasks(Long oid, Long userId, TaskTypeEnum taskType, TaskStatusEnum status) {
        // TODO: 实现用户任务查询逻辑
        throw new UnsupportedOperationException("getUserTasks not implemented yet");
    }

    @Override
    public List<Task> getOrgTasks(Long oid, TaskTypeEnum taskType, TaskStatusEnum status) {
        // TODO: 实现组织任务查询逻辑
        throw new UnsupportedOperationException("getOrgTasks not implemented yet");
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        taskRepository.deleteTask(taskId);
        // 清除缓存
        businessCacheService.evictTaskProgress(taskId);
        log.info("🗑️ 任务已删除: taskId={}", taskId);
    }

    @Override
    @Transactional
    public void deleteTasks(List<String> taskIds) {
        taskRepository.deleteTasks(taskIds);
        for (String taskId : taskIds) {
            deleteTask(taskId);
        }
    }

    @Override
    public Long getMaterialIdByTaskId(String taskId) {
        log.debug("🔍 通过任务ID查询素材ID - taskId: {}", taskId);
        
        try {
            Task task = getTaskById(taskId);
            if (task == null) {
                log.warn("⚠️ 任务不存在 - taskId: {}", taskId);
                return null;
            }
            
            String refId = task.getRefId();
            if (refId == null || refId.trim().isEmpty()) {
                log.warn("⚠️ 任务没有关联的素材ID - taskId: {}", taskId);
                return null;
            }
            
            Long materialId = Long.parseLong(refId);
            log.debug("✅ 找到素材ID - taskId: {}, materialId: {}", taskId, materialId);
            return materialId;
            
        } catch (NumberFormatException e) {
            log.error("❌ 素材ID格式错误 - taskId: {}, refId: {}", taskId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ 查询素材ID失败 - taskId: {}, 错误: {}", taskId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<String, Long> countTasksByStatus(Long orgId, Long userId) {
        PageRequestDTO<QueryTaskRequest> req = new PageRequestDTO<>();
        req.setPageNum(1);
        req.setPageSize(1000);
        req.setParams(new QueryTaskRequest());
        PageVO<Task> page = listTasks(req, orgId, userId);
        return page.getRecords().stream()
                .collect(Collectors.groupingBy(t -> t.getTaskStatus() == null ? "UNKNOWN" : t.getTaskStatus().name(), Collectors.counting()));
    }
}