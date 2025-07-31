package org.nan.cloud.message.infrastructure.websocket.reliability;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP消息重试配置
 * 
 * 提供不同消息类型的重试策略配置，支持：
 * 1. 差异化重试策略（不同消息类型不同的重试参数）
 * 2. 指数退避算法（避免重试风暴）
 * 3. 动态配置调整（运行时调整重试策略）
 * 4. 重试统计和监控
 * 
 * 内置策略：
 * - CRITICAL: 关键消息，最大重试5次，超时60秒
 * - NORMAL: 普通消息，最大重试3次，超时30秒
 * - LOW_PRIORITY: 低优先级消息，最大重试1次，超时15秒
 * - BATCH: 批量消息，最大重试2次，超时45秒
 * 
 * @author Nan
 * @since 3.1.0
 */
@Slf4j
@Component
public class RetryConfiguration {
    
    /**
     * 消息类型重试策略映射
     */
    private final Map<String, RetryPolicy> retryPolicies = new ConcurrentHashMap<>();
    
    /**
     * 默认重试策略
     */
    private final RetryPolicy defaultPolicy;
    
    public RetryConfiguration() {
        // 初始化默认策略
        this.defaultPolicy = createDefaultRetryPolicy();
        
        // 初始化预定义策略
        initializePredefinedPolicies();
        
        log.info("🔧 STOMP消息重试配置初始化完成");
        log.info("📊 预定义策略数量: {}", retryPolicies.size());
    }
    
    /**
     * 创建默认重试策略
     */
    private RetryPolicy createDefaultRetryPolicy() {
        return RetryPolicy.builder()
            .maxRetries(3)
            .initialDelaySeconds(5)
            .maxDelaySeconds(300)
            .backoffMultiplier(2.0)
            .timeoutSeconds(30)
            .retryOnTimeout(true)
            .retryOnReject(true)
            .build();
    }
    
    /**
     * 初始化预定义重试策略
     */
    private void initializePredefinedPolicies() {
        // 关键消息策略
        retryPolicies.put("CRITICAL", RetryPolicy.builder()
            .maxRetries(5)
            .initialDelaySeconds(3)
            .maxDelaySeconds(600)
            .backoffMultiplier(1.5)
            .timeoutSeconds(60)
            .retryOnTimeout(true)
            .retryOnReject(true)
            .build());
        
        // 普通消息策略
        retryPolicies.put("NORMAL", RetryPolicy.builder()
            .maxRetries(3)
            .initialDelaySeconds(5)
            .maxDelaySeconds(300)
            .backoffMultiplier(2.0)
            .timeoutSeconds(30)
            .retryOnTimeout(true)
            .retryOnReject(true)
            .build());
        
        // 低优先级消息策略
        retryPolicies.put("LOW_PRIORITY", RetryPolicy.builder()
            .maxRetries(1)
            .initialDelaySeconds(10)
            .maxDelaySeconds(60)
            .backoffMultiplier(1.0)
            .timeoutSeconds(15)
            .retryOnTimeout(false)
            .retryOnReject(false)
            .build());
        
        // 批量消息策略
        retryPolicies.put("BATCH", RetryPolicy.builder()
            .maxRetries(2)
            .initialDelaySeconds(8)
            .maxDelaySeconds(240)
            .backoffMultiplier(2.5)
            .timeoutSeconds(45)
            .retryOnTimeout(true)
            .retryOnReject(true)
            .build());
        
        // 系统通知策略
        retryPolicies.put("SYSTEM_NOTIFICATION", RetryPolicy.builder()
            .maxRetries(4)
            .initialDelaySeconds(2)
            .maxDelaySeconds(120)
            .backoffMultiplier(1.8)
            .timeoutSeconds(25)
            .retryOnTimeout(true)
            .retryOnReject(true)
            .build());
        
        // 设备指令策略
        retryPolicies.put("DEVICE_COMMAND", RetryPolicy.builder()
            .maxRetries(3)
            .initialDelaySeconds(5)
            .maxDelaySeconds(180)
            .backoffMultiplier(2.2)
            .timeoutSeconds(40)
            .retryOnTimeout(true)
            .retryOnReject(true)
            .build());
        
        // 状态更新策略
        retryPolicies.put("STATUS_UPDATE", RetryPolicy.builder()
            .maxRetries(2)
            .initialDelaySeconds(3)
            .maxDelaySeconds(90)
            .backoffMultiplier(2.0)
            .timeoutSeconds(20)
            .retryOnTimeout(true)
            .retryOnReject(false)
            .build());
    }
    
    /**
     * 获取指定消息类型的重试策略
     */
    public RetryPolicy getRetryPolicy(String messageType) {
        if (messageType == null || messageType.trim().isEmpty()) {
            return defaultPolicy;
        }
        
        return retryPolicies.getOrDefault(messageType.toUpperCase(), defaultPolicy);
    }
    
    /**
     * 设置自定义重试策略
     */
    public void setRetryPolicy(String messageType, RetryPolicy policy) {
        if (messageType == null || policy == null) {
            throw new IllegalArgumentException("消息类型和重试策略不能为空");
        }
        
        retryPolicies.put(messageType.toUpperCase(), policy);
        log.info("🔧 更新重试策略: messageType={}, policy={}", messageType, policy);
    }
    
    /**
     * 移除自定义重试策略
     */
    public void removeRetryPolicy(String messageType) {
        if (messageType != null) {
            RetryPolicy removed = retryPolicies.remove(messageType.toUpperCase());
            if (removed != null) {
                log.info("🗑️ 移除重试策略: messageType={}", messageType);
            }
        }
    }
    
    /**
     * 获取所有重试策略
     */
    public Map<String, RetryPolicy> getAllRetryPolicies() {
        return Map.copyOf(retryPolicies);
    }
    
    /**
     * 获取默认重试策略
     */
    public RetryPolicy getDefaultPolicy() {
        return defaultPolicy;
    }
    
    /**
     * 重试策略模型
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RetryPolicy {
        /**
         * 最大重试次数
         */
        private int maxRetries;
        
        /**
         * 初始延迟时间（秒）
         */
        private long initialDelaySeconds;
        
        /**
         * 最大延迟时间（秒）
         */
        private long maxDelaySeconds;
        
        /**
         * 退避倍数（指数退避算法的倍数）
         */
        private double backoffMultiplier;
        
        /**
         * 消息超时时间（秒）
         */
        private long timeoutSeconds;
        
        /**
         * 是否在超时时重试
         */
        private boolean retryOnTimeout;
        
        /**
         * 是否在拒绝时重试
         */
        private boolean retryOnReject;
        
        /**
         * 计算指定重试次数的延迟时间
         * 
         * @param attemptNumber 重试次数（从0开始）
         * @return 延迟时间（秒）
         */
        public long calculateDelay(int attemptNumber) {
            if (attemptNumber <= 0) {
                return initialDelaySeconds;
            }
            
            // 指数退避算法：delay = initial * (multiplier ^ attempts)
            double delay = initialDelaySeconds * Math.pow(backoffMultiplier, attemptNumber);
            
            // 限制最大延迟时间
            return Math.min((long) delay, maxDelaySeconds);
        }
        
        /**
         * 判断是否应该重试
         * 
         * @param attemptNumber 当前重试次数
         * @param failureReason 失败原因
         * @return 是否应该重试
         */
        public boolean shouldRetry(int attemptNumber, String failureReason) {
            // 检查重试次数限制
            if (attemptNumber >= maxRetries) {
                return false;
            }
            
            // 根据失败原因判断是否重试
            if ("TIMEOUT".equalsIgnoreCase(failureReason)) {
                return retryOnTimeout;
            } else if ("REJECT".equalsIgnoreCase(failureReason)) {
                return retryOnReject;
            }
            
            // 其他情况默认重试
            return true;
        }
        
        @Override
        public String toString() {
            return String.format("RetryPolicy{maxRetries=%d, initialDelay=%ds, maxDelay=%ds, " +
                    "backoffMultiplier=%.1f, timeout=%ds, retryOnTimeout=%s, retryOnReject=%s}",
                    maxRetries, initialDelaySeconds, maxDelaySeconds, backoffMultiplier,
                    timeoutSeconds, retryOnTimeout, retryOnReject);
        }
    }
}