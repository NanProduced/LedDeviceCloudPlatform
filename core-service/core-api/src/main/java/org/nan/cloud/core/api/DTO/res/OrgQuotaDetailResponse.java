package org.nan.cloud.core.api.DTO.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 组织配额详情响应DTO
 * 用于配额可视化的核心数据展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgQuotaDetailResponse {

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 存储配额信息
     */
    private StorageQuotaInfo storageInfo;

    /**
     * 文件配额信息
     */
    private FileQuotaInfo fileInfo;

    /**
     * 告警配置信息
     */
    private WarningInfo warningInfo;

    /**
     * 预测信息
     */
    private PredictionInfo predictionInfo;

    /**
     * 数据更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 存储配额信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageQuotaInfo {
        /**
         * 最大存储空间（字节）
         */
        private Long maxSize;
        
        /**
         * 已使用存储空间（字节）
         */
        private Long usedSize;
        
        /**
         * 剩余存储空间（字节）
         */
        private Long remainingSize;
        
        /**
         * 使用率百分比 (0-100)
         */
        private Double usagePercentage;
        
        /**
         * 存储状态: NORMAL, WARNING, CRITICAL
         */
        private String status;
    }

    /**
     * 文件配额信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileQuotaInfo {
        /**
         * 最大文件数量
         */
        private Integer maxCount;
        
        /**
         * 已使用文件数量
         */
        private Integer usedCount;
        
        /**
         * 剩余文件数量
         */
        private Integer remainingCount;
        
        /**
         * 使用率百分比 (0-100)
         */
        private Double usagePercentage;
        
        /**
         * 文件状态: NORMAL, WARNING, CRITICAL
         */
        private String status;
    }

    /**
     * 告警配置信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarningInfo {
        /**
         * 告警阈值百分比
         */
        private Integer thresholdPercent;
        
        /**
         * 是否已触发告警
         */
        private Boolean isWarning;
        
        /**
         * 告警消息
         */
        private String warningMessage;
    }

    /**
     * 预测信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionInfo {
        /**
         * 预计剩余天数（基于最近趋势）
         */
        private Integer estimatedDaysRemaining;
        
        /**
         * 增长趋势: GROWING, STABLE, DECLINING
         */
        private String growthTrend;
        
        /**
         * 日均增长量（字节）
         */
        private Long dailyGrowthBytes;
    }
}