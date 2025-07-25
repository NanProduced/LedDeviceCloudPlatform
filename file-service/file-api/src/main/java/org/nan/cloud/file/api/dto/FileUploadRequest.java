package org.nan.cloud.file.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文件上传请求DTO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件上传请求")
public class FileUploadRequest {

    @Schema(description = "组织ID", required = true, example = "org001")
    @NotBlank(message = "组织ID不能为空")
    private String organizationId;

    @Schema(description = "目标文件夹ID", example = "folder001")
    private String folderId;

    @Schema(description = "文件类型", required = true, example = "VIDEO", 
            allowableValues = {"VIDEO", "IMAGE", "AUDIO", "DOCUMENT", "OTHER"})
    @NotNull(message = "文件类型不能为空")
    private FileType fileType;

    @Schema(description = "文件描述", example = "宣传视频素材")
    @Size(max = 500, message = "文件描述不能超过500个字符")
    private String description;

    @Schema(description = "文件标签", example = "宣传,广告,2024")
    private String tags;

    @Schema(description = "是否公开", example = "false")
    private Boolean isPublic = false;

    @Schema(description = "存储策略", example = "OSS", 
            allowableValues = {"LOCAL", "OSS", "AUTO"})
    private StorageStrategy storageStrategy = StorageStrategy.AUTO;

    @Schema(description = "上传完成后是否自动转码", example = "true")
    private Boolean autoTranscode = false;

    @Schema(description = "转码预设ID", example = "preset_hd")
    private String transcodingPresetId;

    @Schema(description = "是否生成缩略图", example = "true")
    private Boolean generateThumbnail = true;

    @Schema(description = "自定义元数据JSON")
    private String customMetadata;

    /**
     * 文件类型枚举
     */
    public enum FileType {
        VIDEO,    // 视频文件
        IMAGE,    // 图片文件
        AUDIO,    // 音频文件
        DOCUMENT, // 文档文件
        OTHER     // 其他文件
    }

    /**
     * 存储策略枚举
     */
    public enum StorageStrategy {
        LOCAL,    // 本地存储
        OSS,      // 云存储
        AUTO      // 自动选择
    }
}