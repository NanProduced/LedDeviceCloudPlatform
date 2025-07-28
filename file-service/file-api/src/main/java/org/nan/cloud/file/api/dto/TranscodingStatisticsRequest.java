package org.nan.cloud.file.api.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 转码统计查询请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "转码统计查询请求")
public class TranscodingStatisticsRequest {

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
     * 统计开始日期
     */
    @Schema(description = "统计开始日期")
    private LocalDate startDate;
    
    /**
     * 统计结束日期
     */
    @Schema(description = "统计结束日期")
    private LocalDate endDate;

    /**
     * 统计维度
     */
    @Schema(description = "统计维度", example = "DAILY")
    private String dimension = "DAILY";

    /**
     * 文件类型过滤
     */
    @Schema(description = "文件类型过滤", example = "VIDEO")
    private String fileType;

    /**
     * 转码预设过滤
     */
    @Schema(description = "转码预设过滤")
    private String preset;

    /**
     * 是否包含详细信息
     */
    @Schema(description = "是否包含详细信息")
    private Boolean includeDetails = false;
}