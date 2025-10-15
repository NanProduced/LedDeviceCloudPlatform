package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 文件预览URL响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "文件预览URL响应")
public class FilePreviewUrlResponse {

    /**
     * 文件ID
     */
    @Schema(description = "文件ID")
    private String fileId;

    /**
     * 预览URL
     */
    @Schema(description = "预览URL")
    private String previewUrl;

    /**
     * 预览类型
     */
    @Schema(description = "预览类型")
    private String previewType;

    /**
     * URL过期时间
     */
    @Schema(description = "URL过期时间")
    private LocalDateTime expirationTime;

    /**
     * 是否支持预览
     */
    @Schema(description = "是否支持预览")
    private Boolean supported;
}