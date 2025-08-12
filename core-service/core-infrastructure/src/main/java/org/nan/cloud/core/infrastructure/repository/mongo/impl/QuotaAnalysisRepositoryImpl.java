package org.nan.cloud.core.infrastructure.repository.mongo.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.repository.QuotaAnalysisRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 配额分析Repository MongoDB实现
 * 基于org_quota_change_log集合的聚合查询
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QuotaAnalysisRepositoryImpl implements QuotaAnalysisRepository {

    private static final String COLLECTION_NAME = "org_quota_change_log";
    
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Map<String, Object>> getQuotaUsageTrend(Long orgId, LocalDateTime startTime, LocalDateTime endTime, String period) {
        log.debug("获取配额使用趋势: orgId={}, period={}, range={} to {}", orgId, period, startTime, endTime);

        // 构建聚合管道
        Aggregation aggregation = Aggregation.newAggregation(
            // 1. 筛选条件：组织ID和时间范围
            Aggregation.match(Criteria.where("oid").is(orgId)
                .and("createdAt").gte(startTime).lte(endTime)),
            
            // 2. 按时间分组聚合
            Aggregation.project()
                .and("oid").as("oid")
                .and("bytesChange").as("bytesChange") 
                .and("filesChange").as("filesChange")
                .and(getDateFormatExpression(period)).as("timeGroup"),
            
            Aggregation.group("timeGroup")
                .sum("bytesChange").as("totalBytesChange")
                .sum("filesChange").as("totalFilesChange")
                .count().as("operationCount"),
            
            // 3. 排序
            Aggregation.sort(Sort.Direction.ASC, "_id"),
            
            // 4. 重塑输出结构
            Aggregation.project()
                .and("_id").as("timestamp")
                .and("totalBytesChange").as("bytesChange")
                .and("totalFilesChange").as("filesChange")
                .and("operationCount").as("operationCount")
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>) mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Map.class).getMappedResults();
        return results;
    }

    @Override
    public List<Map<String, Object>> getQuotaBreakdownByFileType(Long orgId) {
        log.debug("获取按文件类型的配额分解: orgId={}", orgId);

        Aggregation aggregation = Aggregation.newAggregation(
            // 1. 筛选组织
            Aggregation.match(Criteria.where("oid").is(orgId)),
            
            // 2. 按文件类型分组
            Aggregation.group("fileType")
                .sum("bytesChange").as("totalBytes")
                .sum("filesChange").as("totalFiles")
                .count().as("operationCount"),
            
            // 3. 筛选正数（当前占用的资源）
            Aggregation.match(Criteria.where("totalBytes").gt(0)),
            
            // 4. 排序（按占用空间降序）
            Aggregation.sort(Sort.Direction.DESC, "totalBytes"),
            
            // 5. 重塑输出
            Aggregation.project()
                .and("_id").as("fileType")
                .and("totalBytes").as("usedBytes")
                .and("totalFiles").as("fileCount")
                .and("operationCount").as("operationCount")
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>) mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Map.class).getMappedResults();
        return results;
    }

    @Override
    public List<Map<String, Object>> getQuotaBreakdownByUserGroup(Long orgId) {
        log.debug("获取按用户组的配额分解: orgId={}", orgId);

        Aggregation aggregation = Aggregation.newAggregation(
            // 1. 筛选组织和有效用户组
            Aggregation.match(Criteria.where("oid").is(orgId).and("ugid").ne(null)),
            
            // 2. 按用户组分组
            Aggregation.group("ugid")
                .sum("bytesChange").as("totalBytes")
                .sum("filesChange").as("totalFiles")
                .addToSet("operationUid").as("uniqueUsers")
                .count().as("operationCount"),
            
            // 3. 筛选正数（当前占用的资源）
            Aggregation.match(Criteria.where("totalBytes").gt(0)),
            
            // 4. 排序（按占用空间降序）
            Aggregation.sort(Sort.Direction.DESC, "totalBytes"),
            
            // 5. 重塑输出
            Aggregation.project()
                .and("_id").as("userGroupId")
                .and("totalBytes").as("usedBytes")
                .and("totalFiles").as("fileCount")
                .andExpression("{$size: '$uniqueUsers'}").as("activeUserCount")
                .and("operationCount").as("operationCount")
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>) mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Map.class).getMappedResults();
        return results;
    }

    @Override
    public List<Map<String, Object>> getQuotaBreakdownByOperationType(Long orgId, int days) {
        log.debug("获取按操作类型的配额分解: orgId={}, days={}", orgId, days);

        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        
        Aggregation aggregation = Aggregation.newAggregation(
            // 1. 筛选组织和时间范围
            Aggregation.match(Criteria.where("oid").is(orgId)
                .and("createdAt").gte(startTime)),
            
            // 2. 按操作类型分组
            Aggregation.group("quotaOperationType")
                .sum("bytesChange").as("totalBytes")
                .count().as("operationCount")
                .max("createdAt").as("lastOperationTime"),
            
            // 3. 排序（按操作次数降序）
            Aggregation.sort(Sort.Direction.DESC, "operationCount"),
            
            // 4. 重塑输出
            Aggregation.project()
                .and("_id").as("operationType")
                .and("totalBytes").as("totalBytes")
                .and("operationCount").as("operationCount")
                .and("lastOperationTime").as("lastOperationTime")
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>) mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Map.class).getMappedResults();
        return results;
    }

    @Override
    public Map<String, Object> getQuotaGrowthAnalysis(Long orgId, int days) {
        log.debug("获取配额增长趋势分析: orgId={}, days={}", orgId, days);

        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        
        Aggregation aggregation = Aggregation.newAggregation(
            // 1. 筛选条件
            Aggregation.match(Criteria.where("oid").is(orgId)
                .and("createdAt").gte(startTime)
                .and("bytesChange").gt(0)), // 只统计增长
            
            // 2. 添加日期字段
            Aggregation.project()
                .and("oid").as("oid")
                .and("bytesChange").as("bytesChange")
                .and(getDateFormatExpression("day")).as("dateGroup"),
            
            // 3. 按天分组
            Aggregation.group("dateGroup")
                .sum("bytesChange").as("dailyGrowth"),
            
            // 4. 计算统计数据
            Aggregation.group()
                .avg("dailyGrowth").as("averageDailyGrowth")
                .max("dailyGrowth").as("maxDailyGrowth")
                .sum("dailyGrowth").as("totalGrowth")
                .count().as("activeDays")
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>) mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Map.class).getMappedResults();
        return results.isEmpty() ? Map.of() : results.get(0);
    }

    @Override
    public Map<String, Object> getRecentOperationStats(Long orgId, int days) {
        log.debug("获取最近操作统计: orgId={}, days={}", orgId, days);

        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        
        Aggregation aggregation = Aggregation.newAggregation(
            // 1. 筛选条件
            Aggregation.match(Criteria.where("oid").is(orgId)
                .and("createdAt").gte(startTime)),
            
            // 2. 统计
            Aggregation.group()
                .count().as("totalOperations")
                .sum("bytesChange").as("totalBytesChange")
                .sum("filesChange").as("totalFilesChange")
                .addToSet("fileType").as("fileTypes")
                .addToSet("quotaOperationType").as("operationTypes")
                .max("createdAt").as("lastOperationTime")
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>) mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Map.class).getMappedResults();
        return results.isEmpty() ? Map.of() : results.get(0);
    }

    /**
     * 根据周期获取日期分组表达式
     */
    private String getDateGroupExpression(String period) {
        return switch (period.toLowerCase()) {
            case "hour" -> "hour";
            case "day" -> "day"; 
            case "week" -> "week";
            case "month" -> "month";
            default -> "day";
        };
    }
    
    /**
     * 根据周期获取MongoDB日期格式化表达式
     */
    private DateOperators.DateToString getDateFormatExpression(String period) {
        return switch (period.toLowerCase()) {
            case "hour" -> DateOperators.DateToString.dateOf("createdAt").toString("%Y-%m-%d %H:00:00");
            case "day" -> DateOperators.DateToString.dateOf("createdAt").toString("%Y-%m-%d");
            case "week" -> DateOperators.DateToString.dateOf("createdAt").toString("%Y-W%U");
            case "month" -> DateOperators.DateToString.dateOf("createdAt").toString("%Y-%m");
            default -> DateOperators.DateToString.dateOf("createdAt").toString("%Y-%m-%d");
        };
    }
}