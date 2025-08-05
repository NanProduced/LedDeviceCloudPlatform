package org.nan.cloud.file.application.service;

import lombok.Getter;
import lombok.Setter;
import org.nan.cloud.file.api.dto.ChunkUploadInitRequest;
import org.nan.cloud.file.api.dto.ChunkUploadRequest;
import org.nan.cloud.file.api.dto.FileUploadRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * 
 * 提供统一的文件存储抽象，支持多种存储策略：
 * - 本地文件系统存储
 * - 阿里云OSS存储
 * - 自动存储策略选择
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface StorageService {

    /**
     * 存储单个文件
     * 
     * @param file 上传的文件
     * @param request 上传请求参数
     * @param fileId 文件ID
     * @return 存储路径
     */
    String store(MultipartFile file, FileUploadRequest request, String fileId);

    /**
     * 存储单个文件（简化版本）
     * 
     * @param file 上传的文件
     * @param fileId 文件ID
     * @return 存储路径
     */
    String store(MultipartFile file, String fileId);

    /**
     * 初始化分片上传
     * 
     * @param uploadId 上传ID
     * @param request 分片上传初始化请求
     * @return 初始化结果信息
     */
    String initChunkUpload(String uploadId, ChunkUploadInitRequest request);

    /**
     * 上传文件分片
     * 
     * @param chunk 文件分片
     * @param request 分片上传请求
     * @return 分片存储路径
     */
    String uploadChunk(MultipartFile chunk, ChunkUploadRequest request);

    /**
     * 合并文件分片
     * 
     * @param uploadId 上传ID
     * @return 最终文件存储路径
     */
    String mergeChunks(String uploadId);

    /**
     * 清理分片临时文件
     * 
     * @param uploadId 上传ID
     */
    void cleanupChunks(String uploadId);

    /**
     * 检查所有分片是否已上传完成
     * 
     * @param uploadId 上传ID
     * @return 是否所有分片都已上传
     */
    boolean isAllChunksUploaded(String uploadId);

    /**
     * 计算合并后文件的MD5值
     * 
     * @param filePath 文件路径
     * @return MD5哈希值
     */
    String calculateMergedFileMD5(String filePath);

    /**
     * 获取分片上传初始化信息
     * 
     * @param uploadId 上传ID
     * @return 初始化请求信息
     */
    ChunkUploadInitRequest getChunkUploadInfo(String uploadId);

    /**
     * 生成文件访问URL
     * 
     * @param storagePath 存储路径
     * @return 访问URL
     */
    String generateAccessUrl(String storagePath);

    /**
     * 生成带过期时间的临时访问URL
     * 
     * @param storagePath 存储路径
     * @param expireMinutes 过期时间（分钟）
     * @return 临时访问URL
     */
    String generateTemporaryUrl(String storagePath, int expireMinutes);

    /**
     * 删除文件
     * 
     * @param storagePath 存储路径
     * @return 是否删除成功
     */
    boolean deleteFile(String storagePath);

    /**
     * 批量删除文件
     * 
     * @param storagePaths 存储路径列表
     * @return 删除成功的文件数量
     */
    int batchDeleteFiles(String[] storagePaths);

    /**
     * 复制文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否复制成功
     */
    boolean copyFile(String sourcePath, String targetPath);

    /**
     * 移动文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否移动成功
     */
    boolean moveFile(String sourcePath, String targetPath);

    /**
     * 检查文件是否存在
     * 
     * @param storagePath 存储路径
     * @return 是否存在
     */
    boolean fileExists(String storagePath);

    /**
     * 获取文件大小
     * 
     * @param storagePath 存储路径
     * @return 文件大小（字节），如果文件不存在返回-1
     */
    long getFileSize(String storagePath);

    /**
     * 获取文件的最后修改时间
     * 
     * @param storagePath 存储路径
     * @return 最后修改时间戳，如果文件不存在返回-1
     */
    long getLastModified(String storagePath);

    /**
     * 获取存储统计信息
     * 
     * @return 存储统计信息
     */
    StorageStatistics getStorageStatistics();

    /**
     * 获取当前存储策略类型
     * 
     * @return 存储策略类型
     */
    StorageStrategy getStorageStrategy();

    /**
     * 健康检查
     * 
     * @return 存储服务是否健康
     */
    boolean healthCheck();

    /**
     * 获取文件的绝对路径
     * 
     * @param storagePath 存储路径
     * @return 文件的绝对路径
     */
    String getAbsolutePath(String storagePath);

    /**
     * 存储策略枚举
     */
    enum StorageStrategy {
        LOCAL("本地存储"),
        OSS("阿里云OSS"),
        AUTO("自动选择");

        private final String description;

        StorageStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 存储统计信息
     */
    @Setter
    @Getter
    class StorageStatistics {
        // Getters and Setters
        private long totalFiles;
        private long totalSize;
        private long usedSpace;
        private long availableSpace;
        private double usageRate;

        public StorageStatistics() {}

        public StorageStatistics(long totalFiles, long totalSize, long usedSpace, long availableSpace) {
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.usedSpace = usedSpace;
            this.availableSpace = availableSpace;
            this.usageRate = availableSpace > 0 ? (double) usedSpace / (usedSpace + availableSpace) : 0.0;
        }

    }
}