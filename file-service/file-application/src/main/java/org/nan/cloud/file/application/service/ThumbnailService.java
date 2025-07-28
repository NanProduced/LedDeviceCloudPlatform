package org.nan.cloud.file.application.service;

import org.nan.cloud.file.application.domain.FileInfo;

import java.util.List;

/**
 * 缩略图生成服务接口
 * 
 * 提供图片和视频缩略图生成功能：
 * - 图片缩略图生成（多种尺寸）
 * - 视频封面提取
 * - 异步批量处理
 * - 缩略图质量优化
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface ThumbnailService {

    /**
     * 为图片文件生成缩略图
     * 
     * @param fileInfo 文件信息
     * @return 生成的缩略图信息
     */
    ThumbnailResult generateImageThumbnail(FileInfo fileInfo);

    /**
     * 为视频文件生成封面缩略图
     * 
     * @param fileInfo 文件信息
     * @param timeOffset 截取时间偏移（秒）
     * @return 生成的缩略图信息
     */
    ThumbnailResult generateVideoThumbnail(FileInfo fileInfo, double timeOffset);

    /**
     * 异步生成缩略图
     * 
     * @param fileInfo 文件信息
     */
    void generateThumbnailAsync(FileInfo fileInfo);

    /**
     * 批量生成缩略图
     * 
     * @param fileInfos 文件信息列表
     * @return 批量处理结果
     */
    BatchThumbnailResult batchGenerateThumbnails(List<FileInfo> fileInfos);

    /**
     * 生成指定尺寸的缩略图
     * 
     * @param fileInfo 文件信息
     * @param width 宽度
     * @param height 高度
     * @return 生成的缩略图信息
     */
    ThumbnailResult generateCustomSizeThumbnail(FileInfo fileInfo, int width, int height);

    /**
     * 删除文件的所有缩略图
     * 
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean deleteThumbnails(String fileId);

    /**
     * 获取文件的缩略图列表
     * 
     * @param fileId 文件ID
     * @return 缩略图列表
     */
    List<ThumbnailInfo> getThumbnails(String fileId);

    /**
     * 检查文件是否已有缩略图
     * 
     * @param fileId 文件ID
     * @return 是否已有缩略图
     */
    boolean hasThumbnails(String fileId);

    /**
     * 重新生成缩略图
     * 
     * @param fileInfo 文件信息
     * @return 重新生成的缩略图信息
     */
    ThumbnailResult regenerateThumbnail(FileInfo fileInfo);

    /**
     * 获取缩略图访问URL
     * 
     * @param thumbnailPath 缩略图路径
     * @return 访问URL
     */
    String getThumbnailUrl(String thumbnailPath);

    /**
     * 获取支持缩略图生成的文件类型
     * 
     * @return 支持的文件类型列表
     */
    List<String> getSupportedFileTypes();

    /**
     * 清理过期的缩略图临时文件
     * 
     * @param expireHours 过期时间（小时）
     * @return 清理的文件数量
     */
    int cleanupExpiredThumbnails(int expireHours);

    /**
     * 缩略图生成结果
     */
    class ThumbnailResult {
        private boolean success;
        private String errorMessage;
        private List<ThumbnailInfo> thumbnails;
        private long processingTime;

        public ThumbnailResult() {}

        public ThumbnailResult(boolean success) {
            this.success = success;
        }

        public ThumbnailResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static ThumbnailResult success(List<ThumbnailInfo> thumbnails) {
            ThumbnailResult result = new ThumbnailResult(true);
            result.setThumbnails(thumbnails);
            return result;
        }

        public static ThumbnailResult failure(String errorMessage) {
            return new ThumbnailResult(false, errorMessage);
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public List<ThumbnailInfo> getThumbnails() { return thumbnails; }
        public void setThumbnails(List<ThumbnailInfo> thumbnails) { this.thumbnails = thumbnails; }

        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
    }

    /**
     * 批量缩略图生成结果
     */
    class BatchThumbnailResult {
        private int totalFiles;
        private int successCount;
        private int failedCount;
        private List<String> failedFileIds;
        private long totalProcessingTime;

        public BatchThumbnailResult() {}

        public BatchThumbnailResult(int totalFiles) {
            this.totalFiles = totalFiles;
            this.successCount = 0;
            this.failedCount = 0;
            this.failedFileIds = new java.util.ArrayList<>();
        }

        public void addSuccess() {
            this.successCount++;
        }

        public void addFailure(String fileId) {
            this.failedCount++;
            if (this.failedFileIds == null) {
                this.failedFileIds = new java.util.ArrayList<>();
            }
            this.failedFileIds.add(fileId);
        }

        // Getters and Setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

        public List<String> getFailedFileIds() { return failedFileIds; }
        public void setFailedFileIds(List<String> failedFileIds) { this.failedFileIds = failedFileIds; }

        public long getTotalProcessingTime() { return totalProcessingTime; }
        public void setTotalProcessingTime(long totalProcessingTime) { this.totalProcessingTime = totalProcessingTime; }
    }

    /**
     * 缩略图信息
     */
    class ThumbnailInfo {
        private String thumbnailId;
        private String fileId;
        private String thumbnailPath;
        private int width;
        private int height;
        private long fileSize;
        private String format;
        private double quality;
        private java.time.LocalDateTime createTime;

        public ThumbnailInfo() {}

        public ThumbnailInfo(String fileId, String thumbnailPath, int width, int height) {
            this.fileId = fileId;
            this.thumbnailPath = thumbnailPath;
            this.width = width;
            this.height = height;
            this.createTime = java.time.LocalDateTime.now();
        }

        // Getters and Setters
        public String getThumbnailId() { return thumbnailId; }
        public void setThumbnailId(String thumbnailId) { this.thumbnailId = thumbnailId; }

        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }

        public String getThumbnailPath() { return thumbnailPath; }
        public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }

        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public double getQuality() { return quality; }
        public void setQuality(double quality) { this.quality = quality; }

        public java.time.LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    }

    /**
     * 缩略图尺寸枚举
     */
    enum ThumbnailSize {
        SMALL(150, 150, "小图"),
        MEDIUM(300, 300, "中图"),
        LARGE(600, 600, "大图"),
        CUSTOM(0, 0, "自定义");

        private final int width;
        private final int height;
        private final String description;

        ThumbnailSize(int width, int height, String description) {
            this.width = width;
            this.height = height;
            this.description = description;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public String getDescription() { return description; }
    }
}