package org.nan.cloud.common.mq.consumer.config;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.mq.consumer.ConsumerStats;
import org.nan.cloud.common.mq.consumer.MessageConsumerManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息消费者健康检查指示器
 * 
 * 集成Spring Boot Actuator，提供消费者健康状态检查。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class MqConsumerHealthIndicator implements HealthIndicator {
    
    private final MessageConsumerManager consumerManager;
    
    @Override
    public Health health() {
        try {
            ConsumerStats stats = consumerManager.getStats();
            
            Health.Builder healthBuilder;
            
            // 根据失败率判断健康状态
            double failureRate = stats.getFailureRate();
            if (failureRate > 20.0) { // 失败率超过20%认为不健康
                healthBuilder = Health.down();
            } else if (failureRate > 10.0) { // 失败率超过10%认为降级
                healthBuilder = Health.status("DEGRADED");
            } else {
                healthBuilder = Health.up();
            }
            
            // 添加详细信息
            Map<String, Object> details = new HashMap<>();
            
            // 统计信息
            details.put("stats", Map.of(
                "totalConsumed", stats.getTotalConsumedCount(),
                "successConsumed", stats.getSuccessConsumedCount(),
                "failedConsumed", stats.getFailedConsumedCount(),
                "successRate", String.format("%.2f%%", stats.getSuccessRate()),
                "failureRate", String.format("%.2f%%", failureRate),
                "retryCount", stats.getRetryCount(),
                "retryRate", String.format("%.2f%%", stats.getRetryRate())
            ));
            
            // 消费者信息
            details.put("consumers", Map.of(
                "registeredCount", stats.getRegisteredConsumerCount(),
                "registeredConsumers", consumerManager.getRegisteredConsumers()
            ));
            
            // 运行时间信息
            details.put("runtime", Map.of(
                "startTime", stats.getStartTime(),
                "lastUpdateTime", stats.getLastUpdateTime()
            ));
            
            return healthBuilder.withDetails(details).build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "健康检查失败: " + e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }
}