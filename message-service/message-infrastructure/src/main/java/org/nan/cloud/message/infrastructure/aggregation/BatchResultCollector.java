package org.nan.cloud.message.infrastructure.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 批量结果收集器
 * 
 * 负责将批量指令聚合数据转换为不同层次的STOMP消息：
 * 1. 摘要消息：适合管理面板、总览显示
 * 2. 详细消息：适合任务详情页面、监控面板
 * 3. 最终结果：任务完成时的完整报告
 * 
 * 消息构建策略：
 * - 摘要消息：包含核心统计数据，消息体积小，推送频率高
 * - 详细消息：包含设备执行详情，消息体积中等，推送频率中等
 * - 最终结果：包含完整执行报告，消息体积大，仅推送一次
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchResultCollector {
    
    /**
     * 构建批量任务摘要消息
     * 
     * @param aggregationData 聚合数据
     * @param trigger 触发器
     * @return STOMP摘要消息
     */
    public CommonStompMessage buildSummaryMessage(BatchCommandAggregationData aggregationData, 
                                                BatchCommandAggregator.PushTrigger trigger) {
        try {
            log.debug("构建批量任务摘要消息 - 批量ID: {}, 触发器: {}", 
                    aggregationData.getBatchId(), trigger);
            
            // 构建摘要载荷
            Map<String, Object> summaryPayload = buildSummaryPayload(aggregationData);
            
            // 确定消息优先级
            Priority priority = determinePriority(aggregationData, trigger);
            
            // 构建主题路径
            String topicPath = buildBatchAggTopicPath(aggregationData);
            
            return CommonStompMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .messageType(StompMessageTypes.TASK_PROGRESS)
                    .subType_1("BATCH_SUMMARY")
                    .subType_2(aggregationData.getStatus().name())
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("message-service")
                            .resourceType("BATCH_COMMAND")
                            .resourceId(aggregationData.getBatchId())
                            .taskId(aggregationData.getTaskId())
                            .batchContext("AGGREGATION_ENGINE")
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("USER_AND_TOPIC")
                            .uids(aggregationData.getUserId() != null ? List.of(aggregationData.getUserId()) : null)
                            .oid(aggregationData.getOrgId())
                            .destination(topicPath)
                            .build())
                    .payload(summaryPayload)
                    .message(buildSummaryMessage(aggregationData))
                    .metadata(CommonStompMessage.Metadata.builder()
                            .priority(priority)
                            .persistent(false)  // 摘要消息不持久化
                            .ttl(300000L)      // 5分钟TTL
                            .sequenceId(System.currentTimeMillis())
                            .correlationId(aggregationData.getBatchId())
                            .build())
                    .build();
                    
        } catch (Exception e) {
            log.error("构建批量任务摘要消息失败 - 批量ID: {}, 错误: {}", 
                    aggregationData.getBatchId(), e.getMessage(), e);
            throw new RuntimeException("构建摘要消息失败", e);
        }
    }
    
    /**
     * 构建批量任务详细消息
     * 
     * @param aggregationData 聚合数据
     * @param trigger 触发器
     * @return STOMP详细消息
     */
    public CommonStompMessage buildDetailedMessage(BatchCommandAggregationData aggregationData, 
                                                 BatchCommandAggregator.PushTrigger trigger) {
        try {
            log.debug("构建批量任务详细消息 - 批量ID: {}, 触发器: {}", 
                    aggregationData.getBatchId(), trigger);
            
            // 构建详细载荷
            Map<String, Object> detailedPayload = buildDetailedPayload(aggregationData);
            
            // 确定消息优先级
            Priority priority = determinePriority(aggregationData, trigger);
            
            // 构建主题路径
            String topicPath = buildBatchAggTopicPath(aggregationData);
            
            return CommonStompMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .messageType(StompMessageTypes.TASK_PROGRESS)
                    .subType_1("BATCH_DETAILED")
                    .subType_2(aggregationData.getStatus().name())
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("message-service")
                            .resourceType("BATCH_COMMAND")
                            .resourceId(aggregationData.getBatchId())
                            .taskId(aggregationData.getTaskId())
                            .batchContext("AGGREGATION_ENGINE")
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("USER_AND_DETAILED_TOPIC")
                            .uids(aggregationData.getUserId() != null ? List.of(aggregationData.getUserId()) : null)
                            .oid(aggregationData.getOrgId())
                            .destination(topicPath)
                            .build())
                    .payload(detailedPayload)
                    .message(buildDetailedMessageString(aggregationData))
                    .metadata(CommonStompMessage.Metadata.builder()
                            .priority(priority)
                            .persistent(true)   // 详细消息需要持久化
                            .ttl(1800000L)     // 30分钟TTL
                            .sequenceId(System.currentTimeMillis())
                            .correlationId(aggregationData.getBatchId())
                            .build())
                    .build();
                    
        } catch (Exception e) {
            log.error("构建批量任务详细消息失败 - 批量ID: {}, 错误: {}", 
                    aggregationData.getBatchId(), e.getMessage(), e);
            throw new RuntimeException("构建详细消息失败", e);
        }
    }
    
    /**
     * 构建批量任务最终结果消息
     * 
     * @param aggregationData 聚合数据
     * @return STOMP最终结果消息
     */
    public CommonStompMessage buildFinalResultMessage(BatchCommandAggregationData aggregationData) {
        try {
            log.debug("构建批量任务最终结果消息 - 批量ID: {}", aggregationData.getBatchId());
            
            // 构建最终结果载荷
            Map<String, Object> finalPayload = buildFinalResultPayload(aggregationData);
            
            // 最终结果消息优先级较高
            Priority priority = aggregationData.isSuccessfullyCompleted() ? Priority.NORMAL : Priority.HIGH;
            
            // 构建主题路径
            String topicPath = buildBatchAggTopicPath(aggregationData);
            
            return CommonStompMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .messageType(StompMessageTypes.TASK_PROGRESS)
                    .subType_1("BATCH_FINAL")
                    .subType_2(aggregationData.getStatus().name())
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("message-service")
                            .resourceType("BATCH_COMMAND")
                            .resourceId(aggregationData.getBatchId())
                            .taskId(aggregationData.getTaskId())
                            .batchContext("AGGREGATION_ENGINE")
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("USER_AND_RESULT_TOPIC")
                            .uids(aggregationData.getUserId() != null ? List.of(aggregationData.getUserId()) : null)
                            .oid(aggregationData.getOrgId())
                            .destination(topicPath)
                            .build())
                    .payload(finalPayload)
                    .message(buildFinalResultMessageString(aggregationData))
                    .metadata(CommonStompMessage.Metadata.builder()
                            .priority(priority)
                            .persistent(true)   // 最终结果需要持久化
                            .ttl(86400000L)    // 24小时TTL
                            .requireAck(true)   // 需要确认
                            .correlationId(aggregationData.getBatchId())
                            .build())
                    .build();
                    
        } catch (Exception e) {
            log.error("构建批量任务最终结果消息失败 - 批量ID: {}, 错误: {}", 
                    aggregationData.getBatchId(), e.getMessage(), e);
            throw new RuntimeException("构建最终结果消息失败", e);
        }
    }
    
    // ==================== 载荷构建方法 ====================
    
    /**
     * 构建摘要载荷
     */
    private Map<String, Object> buildSummaryPayload(BatchCommandAggregationData aggregationData) {
        Map<String, Object> payload = new HashMap<>();
        
        // 基础信息
        payload.put("batchId", aggregationData.getBatchId());
        payload.put("taskId", aggregationData.getTaskId());
        payload.put("status", aggregationData.getStatus().name());
        payload.put("statusDescription", aggregationData.getStatus().getDescription());
        
        // 核心统计
        payload.put("totalCount", aggregationData.getTotalCount());
        payload.put("completedCount", aggregationData.getCompletedCount());
        payload.put("successCount", aggregationData.getSuccessCount());
        payload.put("failureCount", aggregationData.getFailureCount());
        payload.put("completionPercentage", aggregationData.getCompletionPercentage());
        payload.put("successRate", aggregationData.getSuccessRate());
        
        // 时间信息
        payload.put("createdTime", aggregationData.getCreatedTime());
        payload.put("lastUpdateTime", aggregationData.getLastUpdateTime());
        
        // 简单性能指标
        if (aggregationData.getPerformanceMetrics() != null) {
            payload.put("averageExecutionTime", aggregationData.getPerformanceMetrics().getAverageExecutionTime());
        }
        
        return payload;
    }
    
    /**
     * 构建详细载荷
     */
    private Map<String, Object> buildDetailedPayload(BatchCommandAggregationData aggregationData) {
        Map<String, Object> payload = buildSummaryPayload(aggregationData);
        
        // 添加详细统计
        payload.put("timeoutCount", aggregationData.getTimeoutCount());
        payload.put("skippedCount", aggregationData.getSkippedCount());
        
        // 错误统计
        payload.put("errorStatistics", aggregationData.getErrorStatistics());
        
        // 详细性能指标
        if (aggregationData.getPerformanceMetrics() != null) {
            BatchCommandAggregationData.PerformanceMetrics metrics = aggregationData.getPerformanceMetrics();
            Map<String, Object> performanceData = new HashMap<>();
            performanceData.put("totalExecutionTime", metrics.getTotalExecutionTime());
            performanceData.put("minExecutionTime", metrics.getMinExecutionTime());
            performanceData.put("maxExecutionTime", metrics.getMaxExecutionTime());
            performanceData.put("averageExecutionTime", metrics.getAverageExecutionTime());
            performanceData.put("throughputPerSecond", metrics.getThroughputPerSecond());
            performanceData.put("firstCommandTime", metrics.getFirstCommandTime());
            performanceData.put("lastCommandTime", metrics.getLastCommandTime());
            payload.put("performanceMetrics", performanceData);
        }
        
        // 最近的设备执行详情（限制数量以控制消息大小）
        List<BatchCommandAggregationData.DeviceExecutionDetail> recentDetails = 
                aggregationData.getDeviceDetails().stream()
                        .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                        .limit(20)  // 最多包含最近20个设备的详情
                        .toList();
        payload.put("recentDeviceDetails", recentDetails);
        payload.put("totalDeviceDetailsCount", aggregationData.getDeviceDetails().size());
        
        return payload;
    }
    
    /**
     * 构建最终结果载荷
     */
    private Map<String, Object> buildFinalResultPayload(BatchCommandAggregationData aggregationData) {
        Map<String, Object> payload = buildDetailedPayload(aggregationData);
        
        // 完整的设备执行详情
        payload.put("allDeviceDetails", aggregationData.getDeviceDetails());
        payload.remove("recentDeviceDetails");
        payload.remove("totalDeviceDetailsCount");
        
        // 扩展数据
        payload.put("extensionData", aggregationData.getExtensionData());
        
        // 最终结果汇总
        Map<String, Object> resultSummary = new HashMap<>();
        resultSummary.put("isCompleted", aggregationData.isCompleted());
        resultSummary.put("isSuccessfullyCompleted", aggregationData.isSuccessfullyCompleted());
        resultSummary.put("isPartiallyCompleted", aggregationData.isPartiallyCompleted());
        resultSummary.put("finalStatus", aggregationData.getStatus().name());
        resultSummary.put("finalStatusDescription", aggregationData.getStatus().getDescription());
        payload.put("resultSummary", resultSummary);
        
        return payload;
    }
    
    // ==================== 主题路径构建方法 ====================

    public String buildBatchAggTopicPath(BatchCommandAggregationData aggregationData) {
        String batchAggTopic = StompTopic.buildBatchAggTopic(aggregationData.getBatchId());

        if (aggregationData.getUserId() != null) {
            return batchAggTopic + "," + StompTopic.USER_MESSAGES_QUEUE;
        }

        return batchAggTopic;
    }
    
    // ==================== 消息文本构建方法 ====================
    
    /**
     * 构建摘要消息文本
     */
    private String buildSummaryMessage(BatchCommandAggregationData aggregationData) {
        return String.format("批量任务进度更新 - %s: %d/%d (%.1f%%), 成功率: %.1f%%",
                aggregationData.getBatchId(),
                aggregationData.getCompletedCount(),
                aggregationData.getTotalCount(),
                aggregationData.getCompletionPercentage(),
                aggregationData.getSuccessRate());
    }
    
    /**
     * 构建详细消息文本
     */
    private String buildDetailedMessageString(BatchCommandAggregationData aggregationData) {
        return String.format("批量任务详细进度 - %s: 总计%d, 完成%d, 成功%d, 失败%d, 状态:%s",
                aggregationData.getBatchId(),
                aggregationData.getTotalCount(),
                aggregationData.getCompletedCount(),
                aggregationData.getSuccessCount(),
                aggregationData.getFailureCount(),
                aggregationData.getStatus().getDescription());
    }

    
    /**
     * 构建最终结果消息文本
     */
    private String buildFinalResultMessageString(BatchCommandAggregationData aggregationData) {
        String statusIcon = aggregationData.isSuccessfullyCompleted() ? "✅" : 
                           aggregationData.isPartiallyCompleted() ? "⚠️" : "❌";
        
        return String.format("%s 批量任务完成 - %s: %s, 成功率: %.1f%% (%d/%d)",
                statusIcon,
                aggregationData.getBatchId(),
                aggregationData.getStatus().getDescription(),
                aggregationData.getSuccessRate(),
                aggregationData.getSuccessCount(),
                aggregationData.getTotalCount());
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 确定消息优先级
     */
    private Priority determinePriority(BatchCommandAggregationData aggregationData, 
                                     BatchCommandAggregator.PushTrigger trigger) {
        // 状态变更和批量开始的消息优先级较高
        if (trigger == BatchCommandAggregator.PushTrigger.STATUS_CHANGED ||
            trigger == BatchCommandAggregator.PushTrigger.BATCH_STARTED) {
            return Priority.HIGH;
        }
        
        // 失败率较高的任务优先级较高
        if (aggregationData.getSuccessRate() != null && aggregationData.getSuccessRate() < 80.0) {
            return Priority.HIGH;
        }
        
        // 其他情况使用普通优先级
        return Priority.NORMAL;
    }
}