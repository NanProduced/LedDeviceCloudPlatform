package org.nan.cloud.message.infrastructure.routing.enhanced;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 路由策略管理器
 * 
 * 核心职责：
 * 1. 路由策略配置和管理
 * 2. 策略性能监控和自动调优
 * 3. 故障转移和恢复机制
 * 4. 路由策略的热更新
 * 
 * 策略管理：
 * - 策略注册：支持自定义路由策略
 * - 策略评估：基于性能指标评估策略效果
 * - 策略调优：自动调整策略参数
 * - 策略切换：支持热切换路由策略
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingStrategyManager {
    
    /**
     * 路由策略注册表
     * Key: 策略名称, Value: 策略配置
     */
    private final Map<String, RoutingStrategy> strategies = new ConcurrentHashMap<>();
    
    /**
     * 策略性能监控
     * Key: 策略名称, Value: 性能指标
     */
    private final Map<String, StrategyPerformance> strategyPerformances = new ConcurrentHashMap<>();
    
    /**
     * 活跃策略映射
     * Key: 消息类型+目标类型, Value: 当前使用的策略名称
     */
    private final Map<String, String> activeStrategies = new ConcurrentHashMap<>();
    
    /**
     * 故障转移配置
     * Key: 主策略名称, Value: 备用策略列表
     */
    private final Map<String, List<String>> failoverStrategies = new ConcurrentHashMap<>();
    
    /**
     * 策略使用统计
     */
    private final AtomicLong totalStrategyExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);
    
    /**
     * 初始化默认策略
     */
    @jakarta.annotation.PostConstruct
    public void initializeDefaultStrategies() {
        log.info("初始化默认路由策略");
        
        // 高优先级策略
        registerStrategy("high-priority", RoutingStrategy.builder()
                .name("high-priority")
                .description("高优先级消息快速路由策略")
                .maxLatencyMs(100)
                .loadBalancingType(DynamicRoutingEngine.LoadBalancingType.PRIORITY)
                .failoverEnabled(true)
                .retryCount(3)
                .healthCheckEnabled(true)
                .build());
        
        // 批量处理策略
        registerStrategy("batch-processing", RoutingStrategy.builder()
                .name("batch-processing")
                .description("批量消息聚合处理策略")
                .maxLatencyMs(5000)
                .loadBalancingType(DynamicRoutingEngine.LoadBalancingType.WEIGHTED)
                .batchSize(50)
                .timeWindowMs(3000)
                .aggregationEnabled(true)
                .build());
        
        // 广播策略
        registerStrategy("broadcast", RoutingStrategy.builder()
                .name("broadcast")
                .description("消息广播策略")
                .maxLatencyMs(1000)
                .loadBalancingType(DynamicRoutingEngine.LoadBalancingType.ROUND_ROBIN)
                .broadcastEnabled(true)
                .build());
        
        // 负载均衡策略
        registerStrategy("load-balanced", RoutingStrategy.builder()
                .name("load-balanced")
                .description("负载均衡路由策略")
                .maxLatencyMs(500)
                .loadBalancingType(DynamicRoutingEngine.LoadBalancingType.LEAST_CONNECTIONS)
                .healthCheckEnabled(true)
                .autoScalingEnabled(true)
                .build());
        
        // 设置默认活跃策略
        setActiveStrategy("ALERT", "high-priority");
        setActiveStrategy("NOTIFICATION", "load-balanced");
        setActiveStrategy("TASK_PROGRESS", "batch-processing");
        setActiveStrategy("TERMINAL_STATUS_CHANGE", "broadcast");
        
        // 配置故障转移
        configureFailover("high-priority", Arrays.asList("load-balanced", "broadcast"));
        configureFailover("batch-processing", Arrays.asList("load-balanced"));
        configureFailover("load-balanced", Arrays.asList("broadcast"));
        
        log.info("✅ 默认路由策略初始化完成 - 策略数: {}", strategies.size());
    }
    
    /**
     * 选择路由策略
     * 
     * @param message STOMP消息
     * @return 选中的路由策略
     */
    public RoutingStrategy selectStrategy(CommonStompMessage message) {
        try {
            totalStrategyExecutions.incrementAndGet();
            
            // 生成策略键
            String strategyKey = generateStrategyKey(message);
            
            // 获取活跃策略名称
            String strategyName = activeStrategies.get(strategyKey);
            if (strategyName == null) {
                strategyName = selectDefaultStrategy(message);
            }
            
            // 获取策略配置
            RoutingStrategy strategy = strategies.get(strategyName);
            if (strategy == null) {
                log.warn("策略不存在，使用默认策略 - 策略名: {}", strategyName);
                strategy = getDefaultStrategy();
            }
            
            // 检查策略健康状态
            if (!isStrategyHealthy(strategyName)) {
                log.warn("策略不健康，尝试故障转移 - 策略名: {}", strategyName);
                strategy = performFailover(strategyName, message);
            }
            
            // 记录策略使用
            recordStrategyUsage(strategy.getName(), true);
            successfulExecutions.incrementAndGet();
            
            log.debug("选择路由策略 - 消息ID: {}, 策略: {}", message.getMessageId(), strategy.getName());
            
            return strategy;
            
        } catch (Exception e) {
            log.error("选择路由策略失败 - 消息ID: {}, 错误: {}", message.getMessageId(), e.getMessage(), e);
            failedExecutions.incrementAndGet();
            return getDefaultStrategy();
        }
    }
    
    /**
     * 注册路由策略
     * 
     * @param name 策略名称
     * @param strategy 策略配置
     */
    public void registerStrategy(String name, RoutingStrategy strategy) {
        strategies.put(name, strategy);
        strategyPerformances.put(name, new StrategyPerformance(name));
        log.info("注册路由策略 - 策略名: {}, 描述: {}", name, strategy.getDescription());
    }
    
    /**
     * 注销路由策略
     * 
     * @param name 策略名称
     */
    public void unregisterStrategy(String name) {
        RoutingStrategy removed = strategies.remove(name);
        if (removed != null) {
            strategyPerformances.remove(name);
            log.info("注销路由策略 - 策略名: {}", name);
        }
    }
    
    /**
     * 设置活跃策略
     * 
     * @param messageType 消息类型
     * @param strategyName 策略名称
     */
    public void setActiveStrategy(String messageType, String strategyName) {
        activeStrategies.put(messageType, strategyName);
        log.info("设置活跃策略 - 消息类型: {}, 策略: {}", messageType, strategyName);
    }
    
    /**
     * 配置故障转移
     * 
     * @param primaryStrategy 主策略
     * @param backupStrategies 备用策略列表
     */
    public void configureFailover(String primaryStrategy, List<String> backupStrategies) {
        failoverStrategies.put(primaryStrategy, new ArrayList<>(backupStrategies));
        log.info("配置故障转移 - 主策略: {}, 备用策略: {}", primaryStrategy, backupStrategies);
    }
    
    /**
     * 获取路由策略统计
     * 
     * @return 策略统计信息
     */
    public StrategyStats getStrategyStats() {
        return StrategyStats.builder()
                .totalStrategyExecutions(totalStrategyExecutions.get())
                .successfulExecutions(successfulExecutions.get())
                .failedExecutions(failedExecutions.get())
                .totalStrategies(strategies.size())
                .activeStrategies(activeStrategies.size())
                .build();
    }
    
    /**
     * 定期策略性能评估和调优
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void evaluateAndOptimizeStrategies() {
        try {
            log.debug("开始策略性能评估和调优");
            
            for (Map.Entry<String, StrategyPerformance> entry : strategyPerformances.entrySet()) {
                String strategyName = entry.getKey();
                StrategyPerformance performance = entry.getValue();
                
                // 评估策略性能
                double successRate = performance.getSuccessRate();
                double avgLatency = performance.getAverageLatency();
                
                // 性能调优
                if (successRate < 0.95 && avgLatency > 1000) {
                    log.warn("策略性能不佳，考虑调优 - 策略: {}, 成功率: {:.2f}%, 平均延迟: {}ms", 
                            strategyName, successRate * 100, avgLatency);
                    
                    // 自动调优逻辑
                    optimizeStrategy(strategyName, performance);
                }
                
                // 重置性能统计
                performance.reset();
            }
            
        } catch (Exception e) {
            log.error("策略性能评估失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期健康检查
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void performHealthChecks() {
        try {
            for (String strategyName : strategies.keySet()) {
                boolean healthy = checkStrategyHealth(strategyName);
                StrategyPerformance performance = strategyPerformances.get(strategyName);
                if (performance != null) {
                    performance.updateHealthStatus(healthy);
                }
                
                if (!healthy) {
                    log.warn("策略健康检查失败 - 策略: {}", strategyName);
                }
            }
            
        } catch (Exception e) {
            log.error("策略健康检查失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 生成策略键
     */
    private String generateStrategyKey(CommonStompMessage message) {
        return message.getMessageType().name();
    }
    
    /**
     * 选择默认策略
     */
    private String selectDefaultStrategy(CommonStompMessage message) {
        // 基于消息特征选择默认策略
        if (message.getMetadata() != null && message.getMetadata().getPriority() != null) {
            switch (message.getMetadata().getPriority()) {
                case HIGH:
                    return "high-priority";
                case LOW:
                    return "batch-processing";
                default:
                    return "load-balanced";
            }
        }
        
        return "load-balanced";
    }
    
    /**
     * 获取默认策略
     */
    private RoutingStrategy getDefaultStrategy() {
        return strategies.getOrDefault("load-balanced", 
                RoutingStrategy.builder()
                        .name("default")
                        .description("默认路由策略")
                        .maxLatencyMs(1000)
                        .loadBalancingType(DynamicRoutingEngine.LoadBalancingType.ROUND_ROBIN)
                        .build());
    }
    
    /**
     * 检查策略是否健康
     */
    private boolean isStrategyHealthy(String strategyName) {
        StrategyPerformance performance = strategyPerformances.get(strategyName);
        return performance != null && performance.isHealthy();
    }
    
    /**
     * 执行故障转移
     */
    private RoutingStrategy performFailover(String failedStrategy, CommonStompMessage message) {
        List<String> backupStrategies = failoverStrategies.get(failedStrategy);
        if (backupStrategies == null || backupStrategies.isEmpty()) {
            log.warn("无备用策略可用 - 失败策略: {}", failedStrategy);
            return getDefaultStrategy();
        }
        
        // 尝试备用策略
        for (String backupStrategy : backupStrategies) {
            if (isStrategyHealthy(backupStrategy)) {
                RoutingStrategy strategy = strategies.get(backupStrategy);
                if (strategy != null) {
                    log.info("故障转移成功 - 从 {} 切换到 {}", failedStrategy, backupStrategy);
                    return strategy;
                }
            }
        }
        
        log.warn("所有备用策略都不可用 - 失败策略: {}", failedStrategy);
        return getDefaultStrategy();
    }
    
    /**
     * 记录策略使用
     */
    private void recordStrategyUsage(String strategyName, boolean success) {
        StrategyPerformance performance = strategyPerformances.get(strategyName);
        if (performance != null) {
            if (success) {
                performance.recordSuccess();
            } else {
                performance.recordFailure();
            }
        }
    }
    
    /**
     * 检查策略健康状态
     */
    private boolean checkStrategyHealth(String strategyName) {
        // 简单的健康检查逻辑
        StrategyPerformance performance = strategyPerformances.get(strategyName);
        if (performance == null) {
            return false;
        }
        
        // 检查成功率
        double successRate = performance.getSuccessRate();
        if (successRate < 0.9) {
            return false;
        }
        
        // 检查平均延迟
        double avgLatency = performance.getAverageLatency();
        RoutingStrategy strategy = strategies.get(strategyName);
        if (strategy != null && avgLatency > strategy.getMaxLatencyMs()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 优化策略
     */
    private void optimizeStrategy(String strategyName, StrategyPerformance performance) {
        RoutingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            return;
        }
        
        // 简单的自动调优逻辑
        if (performance.getAverageLatency() > strategy.getMaxLatencyMs()) {
            // 调整批量大小
            if (strategy.getBatchSize() > 10) {
                strategy.setBatchSize(strategy.getBatchSize() - 5);
                log.info("策略调优：减少批量大小 - 策略: {}, 新批量大小: {}", 
                        strategyName, strategy.getBatchSize());
            }
            
            // 调整时间窗口
            if (strategy.getTimeWindowMs() > 1000) {
                strategy.setTimeWindowMs(strategy.getTimeWindowMs() - 500);
                log.info("策略调优：减少时间窗口 - 策略: {}, 新时间窗口: {}ms", 
                        strategyName, strategy.getTimeWindowMs());
            }
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 路由策略配置
     */
    public static class RoutingStrategy {
        private String name;
        private String description;
        private long maxLatencyMs;
        private DynamicRoutingEngine.LoadBalancingType loadBalancingType;
        private boolean failoverEnabled;
        private int retryCount;
        private boolean healthCheckEnabled;
        private boolean aggregationEnabled;
        private boolean broadcastEnabled;
        private boolean autoScalingEnabled;
        private int batchSize;
        private long timeWindowMs;
        
        public static StrategyBuilder builder() {
            return new StrategyBuilder();
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public long getMaxLatencyMs() { return maxLatencyMs; }
        public void setMaxLatencyMs(long maxLatencyMs) { this.maxLatencyMs = maxLatencyMs; }
        public DynamicRoutingEngine.LoadBalancingType getLoadBalancingType() { return loadBalancingType; }
        public void setLoadBalancingType(DynamicRoutingEngine.LoadBalancingType loadBalancingType) { this.loadBalancingType = loadBalancingType; }
        public boolean isFailoverEnabled() { return failoverEnabled; }
        public void setFailoverEnabled(boolean failoverEnabled) { this.failoverEnabled = failoverEnabled; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        public boolean isHealthCheckEnabled() { return healthCheckEnabled; }
        public void setHealthCheckEnabled(boolean healthCheckEnabled) { this.healthCheckEnabled = healthCheckEnabled; }
        public boolean isAggregationEnabled() { return aggregationEnabled; }
        public void setAggregationEnabled(boolean aggregationEnabled) { this.aggregationEnabled = aggregationEnabled; }
        public boolean isBroadcastEnabled() { return broadcastEnabled; }
        public void setBroadcastEnabled(boolean broadcastEnabled) { this.broadcastEnabled = broadcastEnabled; }
        public boolean isAutoScalingEnabled() { return autoScalingEnabled; }
        public void setAutoScalingEnabled(boolean autoScalingEnabled) { this.autoScalingEnabled = autoScalingEnabled; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public long getTimeWindowMs() { return timeWindowMs; }
        public void setTimeWindowMs(long timeWindowMs) { this.timeWindowMs = timeWindowMs; }
        
        public static class StrategyBuilder {
            private RoutingStrategy strategy = new RoutingStrategy();
            
            public StrategyBuilder name(String name) { strategy.name = name; return this; }
            public StrategyBuilder description(String description) { strategy.description = description; return this; }
            public StrategyBuilder maxLatencyMs(long maxLatencyMs) { strategy.maxLatencyMs = maxLatencyMs; return this; }
            public StrategyBuilder loadBalancingType(DynamicRoutingEngine.LoadBalancingType type) { strategy.loadBalancingType = type; return this; }
            public StrategyBuilder failoverEnabled(boolean enabled) { strategy.failoverEnabled = enabled; return this; }
            public StrategyBuilder retryCount(int count) { strategy.retryCount = count; return this; }
            public StrategyBuilder healthCheckEnabled(boolean enabled) { strategy.healthCheckEnabled = enabled; return this; }
            public StrategyBuilder aggregationEnabled(boolean enabled) { strategy.aggregationEnabled = enabled; return this; }
            public StrategyBuilder broadcastEnabled(boolean enabled) { strategy.broadcastEnabled = enabled; return this; }
            public StrategyBuilder autoScalingEnabled(boolean enabled) { strategy.autoScalingEnabled = enabled; return this; }
            public StrategyBuilder batchSize(int size) { strategy.batchSize = size; return this; }
            public StrategyBuilder timeWindowMs(long timeMs) { strategy.timeWindowMs = timeMs; return this; }
            
            public RoutingStrategy build() { return strategy; }
        }
    }
    
    /**
     * 策略性能统计
     */
    public static class StrategyPerformance {
        private final String strategyName;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalLatency = new AtomicLong(0);
        private final AtomicLong executionCount = new AtomicLong(0);
        private volatile boolean healthy = true;
        private volatile long lastHealthCheckTime = System.currentTimeMillis();
        
        public StrategyPerformance(String strategyName) {
            this.strategyName = strategyName;
        }
        
        public void recordSuccess() {
            successCount.incrementAndGet();
            executionCount.incrementAndGet();
        }
        
        public void recordFailure() {
            failureCount.incrementAndGet();
            executionCount.incrementAndGet();
        }
        
        public void recordLatency(long latencyMs) {
            totalLatency.addAndGet(latencyMs);
        }
        
        public void updateHealthStatus(boolean healthy) {
            this.healthy = healthy;
            this.lastHealthCheckTime = System.currentTimeMillis();
        }
        
        public double getSuccessRate() {
            long total = executionCount.get();
            return total > 0 ? (double) successCount.get() / total : 1.0;
        }
        
        public double getAverageLatency() {
            long executions = executionCount.get();
            return executions > 0 ? (double) totalLatency.get() / executions : 0.0;
        }
        
        public void reset() {
            successCount.set(0);
            failureCount.set(0);
            totalLatency.set(0);
            executionCount.set(0);
        }
        
        // Getters
        public String getStrategyName() { return strategyName; }
        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        public long getExecutionCount() { return executionCount.get(); }
        public boolean isHealthy() { return healthy; }
        public long getLastHealthCheckTime() { return lastHealthCheckTime; }
    }
    
    /**
     * 策略统计
     */
    public static class StrategyStats {
        private long totalStrategyExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private int totalStrategies;
        private int activeStrategies;
        
        public static StrategyStatsBuilder builder() {
            return new StrategyStatsBuilder();
        }
        
        // Getters
        public long getTotalStrategyExecutions() { return totalStrategyExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public int getTotalStrategies() { return totalStrategies; }
        public int getActiveStrategies() { return activeStrategies; }
        public double getSuccessRate() {
            return totalStrategyExecutions > 0 ? (double) successfulExecutions / totalStrategyExecutions * 100 : 0;
        }
        
        public static class StrategyStatsBuilder {
            private StrategyStats stats = new StrategyStats();
            
            public StrategyStatsBuilder totalStrategyExecutions(long total) {
                stats.totalStrategyExecutions = total;
                return this;
            }
            
            public StrategyStatsBuilder successfulExecutions(long successful) {
                stats.successfulExecutions = successful;
                return this;
            }
            
            public StrategyStatsBuilder failedExecutions(long failed) {
                stats.failedExecutions = failed;
                return this;
            }
            
            public StrategyStatsBuilder totalStrategies(int total) {
                stats.totalStrategies = total;
                return this;
            }
            
            public StrategyStatsBuilder activeStrategies(int active) {
                stats.activeStrategies = active;
                return this;
            }
            
            public StrategyStats build() {
                return stats;
            }
        }
    }
}