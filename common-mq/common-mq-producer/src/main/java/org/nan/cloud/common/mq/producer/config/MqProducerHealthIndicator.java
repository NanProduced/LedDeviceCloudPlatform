package org.nan.cloud.common.mq.producer.config;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.mq.producer.HealthStatus;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.ProducerStats;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息生产者健康检查指示器
 * 
 * 集成Spring Boot Actuator，提供生产者健康状态检查。
 * 
 * @author LedDeviceCloudPlatform Team  
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class MqProducerHealthIndicator implements HealthIndicator {
    
    private final MessageProducer messageProducer;
    
    @Override
    public Health health() {
        try {
            HealthStatus healthStatus = messageProducer.getHealth();
            ProducerStats stats = messageProducer.getStats();
            
            Health.Builder healthBuilder;
            
            switch (healthStatus.getStatus()) {
                case HEALTHY:
                    healthBuilder = Health.up();
                    break;
                case DEGRADED:
                    healthBuilder = Health.status("DEGRADED");
                    break;
                case UNHEALTHY:
                default:
                    healthBuilder = Health.down();
                    break;
            }
            
            // 添加详细信息
            Map<String, Object> details = new HashMap<>();
            details.put("status", healthStatus.getStatus().name());
            details.put("description", healthStatus.getDescription());
            details.put("checkTime", healthStatus.getCheckTime());
            
            if (healthStatus.getErrorMessage() != null) {
                details.put("error", healthStatus.getErrorMessage());
            }
            
            if (healthStatus.getDetails() != null) {
                details.putAll(healthStatus.getDetails());
            }
            
            // 添加统计信息
            details.put("stats", Map.of(
                "totalSent", stats.getTotalSentCount(),
                "successSent", stats.getSuccessSentCount(),
                "failedSent", stats.getFailedSentCount(),
                "successRate", String.format("%.2f%%", stats.getSuccessRate()),
                "averageDuration", String.format("%.2fms", stats.getAverageSendDuration()),
                "totalRetries", stats.getTotalRetryCount()
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