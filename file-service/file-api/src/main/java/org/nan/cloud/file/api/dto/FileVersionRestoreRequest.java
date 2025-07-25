package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件版本恢复请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件版本恢复请求")
public class FileVersionRestoreRequest {

    /**
     * 文件ID
     */
    @NotBlank(message = "文件ID不能为空")
    @Schema(description = "文件ID", required = true)
    private String fileId;

    /**
     * 目标版本ID
     */
    @NotBlank(message = "版本ID不能为空")
    @Schema(description = "要恢复的版本ID", required = true)
    private String versionId;

    /**
     * 恢复方式
     */
    @Schema(description = "恢复方式", example = "REPLACE")
    private String restoreMode = "REPLACE";

    /**
     * 恢复原因
     */
    @Schema(description = "恢复原因")
    private String reason;

    /**
     * 是否创建备份版本
     */
    @Schema(description = "是否在恢复前创建当前版本的备份")
    private Boolean createBackup = true;

    /**
     * 备份版本描述
     */
    @Schema(description = "备份版本描述")
    private String backupDescription;
}