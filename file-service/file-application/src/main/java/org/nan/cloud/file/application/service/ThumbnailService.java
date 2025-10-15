package org.nan.cloud.file.application.service;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.nan.cloud.file.application.domain.FileInfo;

import java.io.InputStream;
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
     * 异步生成缩略图（带回调）
     * 
     * @param fileInfo 文件信息
     * @param callback 缩略图生成完成后的回调
     */
    void generateThumbnailAsync(FileInfo fileInfo, ThumbnailCallback callback);

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
     * 生成缩略图并返回输入流（用于预览接口）
     * 
     * @param request 缩略图生成请求
     * @return 缩略图输入流
     */
    InputStream generateThumbnail(ThumbnailRequest request);
    
    /**
     * 生成视频帧缩略图并返回输入流
     * 
     * @param request 缩略图生成请求
     * @return 视频帧缩略图输入流
     */
    InputStream generateVideoFrameThumbnail(ThumbnailRequest request);
    
    /**
     * 缩略图生成请求参数
     */
    @Data
    @Builder
    class ThumbnailRequest {
        /** 源文件ID */
        private String sourceFileId;
        
        /** 目标宽度 */
        private Integer targetWidth;
        
        /** 目标高度 */
        private Integer targetHeight;
        
        /** 适应方式 */
        @Builder.Default
        private String fit = "cover";
        
        /** 输出格式 */
        @Builder.Default
        private String outputFormat = "jpg";
        
        /** 图片质量 (1-100) */
        @Builder.Default
        private Integer quality = 85;
        
        /** 视频时间偏移（秒） */
        @Builder.Default
        private Double timeOffset = 1.0;
        
        /** 视频帧数 */
        private Integer frameNumber;
    }

    /**
     * 缩略图生成结果
     */
    @Data
    class ThumbnailResult {
        // Getters and Setters
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
    @Data
    class ThumbnailInfo {
        // Getters and Setters
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

    }

    /**
     * 缩略图生成完成回调接口
     */
    @FunctionalInterface
    interface ThumbnailCallback {
        /**
         * 缩略图生成完成回调
         * 
         * @param fileId 文件ID
         * @param result 生成结果
         */
        void onThumbnailGenerated(String fileId, ThumbnailResult result);
    }

}