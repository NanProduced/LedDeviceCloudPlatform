package org.nan.cloud.core.event.mq;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件上传事件
 * 
 * 用于file-service和core-service之间的异步通信
 * 支持上传进度、完成状态、错误信息的传递
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadEvent {

    /**
     * 上传任务事件类型:
     * CREATED : 任务创建
     * STARTED : 任务开始
     * PROGRESS : 任务进度
     * FAILED : 任务失败
     * COMPLETED : 任务完成
     */
    private String eventType;

    /**
     * 明确是哪一种文件上传
     * example:
     * MATERIAL -> 素材文件
     * ZIP -> 升级包
     * ...
     */
    private String uploadType;

    private String organizationId;

    /**
     * 创建任务的用户
     * 如果是系统行为，则为null
     */
    private String userId;

    /**
     * 文件上传任务ID
     */
    private String taskId;

    /**
     * 文件ID (上传完成后生成)
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * MD5哈希值
     */
    private String md5Hash;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * 访问URL
     */
    private String accessUrl;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 上传进度 (0-100)
     */
    private Integer progress;
    
    /**
     * 已上传字节数（用于精确进度计算和传输速度统计）
     */
    private Long uploadedBytes;
    
    /**
     * 总字节数（等同于fileSize，但用于进度计算更明确）
     */
    private Long totalBytes;

    /**
     * 上传状态
     * UPLOADING: 上传中
     * CANCELED: 取消
     * FAILED: 失败
     * COMPLETED: 完成
     */
    private String uploadStatus;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 转码任务ID
     */
    private String transcodingTaskId;

    /**
     * 文件基础元数据
     */
    private FileMetadata metadata;

    /**
     * 业务上下文信息
     */
    private BusinessContext businessContext;

    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 扩展属性
     */
    private Map<String, Object> extras;

    /**
     * 文件上传类型枚举
     * 用于区分不同类型的文件上传业务
     */
    public enum FileUploadType {
        // 素材文件上传
        MATERIAL,
        // 升级包上传
        ZIP;
    }

    /**
     * 文件基础元数据
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FileMetadata {
        private Integer width;
        private Integer height;
        private Integer duration;
        private Double frameRate;
        private Long bitrate;
        private String codec;
        private Integer sampleRate;
        private Integer channels;
        private Integer dpi;
        private String colorSpace;
    }

    /**
     * 业务上下文信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BusinessContext {
        
        /**
         * 组织ID
         */
        private Long oid;
        
        /**
         * 用户组ID
         */
        private Long ugid;
        
        /**
         * 目标文件夹ID
         */
        private Long fid;
        
        /**
         * 上传者用户ID
         */
        private Long uploaderId;
        
        /**
         * 素材名称
         */
        private String materialName;
        
        /**
         * 素材描述
         */
        private String description;
        
        /**
         * 素材标签
         */
        private String tags;
        
        /**
         * 是否公开
         */
        private Boolean isPublic;
        
        /**
         * 存储策略
         */
        private String storageStrategy;
        
        /**
         * 是否自动转码
         */
        private Boolean autoTranscode;
        
        /**
         * 转码预设ID
         */
        private String transcodingPresetId;
        
        /**
         * 是否生成缩略图
         */
        private Boolean generateThumbnail;
        
        /**
         * 自定义元数据JSON
         */
        private String customMetadata;
    }
}