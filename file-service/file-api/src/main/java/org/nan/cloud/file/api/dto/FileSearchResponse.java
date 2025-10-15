package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 文件搜索响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "文件搜索响应")
public class FileSearchResponse {

    /**
     * 搜索结果
     */
    @Schema(description = "搜索结果")
    private List<FileInfoResponse> files;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数")
    private Long total;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码")
    private Integer page;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小")
    private Integer size;
}