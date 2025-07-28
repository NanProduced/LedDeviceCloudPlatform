package org.nan.cloud.file.application.service;

import org.nan.cloud.file.api.dto.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件管理服务接口
 * 
 * 提供文件管理相关的业务逻辑处理：
 * - 文件信息查询和管理
 * - 文件下载和预览
 * - 文件夹结构管理
 * - 文件权限控制
 * - 文件版本管理
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface FileManagementService {

    /**
     * 获取文件信息
     * 
     * @param fileId 文件ID
     * @return 文件详细信息
     */
    FileInfoResponse getFileInfo(String fileId);

    /**
     * 获取文件列表
     * 
     * @param request 文件查询请求
     * @return 文件列表
     */
    FileListResponse getFileList(FileListRequest request);

    /**
     * 搜索文件
     * 
     * @param request 文件搜索请求
     * @return 搜索结果
     */
    FileSearchResponse searchFiles(FileSearchRequest request);

    /**
     * 下载文件
     * 
     * @param fileId 文件ID
     * @param response HTTP响应对象
     */
    void downloadFile(String fileId, HttpServletResponse response);

    /**
     * 获取文件预览URL
     * 
     * @param fileId 文件ID
     * @param request 预览请求参数
     * @return 预览URL
     */
    FilePreviewUrlResponse getPreviewUrl(String fileId, FilePreviewRequest request);

    /**
     * 删除文件
     * 
     * @param fileId 文件ID
     * @return 删除是否成功
     */
    boolean deleteFile(String fileId);

    /**
     * 批量删除文件
     * 
     * @param request 批量删除请求
     * @return 删除结果
     */
    BatchFileDeleteResponse batchDeleteFiles(BatchFileDeleteRequest request);

    /**
     * 移动文件
     * 
     * @param request 文件移动请求
     * @return 移动是否成功
     */
    boolean moveFile(FileMoveRequest request);

    /**
     * 复制文件
     * 
     * @param request 文件复制请求
     * @return 复制结果
     */
    FileCopyResponse copyFile(FileCopyRequest request);

    /**
     * 重命名文件
     * 
     * @param request 文件重命名请求
     * @return 重命名是否成功
     */
    boolean renameFile(FileRenameRequest request);

    /**
     * 创建文件夹
     * 
     * @param request 创建文件夹请求
     * @return 创建结果
     */
    FolderCreateResponse createFolder(FolderCreateRequest request);

    /**
     * 获取文件夹树形结构
     * 
     * @param organizationId 组织ID
     * @return 文件夹树形结构
     */
    List<FolderTreeResponse> getFolderTree(String organizationId);

    /**
     * 获取文件统计信息
     * 
     * @param request 统计请求参数
     * @return 统计信息
     */
    FileStatisticsResponse getFileStatistics(FileStatisticsRequest request);

    /**
     * 获取文件版本历史
     * 
     * @param fileId 文件ID
     * @return 版本历史列表
     */
    List<FileVersionResponse> getFileVersions(String fileId);

    /**
     * 恢复文件版本
     * 
     * @param request 版本恢复请求
     * @return 恢复是否成功
     */
    boolean restoreFileVersion(FileVersionRestoreRequest request);

    /**
     * 更新文件元数据
     * 
     * @param fileId 文件ID
     * @param metadata 新的元数据
     * @return 更新是否成功
     */
    boolean updateFileMetadata(String fileId, String metadata);

    /**
     * 更新文件标签
     * 
     * @param fileId 文件ID
     * @param tags 标签列表
     * @return 更新是否成功
     */
    boolean updateFileTags(String fileId, List<String> tags);

    /**
     * 设置文件权限
     * 
     * @param fileId 文件ID
     * @param isPublic 是否公开
     * @return 设置是否成功
     */
    boolean setFilePermission(String fileId, boolean isPublic);

    /**
     * 获取文件访问日志
     * 
     * @param fileId 文件ID
     * @param page 页码
     * @param size 每页大小
     * @return 访问日志列表
     */
    List<FileAccessLog> getFileAccessLogs(String fileId, int page, int size);

    /**
     * 记录文件访问
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param accessType 访问类型
     * @param userAgent 用户代理
     * @param ipAddress IP地址
     */
    void recordFileAccess(String fileId, String userId, String accessType, String userAgent, String ipAddress);

    /**
     * 检查文件访问权限
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param accessType 访问类型
     * @return 是否有权限
     */
    boolean checkFilePermission(String fileId, String userId, String accessType);

    /**
     * 获取最近上传的文件
     * 
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 最近上传的文件列表
     */
    List<FileInfoResponse> getRecentFiles(String organizationId, int limit);

    /**
     * 获取热门文件
     * 
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 热门文件列表
     */
    List<FileInfoResponse> getPopularFiles(String organizationId, int limit);

    /**
     * 清理过期的临时文件
     * 
     * @param expireHours 过期时间（小时）
     * @return 清理的文件数量
     */
    int cleanupExpiredFiles(int expireHours);

    /**
     * 文件访问日志
     */
    class FileAccessLog {
        private String logId;
        private String fileId;
        private String userId;
        private String accessType; // VIEW, DOWNLOAD, PREVIEW
        private String userAgent;
        private String ipAddress;
        private java.time.LocalDateTime accessTime;

        public FileAccessLog() {}

        public FileAccessLog(String fileId, String userId, String accessType, 
                           String userAgent, String ipAddress) {
            this.fileId = fileId;
            this.userId = userId;
            this.accessType = accessType;
            this.userAgent = userAgent;
            this.ipAddress = ipAddress;
            this.accessTime = java.time.LocalDateTime.now();
        }

        // Getters and Setters
        public String getLogId() { return logId; }
        public void setLogId(String logId) { this.logId = logId; }

        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getAccessType() { return accessType; }
        public void setAccessType(String accessType) { this.accessType = accessType; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public java.time.LocalDateTime getAccessTime() { return accessTime; }
        public void setAccessTime(java.time.LocalDateTime accessTime) { this.accessTime = accessTime; }
    }

    /**
     * 访问类型常量
     */
    class AccessType {
        public static final String VIEW = "VIEW";
        public static final String DOWNLOAD = "DOWNLOAD";
        public static final String PREVIEW = "PREVIEW";
        public static final String EDIT = "EDIT";
        public static final String DELETE = "DELETE";
    }
}