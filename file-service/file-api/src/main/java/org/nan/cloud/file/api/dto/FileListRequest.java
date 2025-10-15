package org.nan.cloud.file.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件列表请求 - 用于简单的文件列表查询
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件列表请求")
public class FileListRequest {

    /**
     * 组织ID
     */
    @Schema(description = "组织ID")
    private String organizationId;

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

    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "createTime")
    private String sortField = "createTime";

    /**
     * 排序方向
     */
    @Schema(description = "排序方向", example = "DESC")
    private String sortDirection = "DESC";
}