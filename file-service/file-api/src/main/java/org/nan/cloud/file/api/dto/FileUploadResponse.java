package org.nan.cloud.file.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件上传响应DTO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传响应")
public class FileUploadResponse {

    @Schema(description = "文件ID", example = "file_123456789")
    private String fileId;

    @Schema(description = "原始文件名", example = "video.mp4")
    private String originalFilename;

    @Schema(description = "文件大小（字节）", example = "1048576")
    private Long fileSize;

    @Schema(description = "文件类型", example = "VIDEO")
    private String fileType;

    @Schema(description = "MIME类型", example = "video/mp4")
    private String mimeType;

    @Schema(description = "文件MD5哈希值", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String md5Hash;

    @Schema(description = "存储路径", example = "/files/2024/01/video.mp4")
    private String storagePath;

    @Schema(description = "访问URL", example = "https://cdn.example.com/files/video.mp4")
    private String accessUrl;

    @Schema(description = "缩略图URL", example = "https://cdn.example.com/thumbs/video_thumb.jpg")
    private String thumbnailUrl;

    @Schema(description = "上传状态", example = "SUCCESS")
    private UploadStatus status;

    @Schema(description = "上传任务ID", example = "task_123456789")
    private String taskId;

    @Schema(description = "转码任务ID", example = "transcode_123456789")
    private String transcodingTaskId;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    @Schema(description = "文件元数据")
    private FileMetadata metadata;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "是否为秒传（文件已存在）", example = "false")
    @Builder.Default
    private Boolean instantUpload = false;

    /**
     * 上传状态枚举
     */
    public enum UploadStatus {
        PENDING,      // 等待中
        UPLOADING,    // 上传中
        SUCCESS,      // 上传成功
        FAILED,       // 上传失败
        CANCELLED     // 已取消
    }

    /**
     * 文件元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "文件元数据")
    public static class FileMetadata {

        @Schema(description = "视频时长（秒）", example = "120")
        private Integer duration;

        @Schema(description = "视频宽度", example = "1920")
        private Integer width;

        @Schema(description = "视频高度", example = "1080")
        private Integer height;

        @Schema(description = "帧率", example = "30")
        private Double frameRate;

        @Schema(description = "比特率", example = "5000")
        private Long bitrate;

        @Schema(description = "编码格式", example = "H.264")
        private String codec;

        @Schema(description = "音频采样率", example = "44100")
        private Integer sampleRate;

        @Schema(description = "音频通道数", example = "2")
        private Integer channels;

        @Schema(description = "图片DPI", example = "300")
        private Integer dpi;

        @Schema(description = "颜色空间", example = "RGB")
        private String colorSpace;
    }
}