package org.nan.cloud.core.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 配额分析Repository接口
 * 用于配额可视化的复杂查询和聚合分析
 */
public interface QuotaAnalysisRepository {

    /**
     * 获取配额使用趋势数据
     * @param orgId 组织ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param period 聚合周期: day, hour
     * @return 趋势数据点列表
     */
    List<Map<String, Object>> getQuotaUsageTrend(Long orgId, LocalDateTime startTime, LocalDateTime endTime, String period);

    /**
     * 获取按文件类型的配额分解统计
     * @param orgId 组织ID
     * @return 文件类型分解统计
     */
    List<Map<String, Object>> getQuotaBreakdownByFileType(Long orgId);

    /**
     * 获取按用户组的配额分解统计
     * @param orgId 组织ID
     * @return 用户组分解统计
     */
    List<Map<String, Object>> getQuotaBreakdownByUserGroup(Long orgId);

    /**
     * 获取按操作类型的配额分解统计
     * @param orgId 组织ID
     * @param days 统计天数
     * @return 操作类型分解统计
     */
    List<Map<String, Object>> getQuotaBreakdownByOperationType(Long orgId, int days);

    /**
     * 获取配额使用增长趋势分析
     * @param orgId 组织ID
     * @param days 分析天数
     * @return 增长趋势统计 {averageDailyGrowth, maxDailyGrowth, growthRate, trend}
     */
    Map<String, Object> getQuotaGrowthAnalysis(Long orgId, int days);

    /**
     * 获取最近操作统计
     * @param orgId 组织ID
     * @param days 统计天数
     * @return 操作统计
     */
    Map<String, Object> getRecentOperationStats(Long orgId, int days);
}