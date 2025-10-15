package org.nan.cloud.file.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件查询请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件查询请求")
public class FileQueryRequest {

    /**
     * 组织ID
     */
    @Schema(description = "组织ID")
    private String organizationId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private String userId;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型", example = "VIDEO")
    private String fileType;

    /**
     * 文件状态
     */
    @Schema(description = "文件状态", example = "ACTIVE")
    private String status;

    /**
     * 存储策略
     */
    @Schema(description = "存储策略", example = "OSS")
    private String storageStrategy;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型", example = "LED_CONTENT")
    private String businessType;

    /**
     * 文件标签
     */
    @Schema(description = "文件标签")
    private List<String> tags;

    /**
     * 关键词搜索
     */
    @Schema(description = "关键词搜索(文件名、描述)")
    private String keyword;

    /**
     * 文件大小范围 - 最小值 (字节)
     */
    @Schema(description = "文件大小最小值")
    private Long minSize;

    /**
     * 文件大小范围 - 最大值 (字节)
     */
    @Schema(description = "文件大小最大值")
    private Long maxSize;

    /**
     * 创建时间范围 - 开始时间
     */
    @Schema(description = "创建时间开始")
    private LocalDateTime createTimeStart;

    /**
     * 创建时间范围 - 结束时间
     */
    @Schema(description = "创建时间结束")
    private LocalDateTime createTimeEnd;

    /**
     * 最后修改时间范围 - 开始时间
     */
    @Schema(description = "修改时间开始")
    private LocalDateTime updateTimeStart;

    /**
     * 最后修改时间范围 - 结束时间
     */
    @Schema(description = "修改时间结束")
    private LocalDateTime updateTimeEnd;

    /**
     * 是否包含已删除文件
     */
    @Schema(description = "是否包含已删除文件")
    private Boolean includeDeleted = false;

    /**
     * 是否仅查询公开文件
     */
    @Schema(description = "是否仅查询公开文件")
    private Boolean publicOnly = false;

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