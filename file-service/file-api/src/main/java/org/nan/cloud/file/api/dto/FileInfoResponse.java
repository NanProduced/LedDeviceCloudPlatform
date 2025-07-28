package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文件信息响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "文件信息响应")
public class FileInfoResponse {

    /**
     * 文件ID
     */
    @Schema(description = "文件ID")
    private String fileId;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String originalFilename;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型", example = "VIDEO")
    private String fileType;

    /**
     * MIME类型
     */
    @Schema(description = "MIME类型", example = "video/mp4")
    private String contentType;

    /**
     * 文件大小 (字节)
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 文件MD5
     */
    @Schema(description = "文件MD5哈希值")
    private String fileMd5;

    /**
     * 存储路径
     */
    @Schema(description = "存储路径")
    private String storagePath;

    /**
     * 存储策略
     */
    @Schema(description = "存储策略", example = "OSS")
    private String storageStrategy;

    /**
     * 文件状态
     */
    @Schema(description = "文件状态", example = "ACTIVE")
    private String status;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型", example = "LED_CONTENT")
    private String businessType;

    /**
     * 组织ID
     */
    @Schema(description = "组织ID")
    private String organizationId;

    /**
     * 上传用户ID
     */
    @Schema(description = "上传用户ID")
    private String uploadUserId;

    /**
     * 上传用户名
     */
    @Schema(description = "上传用户名")
    private String uploadUserName;

    /**
     * 文件标签
     */
    @Schema(description = "文件标签")
    private List<String> tags;

    /**
     * 文件描述
     */
    @Schema(description = "文件描述")
    private String description;

    /**
     * 是否公开
     */
    @Schema(description = "是否公开文件")
    private Boolean isPublic;

    /**
     * 下载地址
     */
    @Schema(description = "下载地址")
    private String downloadUrl;

    /**
     * 预览地址
     */
    @Schema(description = "预览地址")
    private String previewUrl;

    /**
     * 缩略图地址
     */
    @Schema(description = "缩略图地址")
    private String thumbnailUrl;

    /**
     * 文件版本
     */
    @Schema(description = "文件版本")
    private String version;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 最后访问时间
     */
    @Schema(description = "最后访问时间")
    private LocalDateTime lastAccessTime;

    /**
     * 下载次数
     */
    @Schema(description = "下载次数")
    private Long downloadCount;

    /**
     * 扩展属性
     */
    @Schema(description = "扩展属性")
    private Map<String, Object> extendedAttributes;

    /**
     * 媒体信息 (视频/音频文件)
     */
    @Schema(description = "媒体信息")
    private MediaInfo mediaInfo;

    /**
     * 转码信息
     */
    @Schema(description = "转码信息")
    private TranscodingInfo transcodingInfo;

    /**
     * 媒体信息
     */
    @Data
    @Builder
    @Schema(description = "媒体信息")
    public static class MediaInfo {
        
        /**
         * 视频时长 (秒)
         */
        @Schema(description = "视频时长")
        private Long duration;

        /**
         * 视频分辨率
         */
        @Schema(description = "视频分辨率", example = "1920x1080")
        private String resolution;

        /**
         * 视频码率 (kbps)
         */
        @Schema(description = "视频码率")
        private Integer bitrate;

        /**
         * 帧率
         */
        @Schema(description = "帧率")
        private Double frameRate;

        /**
         * 视频编码
         */
        @Schema(description = "视频编码", example = "H.264")
        private String videoCodec;

        /**
         * 音频编码
         */
        @Schema(description = "音频编码", example = "AAC")
        private String audioCodec;
    }

    /**
     * 转码信息
     */
    @Data
    @Builder
    @Schema(description = "转码信息")
    public static class TranscodingInfo {
        
        /**
         * 是否已转码
         */
        @Schema(description = "是否已转码")
        private Boolean isTranscoded;

        /**
         * 转码状态
         */
        @Schema(description = "转码状态")
        private String transcodingStatus;

        /**
         * 转码任务ID
         */
        @Schema(description = "转码任务ID")
        private String transcodingTaskId;

        /**
         * 转码预设
         */
        @Schema(description = "转码预设")
        private String preset;

        /**
         * 转码完成时间
         */
        @Schema(description = "转码完成时间")
        private LocalDateTime transcodingCompleteTime;

        /**
         * 转码文件列表
         */
        @Schema(description = "转码输出文件列表")
        private List<String> transcodedFiles;
    }
}