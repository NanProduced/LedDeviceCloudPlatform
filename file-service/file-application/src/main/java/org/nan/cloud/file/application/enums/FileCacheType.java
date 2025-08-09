package org.nan.cloud.file.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * File Service缓存类型枚举
 * 参照core-service标准，定义不同业务数据的缓存策略
 * 
 * 缓存键命名规范：file:{category}:{subcategory}
 * 
 * @author LedDeviceCloudPlatform Team  
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum FileCacheType {
    
    // ==================== 文件元数据缓存 ====================
    
    /**
     * 文件信息缓存 - 文件基本信息（文件名、大小、类型等）
     * 键模式：file:info:{fileId}
     */
    FILE_INFO("file:info", Duration.ofMinutes(30), true, true),
    
    /**
     * 文件元数据缓存 - 详细元数据（EXIF、视频信息等）
     * 键模式：file:metadata:{fileId}
     */
    FILE_METADATA("file:metadata", Duration.ofHours(2), true, true),
    
    /**
     * 文件存储路径缓存 - 物理存储路径映射
     * 键模式：file:storage:path:{fileId}
     */
    FILE_STORAGE_PATH("file:storage:path", Duration.ofMinutes(60), true, true),
    
    // ==================== 缩略图缓存 ====================
    
    /**
     * 缩略图信息缓存 - 缩略图基本信息（路径、尺寸等）
     * 键模式：file:thumbnail:info:{fileId}
     */
    THUMBNAIL_INFO("file:thumbnail:info", Duration.ofMinutes(60), true, true),
    
    /**
     * 缩略图数据缓存 - 缩略图二进制数据（小文件适用）
     * 键模式：file:thumbnail:data:{fileId}:{size}
     */
    THUMBNAIL_DATA("file:thumbnail:data", Duration.ofMinutes(30), false, true),
    
    /**
     * 缩略图生成状态缓存 - 防止重复生成
     * 键模式：file:thumbnail:status:{fileId}
     */
    THUMBNAIL_STATUS("file:thumbnail:status", Duration.ofMinutes(10), true, true),
    
    // ==================== 上传进度缓存 ====================
    
    /**
     * 文件上传进度缓存 - 分片上传进度跟踪
     * 键模式：file:upload:progress:{uploadId}
     */
    UPLOAD_PROGRESS("file:upload:progress", Duration.ofHours(24), false, true),
    
    /**
     * 上传任务信息缓存 - 上传任务上下文信息
     * 键模式：file:upload:task:{uploadId}
     */
    UPLOAD_TASK("file:upload:task", Duration.ofHours(24), true, true),
    
    /**
     * 分片信息缓存 - 分片上传的片段信息
     * 键模式：file:upload:chunk:{uploadId}:{chunkIndex}
     */
    UPLOAD_CHUNK("file:upload:chunk", Duration.ofHours(12), false, true),
    
    // ==================== 转码任务缓存 ====================
    
    /**
     * 转码任务进度缓存 - FFmpeg转码进度跟踪
     * 键模式：file:transcode:progress:{taskId}
     */
    TRANSCODE_PROGRESS("file:transcode:progress", Duration.ofHours(6), false, true),
    
    /**
     * 转码任务状态缓存 - 任务执行状态
     * 键模式：file:transcode:status:{taskId}
     */
    TRANSCODE_STATUS("file:transcode:status", Duration.ofHours(12), true, true),
    
    /**
     * 转码结果缓存 - 转码完成后的结果信息
     * 键模式：file:transcode:result:{taskId}
     */
    TRANSCODE_RESULT("file:transcode:result", Duration.ofHours(24), true, true),
    
    // ==================== 文件预览缓存 ====================
    
    /**
     * 文件预览URL缓存 - 预览链接（临时URL）
     * 键模式：file:preview:url:{fileId}
     */
    PREVIEW_URL("file:preview:url", Duration.ofMinutes(15), true, true),
    
    /**
     * 预览数据缓存 - 小文件预览数据
     * 键模式：file:preview:data:{fileId}
     */
    PREVIEW_DATA("file:preview:data", Duration.ofMinutes(30), false, true),
    
    // ==================== 统计与配额缓存 ====================
    
    /**
     * 用户存储配额缓存 - 用户存储使用情况
     * 键模式：file:quota:user:{userId}
     */
    USER_QUOTA("file:quota:user", Duration.ofMinutes(15), true, true),
    
    /**
     * 组织存储配额缓存 - 组织存储使用情况
     * 键模式：file:quota:org:{orgId}
     */
    ORG_QUOTA("file:quota:org", Duration.ofMinutes(15), true, true),
    
    /**
     * 文件统计缓存 - 文件数量、大小统计
     * 键模式：file:stats:{dimension}:{period}
     */
    FILE_STATISTICS("file:stats", Duration.ofMinutes(30), true, true),
    
    // ==================== 系统配置缓存 ====================
    
    /**
     * 存储策略配置缓存 - 存储策略配置
     * 键模式：file:config:storage
     */
    STORAGE_CONFIG("file:config:storage", Duration.ofHours(12), true, false),
    
    /**
     * 转码预设配置缓存 - FFmpeg转码预设
     * 键模式：file:config:transcode:preset
     */
    TRANSCODE_CONFIG("file:config:transcode", Duration.ofHours(12), true, false),
    
    /**
     * 文件类型配置缓存 - 支持的文件类型配置
     * 键模式：file:config:filetype
     */
    FILETYPE_CONFIG("file:config:filetype", Duration.ofHours(12), true, false),
    
    // ==================== 临时数据缓存 ====================
    
    /**
     * 临时令牌缓存 - 文件访问临时令牌
     * 键模式：file:token:access:{token}
     */
    ACCESS_TOKEN("file:token:access", Duration.ofMinutes(30), false, true),
    
    /**
     * 验证码缓存 - 文件操作验证码
     * 键模式：file:token:verify:{code}
     */
    VERIFY_CODE("file:token:verify", Duration.ofMinutes(5), false, true);

    private final String keyPrefix;
    private final Duration defaultTtl;
    private final boolean useLocalCache;        // 是否使用本地缓存
    private final boolean useDistributedCache;  // 是否使用分布式缓存
    
    /**
     * 构建完整的缓存键（不包含组织隔离）
     * @param keyParts 键部分
     * @return 完整缓存键
     */
    public String buildKey(String... keyParts) {
        if (keyParts == null || keyParts.length == 0) {
            return keyPrefix;
        }
        return keyPrefix + ":" + String.join(":", keyParts);
    }
    
    /**
     * 构建组织隔离的缓存键
     * @param orgId 组织ID
     * @param keyParts 其他键部分
     * @return 带组织隔离的缓存键
     */
    public String buildOrgKey(Long orgId, String... keyParts) {
        String baseKey = "org:" + orgId + ":" + keyPrefix;
        if (keyParts == null || keyParts.length == 0) {
            return baseKey;
        }
        return baseKey + ":" + String.join(":", keyParts);
    }

    /**
     * 构建任务相关的缓存键
     * @param taskId 任务ID
     * @return 任务缓存键
     */
    public String buildTaskKey(String taskId) {
        return keyPrefix + ":" + taskId;
    }
    
    /**
     * 构建用户相关的缓存键
     * @param userId 用户ID
     * @param keyParts 其他键部分
     * @return 用户缓存键
     */
    public String buildUserKey(String userId, String... keyParts) {
        String baseKey = keyPrefix + ":" + userId;
        if (keyParts == null || keyParts.length == 0) {
            return baseKey;
        }
        return baseKey + ":" + String.join(":", keyParts);
    }
    
    /**
     * 构建组织级别的缓存键模式（用于批量清理）
     * @param orgId 组织ID
     * @return 组织缓存键模式
     */
    public String buildOrgPattern(Long orgId) {
        return "org:" + orgId + ":" + keyPrefix + ":*";
    }
    
    /**
     * 构建用户级别的缓存键模式（用于批量清理）
     * @param userId 用户ID
     * @return 用户缓存键模式
     */
    public String buildUserPattern(String userId) {
        return keyPrefix + ":" + userId + ":*";
    }
}