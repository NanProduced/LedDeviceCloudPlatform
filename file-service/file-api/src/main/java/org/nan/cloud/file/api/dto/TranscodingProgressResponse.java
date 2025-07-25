package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 转码进度响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "转码进度响应")
public class TranscodingProgressResponse {

    /**
     * 任务ID
     */
    @Schema(description = "转码任务ID")
    private String taskId;

    /**
     * 任务状态
     */
    @Schema(description = "任务状态", example = "PROCESSING")
    private String status;

    /**
     * 进度百分比 (0-100)
     */
    @Schema(description = "进度百分比", example = "75")
    private Integer progress;

    /**
     * 处理速度 (fps)
     */
    @Schema(description = "处理速度", example = "30.5")
    private Double fps;

    /**
     * 已处理时长 (秒)
     */
    @Schema(description = "已处理时长", example = "120")
    private Long processedDuration;

    /**
     * 总时长 (秒)
     */
    @Schema(description = "视频总时长", example = "300")
    private Long totalDuration;

    /**
     * 预计剩余时间 (秒)
     */
    @Schema(description = "预计剩余时间", example = "60")
    private Long estimatedTimeRemaining;

    /**
     * 当前处理的帧数
     */
    @Schema(description = "当前处理的帧数")
    private Long frameCount;

    /**
     * 当前处理时间点
     */
    @Schema(description = "当前处理时间点", example = "00:02:30")
    private String currentTime;

    /**
     * 输出文件大小 (字节)
     */
    @Schema(description = "输出文件大小")
    private Long outputFileSize;

    /**
     * 码率 (kbps)
     */
    @Schema(description = "当前码率")
    private Integer bitrate;

    /**
     * 质量评分
     */
    @Schema(description = "转码质量评分")
    private Double qualityScore;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 开始时间
     */
    @Schema(description = "转码开始时间")
    private LocalDateTime startTime;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;
}