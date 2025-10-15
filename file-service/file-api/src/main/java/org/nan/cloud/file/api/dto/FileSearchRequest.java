package org.nan.cloud.file.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件搜索请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件搜索请求")
public class FileSearchRequest {

    /**
     * 搜索关键词
     */
    @Schema(description = "搜索关键词")
    private String keyword;

    /**
     * 组织ID
     */
    @Schema(description = "组织ID")
    private String organizationId;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 页码
     */
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;
}