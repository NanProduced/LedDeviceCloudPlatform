package org.nan.cloud.file.application.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务上下文领域对象
 * 
 * 存储上传任务的完整上下文信息，用于异步处理时获取用户信息。
 * 支持任务状态跟踪和进度管理。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskContext {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private Long uid;

    /**
     * 组织ID
     */
    private Long oid;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 任务进度 (0-100)
     */
    private Integer progress;

    /**
     * 素材ID（创建完成后设置）
     */
    private Long materialId;

    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 任务更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        /**
         * 已创建
         */
        CREATED("created", "已创建"),
        
        /**
         * 上传中
         */
        UPLOADING("uploading", "上传中"),
        
        /**
         * 上传完成
         */
        UPLOADED("uploaded", "上传完成"),
        
        /**
         * 处理中
         */
        PROCESSING("processing", "处理中"),
        
        /**
         * 已完成
         */
        COMPLETED("completed", "已完成"),
        
        /**
         * 失败
         */
        FAILED("failed", "失败");

        private final String code;
        private final String description;

        TaskStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 任务创建时的静态工厂方法
     */
    public static TaskContext create(String taskId, String fileId, Long uid, Long oid, 
                                   String originalFilename, Long fileSize) {
        return TaskContext.builder()
                .taskId(taskId)
                .fileId(fileId)
                .uid(uid)
                .oid(oid)
                .originalFilename(originalFilename)
                .fileSize(fileSize)
                .status(TaskStatus.CREATED)
                .progress(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 更新任务状态
     */
    public void updateStatus(TaskStatus newStatus) {
        this.status = newStatus;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新任务进度
     */
    public void updateProgress(Integer newProgress) {
        this.progress = newProgress;
        this.updateTime = LocalDateTime.now();
        
        // 根据进度自动更新状态
        if (newProgress >= 100 && this.status == TaskStatus.UPLOADING) {
            this.status = TaskStatus.UPLOADED;
        }
    }

    /**
     * 标记任务失败
     */
    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记任务完成
     */
    public void markCompleted(Long materialId) {
        this.status = TaskStatus.COMPLETED;
        this.materialId = materialId;
        this.progress = 100;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 检查任务是否已完成（成功或失败）
     */
    public boolean isFinished() {
        return status == TaskStatus.COMPLETED || status == TaskStatus.FAILED;
    }

    /**
     * 检查任务是否成功完成
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    /**
     * 检查任务是否失败
     */
    public boolean isFailed() {
        return status == TaskStatus.FAILED;
    }
}