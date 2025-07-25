package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 上传进度响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "上传进度响应")
public class UploadProgressResponse {

    /**
     * 上传ID
     */
    @Schema(description = "上传ID")
    private String uploadId;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 上传状态
     */
    @Schema(description = "上传状态", example = "UPLOADING")
    private String status;

    /**
     * 上传进度百分比 (0-100)
     */
    @Schema(description = "上传进度百分比")
    private Double progress;

    /**
     * 已上传大小 (字节)
     */
    @Schema(description = "已上传大小")
    private Long uploadedSize;

    /**
     * 总文件大小 (字节)
     */
    @Schema(description = "总文件大小")
    private Long totalSize;

    /**
     * 上传速度 (字节/秒)
     */
    @Schema(description = "上传速度")
    private Long uploadSpeed;

    /**
     * 预计剩余时间 (秒)
     */
    @Schema(description = "预计剩余时间")
    private Long estimatedTimeRemaining;

    /**
     * 已上传分块数
     */
    @Schema(description = "已上传分块数")
    private Integer uploadedChunks;

    /**
     * 总分块数
     */
    @Schema(description = "总分块数")
    private Integer totalChunks;

    /**
     * 失败分块列表
     */
    @Schema(description = "失败分块列表")
    private List<Integer> failedChunks;

    /**
     * 开始时间
     */
    @Schema(description = "上传开始时间")
    private LocalDateTime startTime;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /**
     * 上传质量指标
     */
    @Schema(description = "上传质量指标")
    private QualityMetrics qualityMetrics;

    /**
     * 上传质量指标
     */
    @Data
    @Builder
    @Schema(description = "上传质量指标")
    public static class QualityMetrics {
        
        /**
         * 平均上传速度
         */
        @Schema(description = "平均上传速度")
        private Long avgSpeed;

        /**
         * 最大上传速度
         */
        @Schema(description = "最大上传速度")
        private Long maxSpeed;

        /**
         * 最小上传速度
         */
        @Schema(description = "最小上传速度")
        private Long minSpeed;

        /**
         * 连接稳定性 (0-1)
         */
        @Schema(description = "连接稳定性")
        private Double stability;

        /**
         * 重传率
         */
        @Schema(description = "重传率")
        private Double retransmissionRate;
    }
}