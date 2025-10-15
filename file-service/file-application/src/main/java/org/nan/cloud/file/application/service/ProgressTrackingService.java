package org.nan.cloud.file.application.service;

import org.nan.cloud.file.api.dto.UploadProgressResponse;

/**
 * 进度跟踪服务接口
 * 
 * 提供文件上传和转码进度的跟踪功能：
 * - 上传进度监控
 * - 分片上传进度管理
 * - 转码进度跟踪
 * - 实时进度推送
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface ProgressTrackingService {

    /**
     * 初始化上传进度
     * 
     * @param taskId 任务ID
     * @param totalSize 文件总大小
     */
    void initializeProgress(String taskId, long totalSize);

    /**
     * 初始化分片上传进度
     * 
     * @param uploadId 上传ID
     * @param totalChunks 总分片数
     */
    void initializeChunkProgress(String uploadId, int totalChunks);

    /**
     * 更新上传进度
     * 
     * @param taskId 任务ID
     * @param uploadedSize 已上传大小
     */
    void updateProgress(String taskId, long uploadedSize);

    /**
     * 更新分片上传进度
     * 
     * @param uploadId 上传ID
     * @param chunkNumber 分片编号
     */
    void updateChunkProgress(String uploadId, int chunkNumber);

    /**
     * 完成进度跟踪
     * 
     * @param taskId 任务ID
     */
    void completeProgress(String taskId);

    /**
     * 取消进度跟踪
     * 
     * @param taskId 任务ID
     */
    void cancelProgress(String taskId);

    /**
     * 标记进度失败
     * 
     * @param taskId 任务ID
     * @param errorMessage 错误消息
     */
    void failProgress(String taskId, String errorMessage);

    /**
     * 获取进度信息
     * 
     * @param taskId 任务ID
     * @return 进度信息
     */
    UploadProgressResponse getProgress(String taskId);

    /**
     * 获取分片上传进度
     * 
     * @param uploadId 上传ID
     * @return 分片进度信息
     */
    ChunkProgressInfo getChunkProgress(String uploadId);

    /**
     * 设置进度更新回调
     * 
     * @param taskId 任务ID
     * @param callback 进度回调
     */
    void setProgressCallback(String taskId, ProgressCallback callback);

    /**
     * 清理过期的进度记录
     * 
     * @param expireMinutes 过期时间（分钟）
     * @return 清理的记录数量
     */
    int cleanupExpiredProgress(int expireMinutes);

    /**
     * 获取所有活跃的进度任务
     * 
     * @return 活跃任务列表
     */
    java.util.List<String> getActiveProgressTasks();

    /**
     * 检查任务是否存在
     * 
     * @param taskId 任务ID
     * @return 是否存在
     */
    boolean existsProgress(String taskId);

    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    ProgressStatus getProgressStatus(String taskId);

    /**
     * 更新进度消息
     * 
     * @param taskId 任务ID
     * @param message 进度消息
     */
    void updateProgressMessage(String taskId, String message);

    /**
     * 进度回调接口
     */
    @FunctionalInterface
    interface ProgressCallback {
        void onProgressUpdate(String taskId, int progress, String message);
    }

    /**
     * 分片进度信息
     */
    class ChunkProgressInfo {
        private String uploadId;
        private int totalChunks;
        private int completedChunks;
        private int progress;
        private ProgressStatus status;
        private String message;
        private long startTime;
        private long updateTime;

        public ChunkProgressInfo() {}

        public ChunkProgressInfo(String uploadId, int totalChunks) {
            this.uploadId = uploadId;
            this.totalChunks = totalChunks;
            this.completedChunks = 0;
            this.progress = 0;
            this.status = ProgressStatus.IN_PROGRESS;
            this.startTime = System.currentTimeMillis();
            this.updateTime = this.startTime;
        }

        public void updateProgress(int completedChunks) {
            this.completedChunks = completedChunks;
            this.progress = totalChunks > 0 ? (completedChunks * 100 / totalChunks) : 0;
            this.updateTime = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }

        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }

        public int getCompletedChunks() { return completedChunks; }
        public void setCompletedChunks(int completedChunks) { this.completedChunks = completedChunks; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public ProgressStatus getStatus() { return status; }
        public void setStatus(ProgressStatus status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getUpdateTime() { return updateTime; }
        public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
    }

    /**
     * 进度状态枚举
     */
    enum ProgressStatus {
        PENDING("等待中"),
        IN_PROGRESS("进行中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");

        private final String description;

        ProgressStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}