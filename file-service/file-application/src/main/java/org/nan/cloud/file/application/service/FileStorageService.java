package org.nan.cloud.file.application.service;

import org.nan.cloud.file.application.domain.FileInfo;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储服务接口
 * 
 * 提供文件存储的核心功能：
 * - 文件上传和存储
 * - 文件检索和下载
 * - 文件删除和管理
 * - 存储策略选择
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface FileStorageService {

    /**
     * 存储文件
     * 
     * @param inputStream 文件输入流
     * @param fileInfo 文件信息
     * @return 存储后的文件ID
     */
    String storeFile(InputStream inputStream, FileInfo fileInfo);

    /**
     * 获取文件流
     * 
     * @param fileId 文件ID
     * @return 文件输入流
     */
    InputStream getFileStream(String fileId);

    /**
     * 获取文件信息
     * 
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileInfo getFileInfo(String fileId);
    
    /**
     * 获取文件存储信息
     * 
     * @param fileId 文件ID
     * @return 文件存储信息
     */
    FileStorageInfo getFileStorageInfo(String fileId);

    /**
     * 删除文件
     * 
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean deleteFile(String fileId);

    /**
     * 检查文件是否存在
     * 
     * @param fileId 文件ID
     * @return 是否存在
     */
    boolean fileExists(String fileId);

    /**
     * 批量删除文件
     * 
     * @param fileIds 文件ID列表
     * @return 删除成功的文件数量
     */
    int batchDeleteFiles(List<String> fileIds);

    /**
     * 获取文件访问URL
     * 
     * @param fileId 文件ID
     * @return 访问URL
     */
    String getFileAccessUrl(String fileId);

    /**
     * 获取文件存储路径
     * 
     * @param fileId 文件ID
     * @return 存储路径
     */
    String getStoragePath(String fileId);

    /**
     * 复制文件
     * 
     * @param sourceFileId 源文件ID
     * @param targetFileId 目标文件ID
     * @return 是否复制成功
     */
    boolean copyFile(String sourceFileId, String targetFileId);

    /**
     * 移动文件
     * 
     * @param sourceFileId 源文件ID
     * @param targetFileId 目标文件ID
     * @return 是否移动成功
     */
    boolean moveFile(String sourceFileId, String targetFileId);

    /**
     * 获取存储策略类型
     * 
     * @return 存储策略类型
     */
    String getStorageType();

    /**
     * 获取存储统计信息
     * 
     * @return 存储统计信息
     */
    StorageStatistics getStorageStatistics();

    /**
     * 文件存储信息
     */
    class FileStorageInfo {
        private String fileId;
        private String originalFilename;
        private long fileSize;
        private String mimeType;
        private String storagePath;
        private String md5Hash;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        
        // Getters and Setters
        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        
        public String getStoragePath() { return storagePath; }
        public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
        
        public String getMd5Hash() { return md5Hash; }
        public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }
        
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    /**
     * 存储统计信息
     */
    class StorageStatistics {
        private long totalFiles;
        private long totalSize;
        private long availableSpace;
        private String storageType;

        // Getters and Setters
        public long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }

        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

        public long getAvailableSpace() { return availableSpace; }
        public void setAvailableSpace(long availableSpace) { this.availableSpace = availableSpace; }

        public String getStorageType() { return storageType; }
        public void setStorageType(String storageType) { this.storageType = storageType; }
    }
}