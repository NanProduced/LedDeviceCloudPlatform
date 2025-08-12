package org.nan.cloud.core.api.DTO.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配额趋势响应DTO
 * 用于显示配额使用的时间序列趋势
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaTrendResponse {

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 统计周期: DAILY, WEEKLY, MONTHLY
     */
    private String period;

    /**
     * 存储使用趋势数据点
     */
    private List<TrendDataPoint> storageUsageTrend;

    /**
     * 文件数量趋势数据点
     */
    private List<TrendDataPoint> fileCountTrend;

    /**
     * 趋势摘要信息
     */
    private TrendSummary summary;

    /**
     * 数据生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 趋势数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        /**
         * 时间点
         */
        private LocalDateTime timestamp;
        
        /**
         * 数值
         */
        private Long value;
        
        /**
         * 变化量（相对于前一个数据点）
         */
        private Long deltaValue;
        
        /**
         * 变化百分比
         */
        private Double deltaPercentage;
    }

    /**
     * 趋势摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendSummary {
        /**
         * 总体趋势: INCREASING, DECREASING, STABLE
         */
        private String overallTrend;
        
        /**
         * 平均日增长量
         */
        private Long averageDailyGrowth;
        
        /**
         * 最大单日增长量
         */
        private Long maxDailyGrowth;
        
        /**
         * 增长率 (%)
         */
        private Double growthRate;
        
        /**
         * 峰值时间
         */
        private LocalDateTime peakTime;
        
        /**
         * 峰值
         */
        private Long peakValue;
    }
}