package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分块上传初始化请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "分块上传初始化请求")
public class ChunkUploadInitRequest {

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    @Schema(description = "文件名", required = true, example = "video.mp4")
    private String filename;

    /**
     * 文件大小 (字节)
     */
    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    @Schema(description = "文件大小", required = true, example = "1073741824")
    private Long fileSize;

    /**
     * 文件MD5
     */
    @Schema(description = "文件MD5哈希值", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String fileMd5;

    /**
     * 文件类型
     */
    @Schema(description = "文件MIME类型", example = "video/mp4")
    private String mimeType;

    /**
     * 文件夹ID
     */
    @Schema(description = "目标文件夹ID")
    private String folderId;

    /**
     * 文件类型枚举
     */
    @Schema(description = "文件类型", example = "VIDEO")
    private FileUploadRequest.FileType fileType;

    /**
     * 分块大小 (字节)
     */
    @Positive(message = "分块大小必须大于0")
    @Schema(description = "分块大小", example = "5242880")
    private Long chunkSize = 5L * 1024 * 1024; // 默认5MB

    /**
     * 组织ID
     */
    @NotBlank(message = "组织ID不能为空")
    @Schema(description = "组织ID", required = true)
    private String organizationId;

    /**
     * 上传用户ID
     */
    @Schema(description = "上传用户ID")
    private String uploadUserId;

    /**
     * 存储策略
     */
    @Schema(description = "存储策略", example = "OSS")
    private String storageStrategy = "AUTO";

    /**
     * 文件标签
     */
    @Schema(description = "文件标签")
    private String tags;

    /**
     * 文件描述
     */
    @Schema(description = "文件描述")
    private String description;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型", example = "LED_CONTENT")
    private String businessType;

    /**
     * 是否公开
     */
    @Schema(description = "是否公开文件")
    private Boolean isPublic = false;
}