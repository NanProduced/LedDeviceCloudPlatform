package org.nan.cloud.message.infrastructure.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 批量进度跟踪器
 * 
 * 核心职责：
 * 1. 定期检查批量任务执行进度
 * 2. 检测超时和异常情况
 * 3. 自动清理过期数据
 * 4. 提供批量任务统计信息
 * 
 * 监控功能：
 * - 超时检测：检测长时间未更新的批量任务
 * - 异常检测：检测异常停止的批量任务
 * - 性能监控：统计批量任务执行性能
 * - 资源监控：监控内存使用和数据量
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProgressTracker {
    
    private final BatchCommandAggregator batchCommandAggregator;
    
    /**
     * 批量任务超时阈值映射
     * Key: batchId, Value: 超时时间戳
     */
    private final Map<String, Long> batchTimeoutThresholds = new ConcurrentHashMap<>();
    
    /**
     * 批量任务警告状态
     * Key: batchId, Value: 警告标记
     */
    private final Map<String, Boolean> batchWarningFlags = new ConcurrentHashMap<>();
    
    /**
     * 统计计数器
     */
    private final AtomicLong totalTrackedBatches = new AtomicLong(0);
    private final AtomicLong totalTimeoutBatches = new AtomicLong(0);
    private final AtomicLong totalCompletedBatches = new AtomicLong(0);
    
    // 配置参数
    private static final long DEFAULT_TIMEOUT_MS = 1800000; // 30分钟默认超时
    private static final long WARNING_THRESHOLD_MS = 900000; // 15分钟警告阈值
    private static final int CLEANUP_RETENTION_HOURS = 24; // 数据保留24小时
    
    /**
     * 开始跟踪批量任务
     * 
     * @param batchId 批量任务ID
     * @param timeoutMs 超时时间（毫秒），null表示使用默认值
     */
    public void startTracking(String batchId, Long timeoutMs) {
        try {
            long timeout = timeoutMs != null ? timeoutMs : DEFAULT_TIMEOUT_MS;
            long timeoutThreshold = System.currentTimeMillis() + timeout;
            
            batchTimeoutThresholds.put(batchId, timeoutThreshold);
            batchWarningFlags.put(batchId, false);
            totalTrackedBatches.incrementAndGet();
            
            log.info("开始跟踪批量任务 - 批量ID: {}, 超时时间: {}分钟", 
                    batchId, timeout / 60000);
                    
        } catch (Exception e) {
            log.error("开始跟踪批量任务失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 停止跟踪批量任务
     * 
     * @param batchId 批量任务ID
     * @param completed 是否成功完成
     */
    public void stopTracking(String batchId, boolean completed) {
        try {
            batchTimeoutThresholds.remove(batchId);
            batchWarningFlags.remove(batchId);
            
            if (completed) {
                totalCompletedBatches.incrementAndGet();
            }
            
            log.debug("停止跟踪批量任务 - 批量ID: {}, 完成状态: {}", batchId, completed);
            
        } catch (Exception e) {
            log.error("停止跟踪批量任务失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 更新批量任务进度
     * 刷新超时时间戳
     * 
     * @param batchId 批量任务ID
     */
    public void updateProgress(String batchId) {
        try {
            Long currentTimeout = batchTimeoutThresholds.get(batchId);
            if (currentTimeout != null) {
                // 重置超时时间
                long newTimeout = System.currentTimeMillis() + DEFAULT_TIMEOUT_MS;
                batchTimeoutThresholds.put(batchId, newTimeout);
                
                // 重置警告标记
                batchWarningFlags.put(batchId, false);
                
                log.debug("更新批量任务进度跟踪 - 批量ID: {}", batchId);
            }
            
        } catch (Exception e) {
            log.error("更新批量任务进度跟踪失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取跟踪统计信息
     * 
     * @return 统计信息
     */
    public BatchTrackingStats getTrackingStats() {
        return BatchTrackingStats.builder()
                .totalTrackedBatches(totalTrackedBatches.get())
                .totalTimeoutBatches(totalTimeoutBatches.get())
                .totalCompletedBatches(totalCompletedBatches.get())
                .activeBatchCount(batchTimeoutThresholds.size())
                .warningBatchCount((int) batchWarningFlags.values().stream().mapToLong(flag -> flag ? 1 : 0).sum())
                .build();
    }
    
    /**
     * 获取当前跟踪的批量任务ID集合
     * 
     * @return 批量任务ID集合
     */
    public Set<String> getTrackedBatchIds() {
        return Set.copyOf(batchTimeoutThresholds.keySet());
    }
    
    /**
     * 检查批量任务是否超时
     * 
     * @param batchId 批量任务ID
     * @return true表示超时，false表示未超时
     */
    public boolean isBatchTimeout(String batchId) {
        Long timeoutThreshold = batchTimeoutThresholds.get(batchId);
        return timeoutThreshold != null && System.currentTimeMillis() > timeoutThreshold;
    }
    
    /**
     * 检查批量任务是否接近超时
     * 
     * @param batchId 批量任务ID
     * @return true表示接近超时，false表示未接近超时
     */
    public boolean isBatchNearTimeout(String batchId) {
        Long timeoutThreshold = batchTimeoutThresholds.get(batchId);
        if (timeoutThreshold == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long warningTime = timeoutThreshold - WARNING_THRESHOLD_MS;
        
        return currentTime > warningTime && currentTime <= timeoutThreshold;
    }
    
    // ==================== 定时任务 ====================
    
    /**
     * 定期检查批量任务超时
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 1分钟
    public void checkBatchTimeouts() {
        try {
            long currentTime = System.currentTimeMillis();
            List<String> timeoutBatches = batchTimeoutThresholds.entrySet().stream()
                    .filter(entry -> currentTime > entry.getValue())
                    .map(Map.Entry::getKey)
                    .toList();
            
            for (String batchId : timeoutBatches) {
                handleBatchTimeout(batchId);
            }
            
            if (!timeoutBatches.isEmpty()) {
                log.warn("检测到批量任务超时 - 超时任务数: {}", timeoutBatches.size());
            } else {
                log.debug("批量任务超时检查完成 - 跟踪任务数: {}", batchTimeoutThresholds.size());
            }
            
        } catch (Exception e) {
            log.error("检查批量任务超时失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期检查批量任务警告
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void checkBatchWarnings() {
        try {
            long currentTime = System.currentTimeMillis();
            int warningCount = 0;
            
            for (Map.Entry<String, Long> entry : batchTimeoutThresholds.entrySet()) {
                String batchId = entry.getKey();
                Long timeoutThreshold = entry.getValue();
                
                // 检查是否接近超时且未发出警告
                long warningTime = timeoutThreshold - WARNING_THRESHOLD_MS;
                if (currentTime > warningTime && !batchWarningFlags.getOrDefault(batchId, false)) {
                    handleBatchWarning(batchId);
                    batchWarningFlags.put(batchId, true);
                    warningCount++;
                }
            }
            
            if (warningCount > 0) {
                log.warn("检测到批量任务即将超时 - 警告任务数: {}", warningCount);
            }
            
        } catch (Exception e) {
            log.error("检查批量任务警告失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期清理过期数据
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void cleanupExpiredData() {
        try {
            log.info("开始清理过期批量任务跟踪数据");
            
            // 清理聚合器中的过期数据
            batchCommandAggregator.cleanupCompletedBatches(CLEANUP_RETENTION_HOURS);
            
            // 清理跟踪器中的过期数据
            int cleanedCount = cleanupLocalExpiredData();
            
            log.info("✅ 清理过期批量任务跟踪数据完成 - 清理数量: {}", cleanedCount);
            
        } catch (Exception e) {
            log.error("清理过期批量任务跟踪数据失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期输出统计信息
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    public void reportTrackingStats() {
        try {
            BatchTrackingStats stats = getTrackingStats();
            
            log.info("📊 批量任务跟踪统计 - 总跟踪: {}, 活跃: {}, 完成: {}, 超时: {}, 警告: {}",
                    stats.getTotalTrackedBatches(),
                    stats.getActiveBatchCount(),
                    stats.getTotalCompletedBatches(),
                    stats.getTotalTimeoutBatches(),
                    stats.getWarningBatchCount());
                    
        } catch (Exception e) {
            log.error("输出批量任务跟踪统计失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 处理批量任务超时
     */
    private void handleBatchTimeout(String batchId) {
        try {
            log.warn("⏰ 处理批量任务超时 - 批量ID: {}", batchId);
            
            // 通知聚合器任务超时
            batchCommandAggregator.aggregateStatusChange(
                    batchId, 
                    BatchCommandAggregationData.BatchStatus.TIMEOUT, 
                    Map.of("timeoutReason", "EXECUTION_TIMEOUT", "timeoutTime", LocalDateTime.now())
            );
            
            // 停止跟踪
            stopTracking(batchId, false);
            totalTimeoutBatches.incrementAndGet();
            
            log.warn("✅ 批量任务超时处理完成 - 批量ID: {}", batchId);
            
        } catch (Exception e) {
            log.error("处理批量任务超时失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 处理批量任务警告
     */
    private void handleBatchWarning(String batchId) {
        try {
            log.warn("⚠️ 处理批量任务即将超时警告 - 批量ID: {}", batchId);
            
            // 获取聚合数据
            batchCommandAggregator.getAggregationData(batchId).ifPresent(aggregationData -> {
                // 可以在这里发送警告通知
                log.warn("批量任务即将超时 - 批量ID: {}, 当前进度: {}/{}",
                        batchId,
                        aggregationData.getCompletedCount(),
                        aggregationData.getTotalCount());
            });
            
        } catch (Exception e) {
            log.error("处理批量任务警告失败 - 批量ID: {}, 错误: {}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 清理本地过期数据
     */
    private int cleanupLocalExpiredData() {
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime - (CLEANUP_RETENTION_HOURS * 3600000L);
        
        List<String> toRemove = batchTimeoutThresholds.entrySet().stream()
                .filter(entry -> entry.getValue() < expireTime)
                .map(Map.Entry::getKey)
                .toList();
        
        for (String batchId : toRemove) {
            batchTimeoutThresholds.remove(batchId);
            batchWarningFlags.remove(batchId);
        }
        
        return toRemove.size();
    }
    
    /**
     * 批量跟踪统计数据
     */
    public static class BatchTrackingStats {
        private long totalTrackedBatches;
        private long totalTimeoutBatches;
        private long totalCompletedBatches;
        private int activeBatchCount;
        private int warningBatchCount;
        
        public static BatchTrackingStatsBuilder builder() {
            return new BatchTrackingStatsBuilder();
        }
        
        // Getters
        public long getTotalTrackedBatches() { return totalTrackedBatches; }
        public long getTotalTimeoutBatches() { return totalTimeoutBatches; }
        public long getTotalCompletedBatches() { return totalCompletedBatches; }
        public int getActiveBatchCount() { return activeBatchCount; }
        public int getWarningBatchCount() { return warningBatchCount; }
        
        // Builder
        public static class BatchTrackingStatsBuilder {
            private BatchTrackingStats stats = new BatchTrackingStats();
            
            public BatchTrackingStatsBuilder totalTrackedBatches(long total) {
                stats.totalTrackedBatches = total;
                return this;
            }
            
            public BatchTrackingStatsBuilder totalTimeoutBatches(long timeout) {
                stats.totalTimeoutBatches = timeout;
                return this;
            }
            
            public BatchTrackingStatsBuilder totalCompletedBatches(long completed) {
                stats.totalCompletedBatches = completed;
                return this;
            }
            
            public BatchTrackingStatsBuilder activeBatchCount(int active) {
                stats.activeBatchCount = active;
                return this;
            }
            
            public BatchTrackingStatsBuilder warningBatchCount(int warning) {
                stats.warningBatchCount = warning;
                return this;
            }
            
            public BatchTrackingStats build() {
                return stats;
            }
        }
    }
}