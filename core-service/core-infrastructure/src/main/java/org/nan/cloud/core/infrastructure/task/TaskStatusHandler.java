package org.nan.cloud.core.infrastructure.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.event.mq.FileUploadEvent;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;
import org.nan.cloud.core.service.TaskService;
import org.springframework.stereotype.Component;

/**
 * 对任务状态的处理
 * @author Nan
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TaskStatusHandler {

    private final TaskService taskService;

    /**
     * 收到素材上传开始消息后，初始化任务和缓存任务进度
     * @param fileUploadEvent
     */
    public void initMaterialUploadTask(FileUploadEvent fileUploadEvent) {
        Task task = Task.builder()
                .taskId(fileUploadEvent.getTaskId())
                .taskType(TaskTypeEnum.MATERIAL_UPLOAD)
                .taskStatus(TaskStatusEnum.PENDING)
                .ref(fileUploadEvent.getOriginalFilename())
                .createTime(fileUploadEvent.getTimestamp())
                .oid(Long.valueOf(fileUploadEvent.getOrganizationId()))
                .creator(Long.valueOf(fileUploadEvent.getUserId()))
                .progress(0)
                .build();
        
        try {
            taskService.createTask(task);
            log.info("✅ 素材上传任务初始化成功: oid={}, taskId={}", 
                    task.getOid(), task.getTaskId());
        } catch (Exception e) {
            log.error("❌ 素材上传任务初始化失败: oid={}, taskId={}, error={}", 
                    task.getOid(), task.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 更新任务进度
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    public void updateTaskProgress(String taskId, Integer progress) {
        try {
            taskService.updateTaskProgress(taskId, progress);
            log.debug("📊 任务进度更新: taskId={}, progress={}%", taskId, progress);
        } catch (Exception e) {
            log.error("❌ 任务进度更新失败: taskId={}, progress={}, error={}", 
                    taskId, progress, e.getMessage(), e);
        }
    }

    /**
     * 完成任务
     * @param taskId 任务ID
     * @param thumbnailUrl 缩略图链接（可选）
     */
    public void completeTask(String taskId, String thumbnailUrl) {
        try {
            taskService.completeTask(taskId, null, thumbnailUrl);
            log.info("✅ 任务完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("❌ 任务完成更新失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 任务失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    public void failTask(String taskId, String errorMessage) {
        try {
            taskService.failTask(taskId, errorMessage);
            log.error("❌ 任务失败: taskId={}, error={}", taskId, errorMessage);
        } catch (Exception e) {
            log.error("❌ 任务失败状态更新异常: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 任务状态
     */
    public void updateTaskStatus(String taskId, TaskStatusEnum status) {
        try {
            taskService.updateTaskStatus(taskId, status);
            log.info("✅ 任务状态更新: taskId={}, status={}", taskId, status);
        } catch (Exception e) {
            log.error("❌ 任务状态更新失败: taskId={}, status={}, error={}", 
                    taskId, status, e.getMessage(), e);
        }
    }
}

