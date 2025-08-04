package org.nan.cloud.file.infrastructure.progress;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.domain.TaskContext;
import org.nan.cloud.file.application.service.TaskContextService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务上下文服务实现 - 内存缓存版本
 * 
 * 开发环境实现，使用ConcurrentHashMap作为内存缓存。
 * 生产环境建议使用Redis实现以支持分布式场景。
 * 
 * 特性：
 * 1. 线程安全的任务上下文存储
 * 2. 自动过期清理（默认2小时）
 * 3. 高性能的内存访问
 * 4. 支持任务状态和进度跟踪
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class TaskContextServiceImpl implements TaskContextService {

    /**
     * 任务上下文存储 - 线程安全
     */
    private final ConcurrentHashMap<String, TaskContext> taskContextMap = new ConcurrentHashMap<>();

    /**
     * 定时清理服务
     */
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TaskContext-Cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 默认过期时间：2小时
     */
    private static final int DEFAULT_EXPIRE_MINUTES = 120;

    public TaskContextServiceImpl() {
        // 启动定时清理任务，每30分钟执行一次
        cleanupExecutor.scheduleWithFixedDelay(
            () -> cleanupExpiredTasks(DEFAULT_EXPIRE_MINUTES),
            30, 30, TimeUnit.MINUTES
        );
        
        log.info("TaskContextService initialized with memory cache, auto-cleanup every 30 minutes");
    }

    @Override
    public void createTaskContext(String taskId, String fileId, Long uid, Long oid, 
                                 String originalFilename, Long fileSize) {
        if (taskId == null || uid == null || oid == null) {
            throw new IllegalArgumentException("taskId, uid, oid 不能为空");
        }

        TaskContext context = TaskContext.create(taskId, fileId, uid, oid, originalFilename, fileSize);
        taskContextMap.put(taskId, context);
        
        log.debug("创建任务上下文 - 任务ID: {}, 用户ID: {}, 组织ID: {}, 文件: {}", 
                taskId, uid, oid, originalFilename);
    }

    @Override
    public TaskContext getTaskContext(String taskId) {
        if (taskId == null) {
            return null;
        }
        
        TaskContext context = taskContextMap.get(taskId);
        if (context != null) {
            log.debug("获取任务上下文 - 任务ID: {}, 状态: {}, 进度: {}%", 
                    taskId, context.getStatus(), context.getProgress());
        }
        
        return context;
    }

    @Override
    public void updateTaskStatus(String taskId, TaskContext.TaskStatus status) {
        TaskContext context = taskContextMap.get(taskId);
        if (context != null) {
            context.updateStatus(status);
            log.debug("更新任务状态 - 任务ID: {}, 新状态: {}", taskId, status);
        } else {
            log.warn("任务上下文不存在，无法更新状态 - 任务ID: {}", taskId);
        }
    }

    @Override
    public void updateTaskProgress(String taskId, Integer progress) {
        TaskContext context = taskContextMap.get(taskId);
        if (context != null) {
            context.updateProgress(progress);
            log.debug("更新任务进度 - 任务ID: {}, 进度: {}%", taskId, progress);
        } else {
            log.warn("任务上下文不存在，无法更新进度 - 任务ID: {}", taskId);
        }
    }

    @Override
    public void setTaskMaterialId(String taskId, Long materialId) {
        TaskContext context = taskContextMap.get(taskId);
        if (context != null) {
            context.setMaterialId(materialId);
            context.setUpdateTime(LocalDateTime.now());
            log.debug("设置任务素材ID - 任务ID: {}, 素材ID: {}", taskId, materialId);
        } else {
            log.warn("任务上下文不存在，无法设置素材ID - 任务ID: {}", taskId);
        }
    }

    @Override
    public boolean existsTask(String taskId) {
        return taskId != null && taskContextMap.containsKey(taskId);
    }

    @Override
    public void clearTaskContext(String taskId) {
        if (taskId != null) {
            TaskContext removed = taskContextMap.remove(taskId);
            if (removed != null) {
                log.debug("清理任务上下文 - 任务ID: {}, 最终状态: {}", taskId, removed.getStatus());
            }
        }
    }

    @Override
    public int cleanupExpiredTasks(int expireMinutes) {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(expireMinutes);
        int cleanedCount = 0;

        // 遍历并清理过期任务
        for (var iterator = taskContextMap.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            TaskContext context = entry.getValue();
            
            // 清理过期或已完成的任务
            if (context.getUpdateTime().isBefore(expireTime) || context.isFinished()) {
                iterator.remove();
                cleanedCount++;
                
                log.debug("清理过期任务 - 任务ID: {}, 状态: {}, 更新时间: {}", 
                        entry.getKey(), context.getStatus(), context.getUpdateTime());
            }
        }

        if (cleanedCount > 0) {
            log.info("清理过期任务完成 - 清理数量: {}, 剩余任务: {}", cleanedCount, taskContextMap.size());
        }

        return cleanedCount;
    }

    @Override
    public Long getTaskUserId(String taskId) {
        TaskContext context = getTaskContext(taskId);
        return context != null ? context.getUid() : null;
    }

    @Override
    public Long getTaskOrganizationId(String taskId) {
        TaskContext context = getTaskContext(taskId);
        return context != null ? context.getOid() : null;
    }

    /**
     * 获取当前任务数量（用于监控）
     */
    public int getCurrentTaskCount() {
        return taskContextMap.size();
    }

    /**
     * 关闭清理服务
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        log.info("TaskContextService shutdown completed");
    }
}