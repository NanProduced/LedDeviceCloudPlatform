package org.nan.cloud.file.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 文件上传请求DTO - 简化版本
 * 组织ID通过InvocationContextHolder获取
 * 文件类型根据MIME类型自动检测
 * 缩略图对图片/视频文件自动生成
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件上传请求")
public class FileUploadRequest {

    @Schema(description = "目标文件夹ID", example = "folder001")
    private String folderId;

    @Schema(description = "素材名称", example = "宣传视频")
    @Size(max = 200, message = "素材名称不能超过200个字符")
    private String materialName;

    @Schema(description = "文件描述", example = "宣传视频素材")
    @Size(max = 500, message = "文件描述不能超过500个字符")
    private String description;

    /* =================== 业务填充用户认证数据 ======================== */

    @Schema(hidden = true)
    private Long oid;

    @Schema(hidden = true)
    private Long uid;

    @Schema(hidden = true)
    private Long ugid;

}