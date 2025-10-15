package org.nan.cloud.file.application.service;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 性能监控服务接口
 * 
 * 提供File Service的comprehensive performance monitoring：
 * 1. 实时性能指标收集 - 响应时间、吞吐量、错误率
 * 2. 健康状况评估 - 基于Backend专家99.9%可靠性要求
 * 3. 告警阈值监控 - 自动检测性能瓶颈和异常
 * 4. 性能趋势分析 - 历史数据分析和预测
 * 5. 资源使用优化建议 - 基于性能数据的优化建议
 * 
 * Backend可靠性KPI：
 * - 可用性目标: 99.9% (8.76h/年停机时间)
 * - 响应时间目标: <200ms (P95)
 * - 内存使用限制: <500MB
 * - 缓存命中率目标: >85%
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface PerformanceMonitoringService {
    
    /**
     * 获取当前性能快照
     * 实时性能指标，用于监控面板显示
     * 
     * @return 性能快照
     */
    PerformanceSnapshot getCurrentPerformance();
    
    /**
     * 获取健康状况报告
     * 综合评估系统健康状况，包含告警信息
     * 
     * @return 健康状况报告
     */
    HealthReport getHealthReport();
    
    /**
     * 获取性能趋势
     * 指定时间范围内的性能变化趋势
     * 
     * @param hours 过去小时数
     * @return 性能趋势数据
     */
    PerformanceTrend getPerformanceTrend(int hours);
    
    /**
     * 记录操作性能
     * 业务操作性能记录，用于统计分析
     * 
     * @param operation 操作类型
     * @param duration 执行时长（毫秒）
     * @param success 是否成功
     */
    void recordOperation(String operation, long duration, boolean success);
    
    /**
     * 记录缓存性能
     * 缓存命中/未命中记录
     * 
     * @param cacheType 缓存类型
     * @param hit 是否命中
     * @param responseTime 响应时间（毫秒）
     */
    void recordCachePerformance(String cacheType, boolean hit, long responseTime);
    
    /**
     * 记录文件传输性能
     * 文件上传/下载性能记录
     * 
     * @param transferType 传输类型（upload/download）
     * @param fileSize 文件大小（字节）
     * @param duration 传输时长（毫秒）
     * @param success 是否成功
     */
    void recordFileTransfer(String transferType, long fileSize, long duration, boolean success);
    
    /**
     * 获取性能优化建议
     * 基于当前性能数据分析，提供优化建议
     * 
     * @return 优化建议列表
     */
    List<OptimizationSuggestion> getOptimizationSuggestions();
    
    /**
     * 检查告警条件
     * 检查当前指标是否触发告警阈值
     * 
     * @return 活跃告警列表
     */
    List<Alert> checkAlerts();
    
    /**
     * 导出性能报告
     * 生成详细的性能分析报告
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 性能报告
     */
    PerformanceReport generateReport(LocalDateTime startTime, LocalDateTime endTime);
    
    // ==================== 数据模型 ====================
    
    /**
     * 性能快照
     */
    @Data
    @Builder
    class PerformanceSnapshot {
        private LocalDateTime timestamp;
        
        // 响应时间指标
        private double avgResponseTime;
        private double p95ResponseTime;
        private double p99ResponseTime;
        
        // 吞吐量指标
        private double requestsPerSecond;
        private double throughputMBps;
        
        // 缓存指标
        private double cacheHitRate;
        private long cacheSize;
        private double cacheResponseTime;
        
        // 系统资源指标
        private double cpuUsage;
        private long memoryUsed;
        private long memoryMax;
        private double diskUsage;
        
        // 并发指标
        private int activeConnections;
        private int activeUploads;
        private int activeDownloads;
        private int queuedTasks;
        
        // 错误率指标
        private double errorRate;
        private long totalErrors;
        
        // 文件处理指标
        private long totalFilesProcessed;
        private double avgFileSize;
        private double thumbnailGenerationRate;
    }
    
    /**
     * 健康状况报告
     */
    @Data
    @Builder
    class HealthReport {
        private HealthStatus overallStatus;
        private double healthScore; // 0-100分
        private LocalDateTime lastCheck;
        
        // 各组件健康状况
        private ComponentHealth cacheHealth;
        private ComponentHealth fileSystemHealth;
        private ComponentHealth databaseHealth;
        private ComponentHealth networkHealth;
        
        // 关键指标状态
        private IndicatorStatus responseTimeStatus;
        private IndicatorStatus memoryUsageStatus;
        private IndicatorStatus errorRateStatus;
        private IndicatorStatus cacheHitRateStatus;
        
        // 活跃告警
        private List<Alert> activeAlerts;
        
        // 性能瓶颈
        private List<String> bottlenecks;
    }
    
    /**
     * 性能趋势
     */
    @Data
    @Builder
    class PerformanceTrend {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        // 时间序列数据
        private Map<String, List<Double>> responseTimeTrend;
        private Map<String, List<Double>> throughputTrend;
        private Map<String, List<Double>> errorRateTrend;
        private Map<String, List<Double>> cacheHitRateTrend;
        private Map<String, List<Double>> memoryUsageTrend;
        
        // 趋势分析
        private TrendDirection responseTimeTrendDirection;
        private TrendDirection throughputTrendDirection;
        private TrendDirection errorRateTrendDirection;
        
        // 预测数据
        private Map<String, Double> predictedMetrics;
    }
    
    /**
     * 优化建议
     */
    @Data
    @Builder
    class OptimizationSuggestion {
        private String category; // 缓存、内存、线程池等
        private String title;
        private String description;
        private SuggestionPriority priority;
        private double expectedImprovement; // 预期改善百分比
        private String implementationSteps;
        private EstimatedImpact impact;
    }
    
    /**
     * 告警
     */
    @Data
    @Builder
    class Alert {
        private String alertId;
        private AlertSeverity severity;
        private String title;
        private String description;
        private LocalDateTime triggeredAt;
        private String metricName;
        private double currentValue;
        private double thresholdValue;
        private AlertStatus status;
        private String recommendedAction;
    }
    
    /**
     * 性能报告
     */
    @Data
    @Builder
    class PerformanceReport {
        private LocalDateTime generatedAt;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        // 汇总统计
        private Map<String, Double> summaryMetrics;
        private List<String> keyFindings;
        private List<String> performanceIssues;
        private List<OptimizationSuggestion> recommendations;
        
        // 详细数据
        private Map<String, Object> detailedMetrics;
        private List<PerformanceSnapshot> snapshots;
        
        // 比较分析
        private Map<String, Double> periodComparison;
    }
    
    // ==================== 枚举类型 ====================
    
    enum HealthStatus {
        HEALTHY, DEGRADED, UNHEALTHY, CRITICAL
    }
    
    enum ComponentHealth {
        HEALTHY, WARNING, ERROR, UNAVAILABLE
    }
    
    enum IndicatorStatus {
        GOOD, WARNING, CRITICAL
    }
    
    enum TrendDirection {
        IMPROVING, STABLE, DEGRADING, VOLATILE
    }
    
    enum SuggestionPriority {
        HIGH, MEDIUM, LOW
    }
    
    enum EstimatedImpact {
        SIGNIFICANT, MODERATE, MINIMAL
    }
    
    enum AlertSeverity {
        CRITICAL, WARNING, INFO
    }
    
    enum AlertStatus {
        ACTIVE, ACKNOWLEDGED, RESOLVED
    }
}