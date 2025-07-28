package org.nan.cloud.file.application.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件信息领域模型
 * 
 * 核心业务实体，包含文件的完整信息和业务规则
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    /**
     * 文件唯一标识
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * MD5哈希值（用于去重）
     */
    private String md5Hash;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * 组织ID
     */
    private String organizationId;

    /**
     * 文件夹ID
     */
    private String folderId;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 文件标签
     */
    private List<String> tags;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 存储策略
     */
    private String storageStrategy;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 访问次数
     */
    private Long accessCount;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 文件状态
     */
    private FileStatus status;

    /**
     * 缩略图路径
     */
    private String thumbnailPath;

    /**
     * 转码任务ID
     */
    private String transcodingTaskId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 文件扩展信息（JSON格式）
     */
    private String metadata;

    /**
     * 创建者ID
     */
    private String createdBy;

    /**
     * 更新者ID
     */
    private String updatedBy;

    /**
     * 文件状态枚举
     */
    public enum FileStatus {
        UPLOADING("上传中"),
        UPLOADED("已上传"),
        PROCESSING("处理中"),
        COMPLETED("处理完成"),
        FAILED("处理失败"),
        DELETED("已删除");

        private final String description;

        FileStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 业务方法：检查文件是否为图片
     */
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 业务方法：检查文件是否为视频
     */
    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * 业务方法：检查文件是否为音频
     */
    public boolean isAudio() {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    /**
     * 业务方法：生成访问URL
     */
    public String generateAccessUrl(String baseUrl) {
        if (storagePath == null) {
            return null;
        }
        return baseUrl + "/" + storagePath;
    }

    /**
     * 业务方法：增加访问次数
     */
    public void incrementAccessCount() {
        this.accessCount = this.accessCount == null ? 1L : this.accessCount + 1;
        this.lastAccessTime = LocalDateTime.now();
    }

    /**
     * 业务方法：更新文件状态
     */
    public void updateStatus(FileStatus newStatus) {
        this.status = newStatus;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 业务方法：检查文件是否可以删除
     */
    public boolean canBeDeleted() {
        return status != FileStatus.UPLOADING && status != FileStatus.PROCESSING;
    }

    /**
     * 业务方法：检查文件是否需要转码
     */
    public boolean needsTranscoding() {
        return isVideo() && (transcodingTaskId == null || transcodingTaskId.isEmpty());
    }

    /**
     * 业务方法：检查文件是否需要生成缩略图
     */
    public boolean needsThumbnail() {
        return (isImage() || isVideo()) && (thumbnailPath == null || thumbnailPath.isEmpty());
    }
}