package org.nan.cloud.file.infrastructure.progress;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.api.dto.UploadProgressResponse;
import org.nan.cloud.file.application.service.ProgressTrackingService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 进度跟踪服务实现 - 内存缓存版本
 * 
 * 开发环境实现，使用ConcurrentHashMap作为内存缓存。
 * 生产环境建议使用Redis实现以支持分布式场景。
 * 
 * 特性：
 * 1. 线程安全的进度存储和更新
 * 2. 实时进度计算（速度、剩余时间等）
 * 3. 自动过期清理（默认1小时）
 * 4. 支持单文件和分片上传进度跟踪
 * 5. 质量指标统计（平均速度、稳定性等）
 * 6. 进度回调支持
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ProgressTrackingServiceImpl implements ProgressTrackingService {

    /**
     * 进度信息存储 - 线程安全
     */
    private final ConcurrentHashMap<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();
    
    /**
     * 分片进度信息存储
     */
    private final ConcurrentHashMap<String, ChunkProgressInfo> chunkProgressMap = new ConcurrentHashMap<>();
    
    /**
     * 进度回调存储
     */
    private final ConcurrentHashMap<String, ProgressCallback> callbackMap = new ConcurrentHashMap<>();
    
    /**
     * 读写锁，保护复杂操作
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 定时清理服务
     */
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ProgressTracking-Cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 默认过期时间：1小时
     */
    private static final int DEFAULT_EXPIRE_MINUTES = 60;

    @PostConstruct
    public void initialize() {
        // 启动定时清理任务，每15分钟执行一次
        cleanupExecutor.scheduleWithFixedDelay(
            () -> cleanupExpiredProgress(DEFAULT_EXPIRE_MINUTES),
            15, 15, TimeUnit.MINUTES
        );
        
        log.info("ProgressTrackingService initialized with memory cache, auto-cleanup every 15 minutes");
    }

    @Override
    public void initializeProgress(String taskId, long totalSize) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId 不能为空");
        }

        ProgressInfo progressInfo = new ProgressInfo(taskId, totalSize);
        progressMap.put(taskId, progressInfo);
        
        log.debug("初始化单文件上传进度 - 任务ID: {}, 总大小: {}B", taskId, totalSize);
        
        // 触发回调
        triggerCallback(taskId, 0, "上传初始化完成");
    }

    @Override
    public void initializeChunkProgress(String uploadId, int totalChunks) {
        if (uploadId == null) {
            throw new IllegalArgumentException("uploadId 不能为空");
        }

        ChunkProgressInfo chunkInfo = new ChunkProgressInfo(uploadId, totalChunks);
        chunkProgressMap.put(uploadId, chunkInfo);
        
        // 同时创建一个通用的进度信息用于统一查询
        ProgressInfo progressInfo = new ProgressInfo(uploadId, 0);
        progressInfo.setStatus(ProgressStatus.IN_PROGRESS);
        progressInfo.setTotalChunks(totalChunks);
        progressInfo.setUploadedChunks(0);
        progressMap.put(uploadId, progressInfo);
        
        log.debug("初始化分片上传进度 - 上传ID: {}, 总分片数: {}", uploadId, totalChunks);
        
        // 触发回调
        triggerCallback(uploadId, 0, "分片上传初始化完成");
    }

    @Override
    public void updateProgress(String taskId, long uploadedSize) {
        ProgressInfo progressInfo = progressMap.get(taskId);
        if (progressInfo == null) {
            log.warn("进度信息不存在，无法更新 - 任务ID: {}", taskId);
            return;
        }

        lock.writeLock().lock();
        try {
            long previousUploaded = progressInfo.getUploadedSize();
            progressInfo.updateProgress(uploadedSize);
            
            // 计算上传速度
            long deltaBytes = uploadedSize - previousUploaded;
            long deltaTime = System.currentTimeMillis() - progressInfo.getLastUpdateTime();
            if (deltaTime > 0) {
                long currentSpeed = (deltaBytes * 1000) / deltaTime; // 字节/秒
                progressInfo.updateSpeed(currentSpeed);
            }
            
            log.debug("更新单文件上传进度 - 任务ID: {}, 进度: {}%, 速度: {}KB/s", 
                    taskId, progressInfo.getProgress(), progressInfo.getUploadSpeed() / 1024);
            
            // 触发回调
            triggerCallback(taskId, (int) progressInfo.getProgress(), 
                    String.format("已上传 %.1f%%", progressInfo.getProgress()));
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void updateChunkProgress(String uploadId, int chunkNumber) {
        ChunkProgressInfo chunkInfo = chunkProgressMap.get(uploadId);
        ProgressInfo progressInfo = progressMap.get(uploadId);
        
        if (chunkInfo == null || progressInfo == null) {
            log.warn("分片进度信息不存在，无法更新 - 上传ID: {}", uploadId);
            return;
        }

        lock.writeLock().lock();
        try {
            int previousCompleted = chunkInfo.getCompletedChunks();
            chunkInfo.updateProgress(chunkInfo.getCompletedChunks() + 1);
            
            // 更新通用进度信息
            progressInfo.setUploadedChunks(chunkInfo.getCompletedChunks());
            progressInfo.setProgress(chunkInfo.getProgress());
            progressInfo.setLastUpdateTime(System.currentTimeMillis());
            
            log.debug("更新分片上传进度 - 上传ID: {}, 完成分片: {}/{}, 进度: {}%", 
                    uploadId, chunkInfo.getCompletedChunks(), chunkInfo.getTotalChunks(), chunkInfo.getProgress());
            
            // 触发回调
            triggerCallback(uploadId, chunkInfo.getProgress(), 
                    String.format("已完成 %d/%d 分片", chunkInfo.getCompletedChunks(), chunkInfo.getTotalChunks()));
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void completeProgress(String taskId) {
        ProgressInfo progressInfo = progressMap.get(taskId);
        if (progressInfo != null) {
            progressInfo.setStatus(ProgressStatus.COMPLETED);
            progressInfo.setProgress(100.0);
            progressInfo.setLastUpdateTime(System.currentTimeMillis());
            
            log.debug("标记进度完成 - 任务ID: {}", taskId);
            
            // 触发回调
            triggerCallback(taskId, 100, "上传完成");
        }
        
        // 同时更新分片进度
        ChunkProgressInfo chunkInfo = chunkProgressMap.get(taskId);
        if (chunkInfo != null) {
            chunkInfo.setStatus(ProgressStatus.COMPLETED);
            chunkInfo.setProgress(100);
            chunkInfo.setUpdateTime(System.currentTimeMillis());
        }
    }

    @Override
    public void cancelProgress(String taskId) {
        ProgressInfo progressInfo = progressMap.get(taskId);
        if (progressInfo != null) {
            progressInfo.setStatus(ProgressStatus.CANCELLED);
            progressInfo.setLastUpdateTime(System.currentTimeMillis());
            
            log.debug("取消进度跟踪 - 任务ID: {}", taskId);
            
            // 触发回调
            triggerCallback(taskId, (int) progressInfo.getProgress(), "上传已取消");
        }
        
        // 同时更新分片进度
        ChunkProgressInfo chunkInfo = chunkProgressMap.get(taskId);
        if (chunkInfo != null) {
            chunkInfo.setStatus(ProgressStatus.CANCELLED);
            chunkInfo.setUpdateTime(System.currentTimeMillis());
        }
    }

    @Override
    public void failProgress(String taskId, String errorMessage) {
        ProgressInfo progressInfo = progressMap.get(taskId);
        if (progressInfo != null) {
            progressInfo.setStatus(ProgressStatus.FAILED);
            progressInfo.setErrorMessage(errorMessage);
            progressInfo.setLastUpdateTime(System.currentTimeMillis());
            
            log.debug("标记进度失败 - 任务ID: {}, 错误: {}", taskId, errorMessage);
            
            // 触发回调
            triggerCallback(taskId, (int) progressInfo.getProgress(), "上传失败: " + errorMessage);
        }
        
        // 同时更新分片进度
        ChunkProgressInfo chunkInfo = chunkProgressMap.get(taskId);
        if (chunkInfo != null) {
            chunkInfo.setStatus(ProgressStatus.FAILED);
            chunkInfo.setMessage("上传失败: " + errorMessage);
            chunkInfo.setUpdateTime(System.currentTimeMillis());
        }
    }

    @Override
    public UploadProgressResponse getProgress(String taskId) {
        ProgressInfo progressInfo = progressMap.get(taskId);
        if (progressInfo == null) {
            return null;
        }

        // 检查是否是分片上传
        ChunkProgressInfo chunkInfo = chunkProgressMap.get(taskId);
        
        UploadProgressResponse.UploadProgressResponseBuilder builder = UploadProgressResponse.builder()
                .uploadId(taskId)
                .fileName(progressInfo.getFileName())
                .status(progressInfo.getStatus().name())
                .progress(progressInfo.getProgress())
                .uploadedSize(progressInfo.getUploadedSize())
                .totalSize(progressInfo.getTotalSize())
                .uploadSpeed(progressInfo.getUploadSpeed())
                .estimatedTimeRemaining(calculateEstimatedTime(progressInfo))
                .startTime(progressInfo.getStartTimeAsLocalDateTime())
                .lastUpdateTime(progressInfo.getLastUpdateTimeAsLocalDateTime())
                .errorMessage(progressInfo.getErrorMessage())
                .retryCount(progressInfo.getRetryCount());

        // 如果是分片上传，添加分片相关信息
        if (chunkInfo != null) {
            builder.uploadedChunks(chunkInfo.getCompletedChunks())
                   .totalChunks(chunkInfo.getTotalChunks())
                   .failedChunks(progressInfo.getFailedChunks());
        }

        // 添加质量指标
        UploadProgressResponse.QualityMetrics qualityMetrics = UploadProgressResponse.QualityMetrics.builder()
                .avgSpeed(progressInfo.getAverageSpeed())
                .maxSpeed(progressInfo.getMaxSpeed())
                .minSpeed(progressInfo.getMinSpeed())
                .stability(calculateStability(progressInfo))
                .build();
        
        builder.qualityMetrics(qualityMetrics);

        return builder.build();
    }

    @Override
    public ChunkProgressInfo getChunkProgress(String uploadId) {
        return chunkProgressMap.get(uploadId);
    }

    @Override
    public void setProgressCallback(String taskId, ProgressCallback callback) {
        if (callback != null) {
            callbackMap.put(taskId, callback);
            log.debug("设置进度回调 - 任务ID: {}", taskId);
        } else {
            callbackMap.remove(taskId);
            log.debug("移除进度回调 - 任务ID: {}", taskId);
        }
    }

    @Override
    public int cleanupExpiredProgress(int expireMinutes) {
        long expireTime = System.currentTimeMillis() - (expireMinutes * 60 * 1000);
        int cleanedCount = 0;

        // 清理普通进度记录
        for (var iterator = progressMap.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            ProgressInfo progressInfo = entry.getValue();
            
            // 清理过期或已完成的任务
            if (progressInfo.getLastUpdateTime() < expireTime || 
                progressInfo.getStatus() == ProgressStatus.COMPLETED ||
                progressInfo.getStatus() == ProgressStatus.FAILED ||
                progressInfo.getStatus() == ProgressStatus.CANCELLED) {
                
                iterator.remove();
                callbackMap.remove(entry.getKey()); // 同时清理回调
                cleanedCount++;
                
                log.debug("清理过期进度记录 - 任务ID: {}, 状态: {}", 
                        entry.getKey(), progressInfo.getStatus());
            }
        }

        // 清理分片进度记录
        for (var iterator = chunkProgressMap.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            ChunkProgressInfo chunkInfo = entry.getValue();
            
            // 清理过期或已完成的任务
            if (chunkInfo.getUpdateTime() < expireTime || 
                chunkInfo.getStatus() == ProgressStatus.COMPLETED ||
                chunkInfo.getStatus() == ProgressStatus.FAILED ||
                chunkInfo.getStatus() == ProgressStatus.CANCELLED) {
                
                iterator.remove();
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("清理过期进度记录完成 - 清理数量: {}, 剩余进度记录: {}", cleanedCount, progressMap.size());
        }

        return cleanedCount;
    }

    @Override
    public List<String> getActiveProgressTasks() {
        return progressMap.entrySet().stream()
                .filter(entry -> entry.getValue().getStatus() == ProgressStatus.IN_PROGRESS)
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public boolean existsProgress(String taskId) {
        return progressMap.containsKey(taskId);
    }

    @Override
    public ProgressStatus getProgressStatus(String taskId) {
        ProgressInfo progressInfo = progressMap.get(taskId);
        return progressInfo != null ? progressInfo.getStatus() : null;
    }

    @Override
    public void updateProgressMessage(String taskId, String message) {
        ProgressInfo progressInfo = progressMap.get(taskId);
        if (progressInfo != null) {
            progressInfo.setMessage(message);
            progressInfo.setLastUpdateTime(System.currentTimeMillis());
            
            log.debug("更新进度消息 - 任务ID: {}, 消息: {}", taskId, message);
            
            // 触发回调
            triggerCallback(taskId, (int) progressInfo.getProgress(), message);
        }
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 触发进度回调
     */
    private void triggerCallback(String taskId, int progress, String message) {
        ProgressCallback callback = callbackMap.get(taskId);
        if (callback != null) {
            try {
                callback.onProgressUpdate(taskId, progress, message);
            } catch (Exception e) {
                log.error("进度回调执行失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * 计算预计剩余时间
     */
    private Long calculateEstimatedTime(ProgressInfo progressInfo) {
        if (progressInfo.getUploadSpeed() <= 0 || progressInfo.getProgress() >= 100) {
            return 0L;
        }
        
        long remainingBytes = progressInfo.getTotalSize() - progressInfo.getUploadedSize();
        return remainingBytes / progressInfo.getUploadSpeed(); // 秒
    }
    
    /**
     * 计算连接稳定性
     */
    private Double calculateStability(ProgressInfo progressInfo) {
        if (progressInfo.getSpeedHistory().size() < 2) {
            return 1.0;
        }
        
        List<Long> speeds = progressInfo.getSpeedHistory();
        double avg = speeds.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        double variance = speeds.stream()
                .mapToDouble(speed -> Math.pow(speed - avg, 2))
                .average()
                .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        return Math.max(0.0, 1.0 - (stdDev / Math.max(avg, 1.0)));
    }

    /**
     * 获取当前活跃任务数量（用于监控）
     */
    public int getCurrentProgressCount() {
        return progressMap.size();
    }

    /**
     * 关闭清理服务
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        log.info("ProgressTrackingService shutdown completed");
    }

    // ==================== 内部进度信息类 ====================
    
    /**
     * 内部进度信息类
     */
    private static class ProgressInfo {
        private String taskId;
        private String fileName;
        private long totalSize;
        private long uploadedSize;
        private double progress;
        private ProgressStatus status;
        private String message;
        private String errorMessage;
        private long startTime;
        private long lastUpdateTime;
        private int retryCount;
        
        // 速度统计
        private long uploadSpeed; // 当前速度
        private long maxSpeed;
        private long minSpeed;
        private long totalSpeedSum;
        private int speedMeasurements;
        private List<Long> speedHistory;
        
        // 分片相关
        private Integer totalChunks;
        private Integer uploadedChunks;
        private List<Integer> failedChunks;

        public ProgressInfo(String taskId, long totalSize) {
            this.taskId = taskId;
            this.totalSize = totalSize;
            this.uploadedSize = 0;
            this.progress = 0.0;
            this.status = ProgressStatus.IN_PROGRESS;
            this.startTime = System.currentTimeMillis();
            this.lastUpdateTime = this.startTime;
            this.retryCount = 0;
            this.uploadSpeed = 0;
            this.maxSpeed = 0;
            this.minSpeed = Long.MAX_VALUE;
            this.totalSpeedSum = 0;
            this.speedMeasurements = 0;
            this.speedHistory = new ArrayList<>();
            this.failedChunks = new ArrayList<>();
        }

        public void updateProgress(long uploadedSize) {
            this.uploadedSize = uploadedSize;
            this.progress = totalSize > 0 ? (double) uploadedSize * 100.0 / totalSize : 0.0;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public void updateSpeed(long currentSpeed) {
            this.uploadSpeed = currentSpeed;
            this.maxSpeed = Math.max(this.maxSpeed, currentSpeed);
            this.minSpeed = Math.min(this.minSpeed, currentSpeed);
            this.totalSpeedSum += currentSpeed;
            this.speedMeasurements++;
            
            // 保留最近20个速度记录
            this.speedHistory.add(currentSpeed);
            if (this.speedHistory.size() > 20) {
                this.speedHistory.remove(0);
            }
        }
        
        public long getAverageSpeed() {
            return speedMeasurements > 0 ? totalSpeedSum / speedMeasurements : 0;
        }
        
        public LocalDateTime getStartTimeAsLocalDateTime() {
            return LocalDateTime.ofEpochSecond(startTime / 1000, (int) (startTime % 1000) * 1000000, 
                    java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now()));
        }
        
        public LocalDateTime getLastUpdateTimeAsLocalDateTime() {
            return LocalDateTime.ofEpochSecond(lastUpdateTime / 1000, (int) (lastUpdateTime % 1000) * 1000000, 
                    java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now()));
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        
        public long getUploadedSize() { return uploadedSize; }
        public void setUploadedSize(long uploadedSize) { this.uploadedSize = uploadedSize; }
        
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
        
        public ProgressStatus getStatus() { return status; }
        public void setStatus(ProgressStatus status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public long getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        
        public long getUploadSpeed() { return uploadSpeed; }
        public void setUploadSpeed(long uploadSpeed) { this.uploadSpeed = uploadSpeed; }
        
        public long getMaxSpeed() { return maxSpeed; }
        public void setMaxSpeed(long maxSpeed) { this.maxSpeed = maxSpeed; }
        
        public long getMinSpeed() { return minSpeed == Long.MAX_VALUE ? 0 : minSpeed; }
        public void setMinSpeed(long minSpeed) { this.minSpeed = minSpeed; }
        
        public List<Long> getSpeedHistory() { return speedHistory; }
        public void setSpeedHistory(List<Long> speedHistory) { this.speedHistory = speedHistory; }
        
        public Integer getTotalChunks() { return totalChunks; }
        public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
        
        public Integer getUploadedChunks() { return uploadedChunks; }
        public void setUploadedChunks(Integer uploadedChunks) { this.uploadedChunks = uploadedChunks; }
        
        public List<Integer> getFailedChunks() { return failedChunks; }
        public void setFailedChunks(List<Integer> failedChunks) { this.failedChunks = failedChunks; }
    }
}