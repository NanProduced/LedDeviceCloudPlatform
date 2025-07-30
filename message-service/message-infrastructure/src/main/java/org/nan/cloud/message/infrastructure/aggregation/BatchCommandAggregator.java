package org.nan.cloud.message.infrastructure.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 批量指令聚合引擎
 * 
 * 核心职责：
 * 1. 聚合批量指令的执行进度和结果
 * 2. 智能触发聚合推送（时间窗口、数量阈值、完成状态）
 * 3. 生成分层聚合报告（摘要、详细、最终结果）
 * 4. 管理聚合数据的生命周期
 * 
 * 聚合策略：
 * - 时间窗口聚合：每隔N秒推送一次进度更新
 * - 数量阈值聚合：完成N个设备后推送进度
 * - 里程碑聚合：25%、50%、75%、100%完成时推送
 * - 状态变更聚合：任务状态发生变化时立即推送
 * 
 * 推送层次：
 * 1. 摘要推送：适合管理面板、总览页面
 * 2. 详细推送：适合任务详情页面、监控页面
 * 3. 最终结果：任务完成时的完整报告
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchCommandAggregator {
    
    private final StompMessageDispatcher stompDispatcher;
    private final BatchResultCollector resultCollector;
    
    /**
     * 活跃的批量任务聚合数据
     * Key: batchId, Value: 聚合数据
     */
    private final Map<String, BatchCommandAggregationData> activeBatches = new ConcurrentHashMap<>();
    
    /**
     * 批量任务的最后推送时间
     * Key: batchId, Value: 最后推送时间戳
     */
    private final Map<String, Long> lastPushTimes = new ConcurrentHashMap<>();
    
    /**
     * 聚合计数器
     */
    private final AtomicLong aggregationCounter = new AtomicLong(0);
    
    // 聚合配置
    private static final long TIME_WINDOW_MS = 5000; // 5秒时间窗口
    private static final int QUANTITY_THRESHOLD = 10; // 10个设备完成后推送
    private static final List<Integer> MILESTONE_PERCENTAGES = List.of(25, 50, 75, 100); // 里程碑百分比
    
    /**
     * 处理单个设备指令结果，更新聚合数据
     * 
     * @param batchId 批量任务ID
     * @param deviceResult 设备执行结果
     */
    public void aggregateDeviceResult(String batchId, Map<String, Object> deviceResult) {
        try {
            log.debug("聚合设备指令结果 - 批量ID: {}, 设备: {}", batchId, deviceResult.get("deviceId"));
            
            // 获取或创建聚合数据
            BatchCommandAggregationData aggregationData = getOrCreateAggregationData(batchId, deviceResult);
            
            // 更新设备执行详情
            updateDeviceExecutionDetail(aggregationData, deviceResult);
            
            // 重新计算聚合统计
            recalculateAggregationStats(aggregationData);
            
            // 检查是否需要推送更新
            checkAndTriggerPush(aggregationData, PushTrigger.DEVICE_COMPLETED);
            
            aggregationCounter.incrementAndGet();
            
        } catch (Exception e) {
            log.error("聚合设备指令结果失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 处理批量任务状态变更
     * 
     * @param batchId 批量任务ID
     * @param newStatus 新状态
     * @param statusData 状态数据
     */
    public void aggregateStatusChange(String batchId, BatchCommandAggregationData.BatchStatus newStatus, 
                                    Map<String, Object> statusData) {
        try {
            log.debug("聚合批量任务状态变更 - 批量ID: {}, 新状态: {}", batchId, newStatus);
            
            BatchCommandAggregationData aggregationData = activeBatches.get(batchId);
            if (aggregationData == null) {
                log.warn("未找到批量任务聚合数据 - 批量ID: {}", batchId);
                return;
            }
            
            // 更新状态和时间
            BatchCommandAggregationData.BatchStatus oldStatus = aggregationData.getStatus();
            aggregationData.setStatus(newStatus);
            aggregationData.setLastUpdateTime(LocalDateTime.now());
            
            // 更新扩展数据
            if (statusData != null) {
                if (aggregationData.getExtensionData() == null) {
                    aggregationData.setExtensionData(new HashMap<>());
                }
                aggregationData.getExtensionData().putAll(statusData);
            }
            
            // 状态变更需要立即推送
            checkAndTriggerPush(aggregationData, PushTrigger.STATUS_CHANGED);
            
            // 如果任务完成，进行最终处理
            if (aggregationData.isCompleted()) {
                handleBatchCompletion(aggregationData);
            }
            
            log.info("✅ 批量任务状态变更聚合完成 - 批量ID: {}, {} -> {}", batchId, oldStatus, newStatus);
            
        } catch (Exception e) {
            log.error("聚合批量任务状态变更失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 启动批量任务聚合
     * 
     * @param batchId 批量任务ID
     * @param initialData 初始数据
     */
    public void startBatchAggregation(String batchId, Map<String, Object> initialData) {
        try {
            log.info("启动批量任务聚合 - 批量ID: {}", batchId);
            
            BatchCommandAggregationData aggregationData = createInitialAggregationData(batchId, initialData);
            activeBatches.put(batchId, aggregationData);
            lastPushTimes.put(batchId, System.currentTimeMillis());
            
            // 发送初始状态推送
            pushSummaryUpdate(aggregationData, PushTrigger.BATCH_STARTED);
            
            log.info("✅ 批量任务聚合启动完成 - 批量ID: {}, 总设备数: {}", 
                    batchId, aggregationData.getTotalCount());
            
        } catch (Exception e) {
            log.error("启动批量任务聚合失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取批量任务聚合数据
     * 
     * @param batchId 批量任务ID
     * @return 聚合数据
     */
    public Optional<BatchCommandAggregationData> getAggregationData(String batchId) {
        return Optional.ofNullable(activeBatches.get(batchId));
    }
    
    /**
     * 获取所有活跃批量任务
     * 
     * @return 活跃任务ID集合
     */
    public Set<String> getActiveBatchIds() {
        return new HashSet<>(activeBatches.keySet());
    }
    
    /**
     * 清理已完成的批量任务数据
     * 
     * @param retentionHours 保留小时数
     */
    public void cleanupCompletedBatches(int retentionHours) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(retentionHours);
            List<String> toRemove = new ArrayList<>();
            
            for (Map.Entry<String, BatchCommandAggregationData> entry : activeBatches.entrySet()) {
                BatchCommandAggregationData data = entry.getValue();
                if (data.isCompleted() && data.getLastUpdateTime().isBefore(cutoffTime)) {
                    toRemove.add(entry.getKey());
                }
            }
            
            for (String batchId : toRemove) {
                activeBatches.remove(batchId);
                lastPushTimes.remove(batchId);
                log.debug("清理已完成批量任务 - 批量ID: {}", batchId);
            }
            
            if (!toRemove.isEmpty()) {
                log.info("✅ 清理已完成批量任务 - 清理数量: {}, 剩余活跃任务: {}", 
                        toRemove.size(), activeBatches.size());
            }
            
        } catch (Exception e) {
            log.error("清理已完成批量任务失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 获取或创建聚合数据
     */
    private BatchCommandAggregationData getOrCreateAggregationData(String batchId, Map<String, Object> deviceResult) {
        return activeBatches.computeIfAbsent(batchId, k -> {
            log.info("创建新的批量任务聚合数据 - 批量ID: {}", batchId);
            return createAggregationDataFromDeviceResult(batchId, deviceResult);
        });
    }
    
    /**
     * 从设备结果创建聚合数据
     */
    private BatchCommandAggregationData createAggregationDataFromDeviceResult(String batchId, 
                                                                            Map<String, Object> deviceResult) {
        return BatchCommandAggregationData.builder()
                .batchId(batchId)
                .taskId((String) deviceResult.get("taskId"))
                .orgId(getLongValue(deviceResult, "orgId"))
                .userId(getLongValue(deviceResult, "userId"))
                .createdTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .status(BatchCommandAggregationData.BatchStatus.RUNNING)
                .totalCount(getIntValue(deviceResult, "totalCount", 1))
                .completedCount(0)
                .successCount(0)
                .failureCount(0)
                .timeoutCount(0)
                .skippedCount(0)
                .completionPercentage(0.0)
                .successRate(0.0)
                .deviceDetails(new ArrayList<>())
                .errorStatistics(new HashMap<>())
                .performanceMetrics(BatchCommandAggregationData.PerformanceMetrics.builder()
                        .firstCommandTime(LocalDateTime.now())
                        .build())
                .extensionData(new HashMap<>())
                .build();
    }
    
    /**
     * 创建初始聚合数据
     */
    private BatchCommandAggregationData createInitialAggregationData(String batchId, Map<String, Object> initialData) {
        return BatchCommandAggregationData.builder()
                .batchId(batchId)
                .taskId((String) initialData.get("taskId"))
                .orgId(getLongValue(initialData, "orgId"))
                .userId(getLongValue(initialData, "userId"))
                .createdTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .status(BatchCommandAggregationData.BatchStatus.PENDING)
                .totalCount(getIntValue(initialData, "totalCount", 0))
                .completedCount(0)
                .successCount(0)
                .failureCount(0)
                .timeoutCount(0)
                .skippedCount(0)
                .completionPercentage(0.0)
                .successRate(0.0)
                .deviceDetails(new ArrayList<>())
                .errorStatistics(new HashMap<>())
                .performanceMetrics(BatchCommandAggregationData.PerformanceMetrics.builder()
                        .firstCommandTime(LocalDateTime.now())
                        .build())
                .extensionData(new HashMap<>(initialData))
                .build();
    }
    
    /**
     * 更新设备执行详情
     */
    private void updateDeviceExecutionDetail(BatchCommandAggregationData aggregationData, 
                                           Map<String, Object> deviceResult) {
        String deviceId = (String) deviceResult.get("deviceId");
        String commandId = (String) deviceResult.get("commandId");
        String status = (String) deviceResult.get("status");
        String result = (String) deviceResult.get("result");
        
        // 创建设备执行详情
        BatchCommandAggregationData.DeviceExecutionDetail detail = 
                BatchCommandAggregationData.DeviceExecutionDetail.builder()
                        .deviceId(deviceId)
                        .commandId(commandId)
                        .status(status)
                        .result(result)
                        .errorCode((String) deviceResult.get("errorCode"))
                        .errorMessage((String) deviceResult.get("errorMessage"))
                        .startTime(parseDateTime(deviceResult.get("startTime")))
                        .endTime(LocalDateTime.now())
                        .executionTime(getLongValue(deviceResult, "executionTime"))
                        .retryCount(getIntValue(deviceResult, "retryCount", 0))
                        .resultData((Map<String, Object>) deviceResult.get("resultData"))
                        .build();
        
        // 计算执行时间
        if (detail.getStartTime() != null && detail.getEndTime() != null) {
            detail.setExecutionTime(java.time.Duration.between(detail.getStartTime(), detail.getEndTime()).toMillis());
        }
        
        // 添加到详情列表
        aggregationData.getDeviceDetails().add(detail);
        
        // 更新错误统计
        if (detail.getErrorCode() != null) {
            aggregationData.getErrorStatistics().merge(detail.getErrorCode(), 1, Integer::sum);
        }
    }
    
    /**
     * 重新计算聚合统计
     */
    private void recalculateAggregationStats(BatchCommandAggregationData aggregationData) {
        List<BatchCommandAggregationData.DeviceExecutionDetail> details = aggregationData.getDeviceDetails();
        
        // 计算基础统计
        aggregationData.setCompletedCount(details.size());
        aggregationData.setSuccessCount((int) details.stream()
                .filter(d -> "SUCCESS".equalsIgnoreCase(d.getStatus()) || "COMPLETED".equalsIgnoreCase(d.getStatus()))
                .count());
        aggregationData.setFailureCount((int) details.stream()
                .filter(d -> "FAILED".equalsIgnoreCase(d.getStatus()) || "ERROR".equalsIgnoreCase(d.getStatus()))
                .count());
        aggregationData.setTimeoutCount((int) details.stream()
                .filter(d -> "TIMEOUT".equalsIgnoreCase(d.getStatus()))
                .count());
        
        // 计算百分比
        aggregationData.calculateCompletionPercentage();
        aggregationData.calculateSuccessRate();
        
        // 计算性能指标
        calculatePerformanceMetrics(aggregationData, details);
        
        // 更新最后更新时间
        aggregationData.setLastUpdateTime(LocalDateTime.now());
    }
    
    /**
     * 计算性能指标
     */
    private void calculatePerformanceMetrics(BatchCommandAggregationData aggregationData,
                                           List<BatchCommandAggregationData.DeviceExecutionDetail> details) {
        if (details.isEmpty()) {
            return;
        }
        
        List<Long> executionTimes = details.stream()
                .filter(d -> d.getExecutionTime() != null)
                .map(BatchCommandAggregationData.DeviceExecutionDetail::getExecutionTime)
                .sorted()
                .toList();
        
        if (!executionTimes.isEmpty()) {
            BatchCommandAggregationData.PerformanceMetrics metrics = aggregationData.getPerformanceMetrics();
            if (metrics == null) {
                metrics = BatchCommandAggregationData.PerformanceMetrics.builder().build();
                aggregationData.setPerformanceMetrics(metrics);
            }
            
            metrics.setMinExecutionTime(executionTimes.get(0));
            metrics.setMaxExecutionTime(executionTimes.get(executionTimes.size() - 1));
            metrics.setAverageExecutionTime(executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
            metrics.setTotalExecutionTime(executionTimes.stream().mapToLong(Long::longValue).sum());
            metrics.setLastCommandTime(LocalDateTime.now());
        }
    }
    
    /**
     * 检查并触发推送
     */
    private void checkAndTriggerPush(BatchCommandAggregationData aggregationData, PushTrigger trigger) {
        String batchId = aggregationData.getBatchId();
        long currentTime = System.currentTimeMillis();
        Long lastPushTime = lastPushTimes.get(batchId);
        
        boolean shouldPush = false;
        String reason = "";
        
        // 检查触发条件
        switch (trigger) {
            case BATCH_STARTED:
            case STATUS_CHANGED:
                shouldPush = true;
                reason = trigger.name();
                break;
                
            case DEVICE_COMPLETED:
                // 时间窗口检查
                if (lastPushTime == null || (currentTime - lastPushTime) >= TIME_WINDOW_MS) {
                    shouldPush = true;
                    reason = "TIME_WINDOW";
                }
                // 数量阈值检查
                else if (aggregationData.getCompletedCount() != null && 
                         aggregationData.getCompletedCount() % QUANTITY_THRESHOLD == 0) {
                    shouldPush = true;
                    reason = "QUANTITY_THRESHOLD";
                }
                // 里程碑检查
                else if (isMilestoneReached(aggregationData)) {
                    shouldPush = true;
                    reason = "MILESTONE";
                }
                break;
        }
        
        if (shouldPush) {
            pushAggregationUpdate(aggregationData, trigger, reason);
            lastPushTimes.put(batchId, currentTime);
        }
    }
    
    /**
     * 检查是否达到里程碑
     */
    private boolean isMilestoneReached(BatchCommandAggregationData aggregationData) {
        Double completionPercentage = aggregationData.getCompletionPercentage();
        if (completionPercentage == null) {
            return false;
        }
        
        for (Integer milestone : MILESTONE_PERCENTAGES) {
            if (Math.abs(completionPercentage - milestone) < 1.0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 推送聚合更新
     */
    @Async
    private void pushAggregationUpdate(BatchCommandAggregationData aggregationData,
                                     PushTrigger trigger, String reason) {
        try {
            log.debug("推送批量任务聚合更新 - 批量ID: {}, 触发器: {}, 原因: {}", 
                    aggregationData.getBatchId(), trigger, reason);
            
            // 总是推送摘要更新
            pushSummaryUpdate(aggregationData, trigger);
            
            // 根据触发器决定是否推送详细更新
            if (trigger == PushTrigger.STATUS_CHANGED || 
                trigger == PushTrigger.BATCH_STARTED ||
                "MILESTONE".equals(reason)) {
                pushDetailedUpdate(aggregationData, trigger);
            }
            
            // 如果任务完成，推送最终结果
            if (aggregationData.isCompleted()) {
                pushFinalResult(aggregationData);
            }
            
        } catch (Exception e) {
            log.error("推送批量任务聚合更新失败 - 批量ID: {}, 错误: {}", 
                    aggregationData.getBatchId(), e.getMessage(), e);
        }
    }
    
    /**
     * 推送摘要更新
     */
    private void pushSummaryUpdate(BatchCommandAggregationData aggregationData, PushTrigger trigger) {
        CommonStompMessage summaryMessage = resultCollector.buildSummaryMessage(aggregationData, trigger);
        DispatchResult result = stompDispatcher.smartDispatch(summaryMessage);
        
        log.debug("批量任务摘要推送结果 - 批量ID: {}, 成功: {}", 
                aggregationData.getBatchId(), result.isSuccess());
    }
    
    /**
     * 推送详细更新
     */
    private void pushDetailedUpdate(BatchCommandAggregationData aggregationData, PushTrigger trigger) {
        CommonStompMessage detailedMessage = resultCollector.buildDetailedMessage(aggregationData, trigger);
        DispatchResult result = stompDispatcher.smartDispatch(detailedMessage);
        
        log.debug("批量任务详细推送结果 - 批量ID: {}, 成功: {}", 
                aggregationData.getBatchId(), result.isSuccess());
    }
    
    /**
     * 推送最终结果
     */
    private void pushFinalResult(BatchCommandAggregationData aggregationData) {
        CommonStompMessage finalMessage = resultCollector.buildFinalResultMessage(aggregationData);
        DispatchResult result = stompDispatcher.smartDispatch(finalMessage);
        
        log.info("✅ 批量任务最终结果推送 - 批量ID: {}, 成功: {}, 成功率: {}%", 
                aggregationData.getBatchId(), result.isSuccess(), aggregationData.getSuccessRate());
    }
    
    /**
     * 处理批量任务完成
     */
    private void handleBatchCompletion(BatchCommandAggregationData aggregationData) {
        try {
            String batchId = aggregationData.getBatchId();
            log.info("处理批量任务完成 - 批量ID: {}, 状态: {}, 成功率: {}%", 
                    batchId, aggregationData.getStatus(), aggregationData.getSuccessRate());
            
            // 最终结果已在pushAggregationUpdate中处理
            
            // 可以在这里添加其他完成处理逻辑，如：
            // - 发送邮件通知
            // - 更新数据库状态
            // - 触发后续工作流
            
        } catch (Exception e) {
            log.error("处理批量任务完成失败 - 批量ID: {}, 错误: {}", 
                    aggregationData.getBatchId(), e.getMessage(), e);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 推送触发器枚举
     */
    public enum PushTrigger {
        BATCH_STARTED,
        DEVICE_COMPLETED,
        STATUS_CHANGED,
        BATCH_COMPLETED
    }
    
    /**
     * 安全获取Long值
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 安全获取Integer值
     */
    private Integer getIntValue(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 解析日期时间
     */
    private LocalDateTime parseDateTime(Object dateTimeValue) {
        if (dateTimeValue == null) return null;
        if (dateTimeValue instanceof LocalDateTime) return (LocalDateTime) dateTimeValue;
        try {
            return LocalDateTime.parse(dateTimeValue.toString());
        } catch (Exception e) {
            return null;
        }
    }
}