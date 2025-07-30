package org.nan.cloud.message.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.aggregation.BatchCommandAggregationData;
import org.nan.cloud.message.infrastructure.aggregation.BatchCommandAggregator;
import org.nan.cloud.message.infrastructure.aggregation.BatchProgressTracker;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 批量指令聚合API控制器
 * 
 * 提供批量指令聚合数据的查询和管理接口：
 * 1. 查询批量任务聚合数据
 * 2. 获取批量任务统计信息
 * 3. 管理批量任务跟踪
 * 4. 获取聚合引擎状态
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/batch-aggregation")
@RequiredArgsConstructor
public class BatchAggregationController {
    
    private final BatchCommandAggregator batchCommandAggregator;
    private final BatchProgressTracker batchProgressTracker;
    
    /**
     * 获取批量任务聚合数据
     * 
     * @param batchId 批量任务ID
     * @return 聚合数据
     */
    @GetMapping("/batch/{batchId}")
    public Map<String, Object> getBatchAggregationData(@PathVariable String batchId) {
        try {
            log.debug("查询批量任务聚合数据 - 批量ID: {}", batchId);
            
            Optional<BatchCommandAggregationData> aggregationData = 
                    batchCommandAggregator.getAggregationData(batchId);
            
            if (aggregationData.isPresent()) {
                BatchCommandAggregationData data = aggregationData.get();
                return Map.of(
                        "success", true,
                        "batchId", batchId,
                        "data", data,
                        "timestamp", System.currentTimeMillis()
                );
            } else {
                return Map.of(
                        "success", false,
                        "batchId", batchId,
                        "message", "批量任务聚合数据不存在",
                        "timestamp", System.currentTimeMillis()
                );
            }
            
        } catch (Exception e) {
            log.error("查询批量任务聚合数据失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "batchId", batchId,
                    "message", "查询失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 获取批量任务摘要信息
     * 
     * @param batchId 批量任务ID
     * @return 摘要信息
     */
    @GetMapping("/batch/{batchId}/summary")
    public Map<String, Object> getBatchSummary(@PathVariable String batchId) {
        try {
            log.debug("查询批量任务摘要 - 批量ID: {}", batchId);
            
            Optional<BatchCommandAggregationData> aggregationData = 
                    batchCommandAggregator.getAggregationData(batchId);
            
            if (aggregationData.isPresent()) {
                BatchCommandAggregationData data = aggregationData.get();

                Map<String, Object> summary = new HashMap<>();
                summary.put("batchId",                 data.getBatchId());
                summary.put("taskId",                  data.getTaskId());
                summary.put("status",                  data.getStatus().name());
                summary.put("statusDescription",       data.getStatus().getDescription());
                summary.put("totalCount",              data.getTotalCount());
                summary.put("completedCount",          data.getCompletedCount());
                summary.put("successCount",            data.getSuccessCount());
                summary.put("failureCount",            data.getFailureCount());
                summary.put("completionPercentage",    data.getCompletionPercentage());
                summary.put("successRate",             data.getSuccessRate());
                summary.put("createdTime",             data.getCreatedTime());
                summary.put("lastUpdateTime",          data.getLastUpdateTime());
                summary.put("isCompleted",             data.isCompleted());
                summary.put("isSuccessfullyCompleted", data.isSuccessfullyCompleted());
                summary.put("isPartiallyCompleted",    data.isPartiallyCompleted());
                
                return Map.of(
                        "success", true,
                        "batchId", batchId,
                        "summary", summary,
                        "timestamp", System.currentTimeMillis()
                );
            } else {
                return Map.of(
                        "success", false,
                        "batchId", batchId,
                        "message", "批量任务不存在",
                        "timestamp", System.currentTimeMillis()
                );
            }
            
        } catch (Exception e) {
            log.error("查询批量任务摘要失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "batchId", batchId,
                    "message", "查询失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 获取所有活跃批量任务
     * 
     * @return 活跃任务列表
     */
    @GetMapping("/active-batches")
    public Map<String, Object> getActiveBatches() {
        try {
            log.debug("查询所有活跃批量任务");
            
            Set<String> activeBatchIds = batchCommandAggregator.getActiveBatchIds();
            Set<String> trackedBatchIds = batchProgressTracker.getTrackedBatchIds();
            
            return Map.of(
                    "success", true,
                    "activeBatchIds", activeBatchIds,
                    "trackedBatchIds", trackedBatchIds,
                    "activeBatchCount", activeBatchIds.size(),
                    "trackedBatchCount", trackedBatchIds.size(),
                    "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("查询活跃批量任务失败 - 错误: {}", e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "message", "查询失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 获取批量任务跟踪统计信息
     * 
     * @return 跟踪统计信息
     */
    @GetMapping("/tracking-stats")
    public Map<String, Object> getTrackingStats() {
        try {
            log.debug("查询批量任务跟踪统计信息");
            
            BatchProgressTracker.BatchTrackingStats stats = batchProgressTracker.getTrackingStats();
            
            return Map.of(
                    "success", true,
                    "stats", Map.of(
                            "totalTrackedBatches", stats.getTotalTrackedBatches(),
                            "totalTimeoutBatches", stats.getTotalTimeoutBatches(),
                            "totalCompletedBatches", stats.getTotalCompletedBatches(),
                            "activeBatchCount", stats.getActiveBatchCount(),
                            "warningBatchCount", stats.getWarningBatchCount()
                    ),
                    "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("查询批量任务跟踪统计信息失败 - 错误: {}", e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "message", "查询失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 检查批量任务是否超时
     * 
     * @param batchId 批量任务ID
     * @return 超时检查结果
     */
    @GetMapping("/batch/{batchId}/timeout-check")
    public Map<String, Object> checkBatchTimeout(@PathVariable String batchId) {
        try {
            log.debug("检查批量任务超时 - 批量ID: {}", batchId);
            
            boolean isTimeout = batchProgressTracker.isBatchTimeout(batchId);
            boolean isNearTimeout = batchProgressTracker.isBatchNearTimeout(batchId);
            
            return Map.of(
                    "success", true,
                    "batchId", batchId,
                    "isTimeout", isTimeout,
                    "isNearTimeout", isNearTimeout,
                    "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("检查批量任务超时失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "batchId", batchId,
                    "message", "检查失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 手动清理已完成的批量任务数据
     * 
     * @param retentionHours 保留小时数
     * @return 清理结果
     */
    @PostMapping("/cleanup")
    public Map<String, Object> cleanupCompletedBatches(@RequestParam(defaultValue = "24") int retentionHours) {
        try {
            log.info("手动清理已完成批量任务数据 - 保留小时数: {}", retentionHours);
            
            batchCommandAggregator.cleanupCompletedBatches(retentionHours);
            
            return Map.of(
                    "success", true,
                    "message", "清理操作已执行",
                    "retentionHours", retentionHours,
                    "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("手动清理已完成批量任务数据失败 - 错误: {}", e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "message", "清理失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 模拟批量任务启动（用于测试）
     * 
     * @param request 启动请求
     * @return 启动结果
     */
    @PostMapping("/test/start-batch")
    public Map<String, Object> startTestBatch(@RequestBody Map<String, Object> request) {
        try {
            String batchId = (String) request.get("batchId");
            log.info("模拟批量任务启动 - 批量ID: {}", batchId);
            
            batchCommandAggregator.startBatchAggregation(batchId, request);
            batchProgressTracker.startTracking(batchId, null);
            
            return Map.of(
                    "success", true,
                    "batchId", batchId,
                    "message", "测试批量任务已启动",
                    "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("模拟批量任务启动失败 - 错误: {}", e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "message", "启动失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 模拟设备执行结果（用于测试）
     * 
     * @param batchId 批量任务ID
     * @param request 设备结果请求
     * @return 处理结果
     */
    @PostMapping("/test/batch/{batchId}/device-result")
    public Map<String, Object> addTestDeviceResult(@PathVariable String batchId, 
                                                  @RequestBody Map<String, Object> request) {
        try {
            String deviceId = (String) request.get("deviceId");
            log.info("模拟设备执行结果 - 批量ID: {}, 设备ID: {}", batchId, deviceId);
            
            batchCommandAggregator.aggregateDeviceResult(batchId, request);
            batchProgressTracker.updateProgress(batchId);
            
            return Map.of(
                    "success", true,
                    "batchId", batchId,
                    "deviceId", deviceId,
                    "message", "设备执行结果已处理",
                    "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("模拟设备执行结果失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "batchId", batchId,
                    "message", "处理失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
}