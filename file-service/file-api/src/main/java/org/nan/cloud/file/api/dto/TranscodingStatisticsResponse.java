package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 转码统计响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "转码统计响应")
public class TranscodingStatisticsResponse {

    /**
     * 总体统计
     */
    @Schema(description = "总体统计信息")
    private OverallStatistics overall;

    /**
     * 时间维度统计
     */
    @Schema(description = "时间维度统计数据")
    private List<TimeSeriesData> timeSeries;

    /**
     * 预设统计
     */
    @Schema(description = "预设使用统计")
    private Map<String, PresetStatistics> presetStats;

    /**
     * 文件类型统计
     */
    @Schema(description = "文件类型统计")
    private Map<String, FileTypeStatistics> fileTypeStats;

    /**
     * 总体统计信息
     */
    @Data
    @Builder
    @Schema(description = "总体统计信息")
    public static class OverallStatistics {
        
        /**
         * 总任务数
         */
        @Schema(description = "总任务数")
        private Long totalTasks;

        /**
         * 成功任务数
         */
        @Schema(description = "成功任务数")
        private Long successTasks;

        /**
         * 失败任务数
         */
        @Schema(description = "失败任务数")
        private Long failedTasks;

        /**
         * 平均处理时间 (分钟)
         */
        @Schema(description = "平均处理时间")
        private Double avgProcessTime;

        /**
         * 总处理时间 (小时)
         */
        @Schema(description = "总处理时间")
        private Double totalProcessTime;

        /**
         * 处理的总文件大小 (GB)
         */
        @Schema(description = "处理的总文件大小")
        private Double totalFileSize;

        /**
         * 成功率
         */
        @Schema(description = "成功率")
        private Double successRate;

        /**
         * 平均等待时间 (分钟)
         */
        @Schema(description = "平均等待时间")
        private Double avgWaitTime;
    }

    /**
     * 时间序列数据
     */
    @Data
    @Builder
    @Schema(description = "时间序列数据")
    public static class TimeSeriesData {
        
        /**
         * 时间点
         */
        @Schema(description = "时间点")
        private LocalDateTime time;

        /**
         * 任务数量
         */
        @Schema(description = "任务数量")
        private Long taskCount;

        /**
         * 成功数量
         */
        @Schema(description = "成功数量")
        private Long successCount;

        /**
         * 失败数量
         */
        @Schema(description = "失败数量")
        private Long failedCount;

        /**
         * 平均处理时间
         */
        @Schema(description = "平均处理时间")
        private Double avgProcessTime;

        /**
         * 处理的文件大小
         */
        @Schema(description = "处理的文件大小")
        private Double processedSize;
    }

    /**
     * 预设统计
     */
    @Data
    @Builder
    @Schema(description = "预设统计")
    public static class PresetStatistics {
        
        /**
         * 使用次数
         */
        @Schema(description = "使用次数")
        private Long usageCount;

        /**
         * 成功次数
         */
        @Schema(description = "成功次数")
        private Long successCount;

        /**
         * 平均处理时间
         */
        @Schema(description = "平均处理时间")
        private Double avgProcessTime;

        /**
         * 平均文件大小压缩比
         */
        @Schema(description = "平均文件大小压缩比")
        private Double avgCompressionRatio;
    }

    /**
     * 文件类型统计
     */
    @Data
    @Builder
    @Schema(description = "文件类型统计")
    public static class FileTypeStatistics {
        
        /**
         * 处理数量
         */
        @Schema(description = "处理数量")
        private Long count;

        /**
         * 总文件大小
         */
        @Schema(description = "总文件大小")
        private Double totalSize;

        /**
         * 平均文件大小
         */
        @Schema(description = "平均文件大小")
        private Double avgSize;

        /**
         * 成功率
         */
        @Schema(description = "成功率")
        private Double successRate;
    }
}