package org.nan.cloud.file.application.service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 熔断器服务接口
 * 
 * 提供resilient system design的熔断降级机制：
 * 1. FFmpeg处理熔断 - 防止视频处理故障影响系统
 * 2. 缓存降级策略 - 缓存不可用时的降级处理
 * 3. 存储降级机制 - 主存储故障时的备用策略
 * 4. 自动健康检查 - 定期检测服务健康状况
 * 5. 智能恢复机制 - 基于成功率的自动恢复
 * 
 * Backend可靠性设计：
 * - 故障隔离：防止单个组件故障导致系统崩溃
 * - 快速失败：减少故障响应时间到<100ms
 * - 自动恢复：基于健康检查的智能恢复
 * - 降级策略：确保核心功能在故障时仍可用
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface CircuitBreakerService {
    
    /**
     * 执行带熔断保护的操作
     * 
     * @param serviceId 服务标识
     * @param operation 执行的操作
     * @param fallback 降级操作
     * @return 操作结果
     */
    <T> T executeWithCircuitBreaker(String serviceId, Supplier<T> operation, Supplier<T> fallback);
    
    /**
     * 异步执行带熔断保护的操作
     * 
     * @param serviceId 服务标识
     * @param operation 执行的操作
     * @param fallback 降级操作
     * @return CompletableFuture包装的操作结果
     */
    <T> CompletableFuture<T> executeAsyncWithCircuitBreaker(String serviceId, 
                                                          Supplier<CompletableFuture<T>> operation, 
                                                          Supplier<CompletableFuture<T>> fallback);
    
    /**
     * FFmpeg视频处理熔断
     * 专门针对FFmpeg视频处理的熔断机制
     * 
     * @param operation FFmpeg处理操作
     * @param fallback 降级策略（如使用默认缩略图）
     * @return 处理结果
     */
    <T> T executeFFmpegWithCircuitBreaker(Supplier<T> operation, Supplier<T> fallback);
    
    /**
     * 缓存访问熔断
     * 缓存服务不可用时的降级处理
     * 
     * @param cacheOperation 缓存操作
     * @param dataSourceOperation 数据源操作
     * @return 数据结果
     */
    <T> T executeCacheWithCircuitBreaker(Supplier<T> cacheOperation, Supplier<T> dataSourceOperation);
    
    /**
     * 存储访问熔断
     * 存储服务故障时的降级处理
     * 
     * @param storageOperation 存储操作
     * @param backupOperation 备用存储操作
     * @return 存储结果
     */
    <T> T executeStorageWithCircuitBreaker(Supplier<T> storageOperation, Supplier<T> backupOperation);
    
    /**
     * 获取熔断器状态
     * 
     * @param serviceId 服务标识
     * @return 熔断器状态
     */
    CircuitBreakerState getCircuitBreakerState(String serviceId);
    
    /**
     * 手动触发熔断器开启
     * 用于紧急故障处理
     * 
     * @param serviceId 服务标识
     * @param reason 触发原因
     */
    void tripCircuitBreaker(String serviceId, String reason);
    
    /**
     * 手动重置熔断器
     * 
     * @param serviceId 服务标识
     */
    void resetCircuitBreaker(String serviceId);
    
    /**
     * 获取熔断器统计信息
     * 
     * @param serviceId 服务标识
     * @return 统计信息
     */
    CircuitBreakerStatistics getStatistics(String serviceId);
    
    /**
     * 配置熔断器参数
     * 
     * @param serviceId 服务标识
     * @param config 配置参数
     */
    void configureCircuitBreaker(String serviceId, CircuitBreakerConfig config);
    
    /**
     * 获取所有熔断器健康状况
     * 
     * @return 健康状况报告
     */
    CircuitBreakerHealthReport getHealthReport();
    
    // ==================== 数据模型 ====================
    
    /**
     * 熔断器状态
     */
    enum CircuitBreakerState {
        CLOSED,     // 关闭状态：正常工作
        OPEN,       // 开启状态：熔断生效，直接降级
        HALF_OPEN   // 半开状态：探测性恢复
    }
    
    /**
     * 熔断器配置
     */
    interface CircuitBreakerConfig {
        int getFailureThreshold();           // 失败阈值
        Duration getTimeout();               // 超时时间
        Duration getRetryAfter();           // 重试间隔
        double getFailureRateThreshold();   // 失败率阈值
        int getMinimumRequests();           // 最小请求数
        Duration getSlidingWindowSize();    // 滑动窗口大小
    }
    
    /**
     * 熔断器统计信息
     */
    interface CircuitBreakerStatistics {
        CircuitBreakerState getState();
        long getTotalRequests();
        long getSuccessfulRequests();
        long getFailedRequests();
        double getFailureRate();
        Duration getAverageResponseTime();
        long getCircuitBreakerOpenCount();
        java.time.LocalDateTime getLastStateChange();
    }
    
    /**
     * 熔断器健康报告
     */
    interface CircuitBreakerHealthReport {
        java.util.Map<String, CircuitBreakerState> getServiceStates();
        java.util.List<String> getUnhealthyServices();
        double getOverallHealthScore();
        java.util.Map<String, String> getRecommendations();
    }
}