package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件预览请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件预览请求")
public class FilePreviewRequest {

    /**
     * 文件ID
     */
    @NotBlank(message = "文件ID不能为空")
    @Schema(description = "文件ID", required = true)
    private String fileId;

    /**
     * 预览类型
     */
    @Schema(description = "预览类型", example = "THUMBNAIL")
    private String previewType = "THUMBNAIL";

    /**
     * 预览尺寸
     */
    @Schema(description = "预览尺寸", example = "200x200")
    private String size;

    /**
     * 预览质量
     */
    @Schema(description = "预览质量", example = "MEDIUM")
    private String quality = "MEDIUM";
}