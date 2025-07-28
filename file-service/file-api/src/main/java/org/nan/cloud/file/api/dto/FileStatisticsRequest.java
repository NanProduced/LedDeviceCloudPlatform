package org.nan.cloud.file.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件统计请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "文件统计请求")
public class FileStatisticsRequest {

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
     * 统计维度
     */
    @Schema(description = "统计维度", example = "DAILY")
    private String dimension = "DAILY";
    
    /**
     * 统计类型
     */
    @Schema(description = "统计类型")
    private String statisticsType;

    /**
     * 统计开始时间
     */
    @Schema(description = "统计开始时间")
    private LocalDateTime startTime;

    /**
     * 统计结束时间
     */
    @Schema(description = "统计结束时间")
    private LocalDateTime endTime;

    /**
     * 文件类型过滤
     */
    @Schema(description = "文件类型过滤")
    private List<String> fileTypes;

    /**
     * 业务类型过滤
     */
    @Schema(description = "业务类型过滤")
    private List<String> businessTypes;

    /**
     * 存储策略过滤
     */
    @Schema(description = "存储策略过滤")
    private List<String> storageStrategies;

    /**
     * 是否包含详细信息
     */
    @Schema(description = "是否包含详细信息")
    private Boolean includeDetails = false;

    /**
     * 统计指标
     */
    @Schema(description = "统计指标")
    private List<String> metrics;
}