package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文件统计响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "文件统计响应")
public class FileStatisticsResponse {

    /**
     * 总体统计
     */
    @Schema(description = "总体统计信息")
    private OverallStatistics overall;

    /**
     * 时间维度统计
     */
    @Schema(description = "时间维度统计数据")
    private List<TimeSeriesStatistics> timeSeries;

    /**
     * 文件类型统计
     */
    @Schema(description = "文件类型统计")
    private Map<String, TypeStatistics> typeStats;

    /**
     * 存储策略统计
     */
    @Schema(description = "存储策略统计")
    private Map<String, StorageStatistics> storageStats;

    /**
     * 用户统计 (仅管理员可见)
     */
    @Schema(description = "用户统计")
    private List<UserStatistics> userStats;

    /**
     * 总体统计信息
     */
    @Data
    @Builder
    @Schema(description = "总体统计信息")
    public static class OverallStatistics {
        
        /**
         * 总文件数
         */
        @Schema(description = "总文件数")
        private Long totalFiles;

        /**
         * 总文件大小 (字节)
         */
        @Schema(description = "总文件大小")
        private Long totalSize;

        /**
         * 活跃文件数
         */
        @Schema(description = "活跃文件数")
        private Long activeFiles;

        /**
         * 已删除文件数
         */
        @Schema(description = "已删除文件数")
        private Long deletedFiles;

        /**
         * 今日新增文件数
         */
        @Schema(description = "今日新增文件数")
        private Long todayNewFiles;

        /**
         * 本周新增文件数
         */
        @Schema(description = "本周新增文件数")
        private Long weekNewFiles;

        /**
         * 本月新增文件数
         */
        @Schema(description = "本月新增文件数")
        private Long monthNewFiles;

        /**
         * 平均文件大小
         */
        @Schema(description = "平均文件大小")
        private Long avgFileSize;

        /**
         * 存储使用率
         */
        @Schema(description = "存储使用率")
        private Double storageUsageRate;

        /**
         * 热门文件类型 (前5)
         */
        @Schema(description = "热门文件类型")
        private List<String> topFileTypes;
    }

    /**
     * 时间序列统计
     */
    @Data
    @Builder
    @Schema(description = "时间序列统计")
    public static class TimeSeriesStatistics {
        
        /**
         * 时间点
         */
        @Schema(description = "时间点")
        private LocalDateTime time;

        /**
         * 文件数量
         */
        @Schema(description = "文件数量")
        private Long fileCount;

        /**
         * 文件大小
         */
        @Schema(description = "文件大小")
        private Long totalSize;

        /**
         * 新增文件数
         */
        @Schema(description = "新增文件数")
        private Long newFiles;

        /**
         * 删除文件数
         */
        @Schema(description = "删除文件数")
        private Long deletedFiles;

        /**
         * 上传文件数
         */
        @Schema(description = "上传文件数")
        private Long uploadedFiles;

        /**
         * 下载次数
         */
        @Schema(description = "下载次数")
        private Long downloadCount;
    }

    /**
     * 类型统计
     */
    @Data
    @Builder
    @Schema(description = "类型统计")
    public static class TypeStatistics {
        
        /**
         * 文件数量
         */
        @Schema(description = "文件数量")
        private Long count;

        /**
         * 总大小
         */
        @Schema(description = "总大小")
        private Long totalSize;

        /**
         * 平均大小
         */
        @Schema(description = "平均大小")
        private Long avgSize;

        /**
         * 占比
         */
        @Schema(description = "占比")
        private Double percentage;

        /**
         * 增长趋势
         */
        @Schema(description = "增长趋势")
        private String trend;
    }

    /**
     * 存储统计
     */
    @Data
    @Builder
    @Schema(description = "存储统计")
    public static class StorageStatistics {
        
        /**
         * 文件数量
         */
        @Schema(description = "文件数量")
        private Long fileCount;

        /**
         * 存储大小
         */
        @Schema(description = "存储大小")
        private Long storageSize;

        /**
         * 成本估算
         */
        @Schema(description = "成本估算")
        private Double estimatedCost;

        /**
         * 可用空间
         */
        @Schema(description = "可用空间")
        private Long availableSpace;

        /**
         * 使用率
         */
        @Schema(description = "使用率")
        private Double usageRate;
    }

    /**
     * 用户统计
     */
    @Data
    @Builder
    @Schema(description = "用户统计")
    public static class UserStatistics {
        
        /**
         * 用户ID
         */
        @Schema(description = "用户ID")
        private String userId;

        /**
         * 用户名
         */
        @Schema(description = "用户名")
        private String userName;

        /**
         * 文件数量
         */
        @Schema(description = "文件数量")
        private Long fileCount;

        /**
         * 存储大小
         */
        @Schema(description = "存储大小")
        private Long storageSize;

        /**
         * 上传次数
         */
        @Schema(description = "上传次数")
        private Long uploadCount;

        /**
         * 最后上传时间
         */
        @Schema(description = "最后上传时间")
        private LocalDateTime lastUploadTime;
    }
}