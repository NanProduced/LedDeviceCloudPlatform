package org.nan.cloud.common.mq.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康状态
 * 
 * 表示消息生产者/消费者的健康状态信息。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatus {
    
    /**
     * 健康状态
     */
    private Status status;
    
    /**
     * 状态描述
     */
    private String description;
    
    /**
     * 检查时间
     */
    @Builder.Default
    private LocalDateTime checkTime = LocalDateTime.now();
    
    /**
     * 详细信息
     */
    private Map<String, Object> details;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 健康状态枚举
     */
    public enum Status {
        HEALTHY,     // 健康
        DEGRADED,    // 降级
        UNHEALTHY    // 不健康
    }
    
    /**
     * 创建健康状态
     */
    public static HealthStatus healthy(String description) {
        return HealthStatus.builder()
                .status(Status.HEALTHY)
                .description(description)
                .build();
    }
    
    /**
     * 创建降级状态
     */
    public static HealthStatus degraded(String description, Map<String, Object> details) {
        return HealthStatus.builder()
                .status(Status.DEGRADED)
                .description(description)
                .details(details)
                .build();
    }
    
    /**
     * 创建不健康状态
     */
    public static HealthStatus unhealthy(String description, String errorMessage) {
        return HealthStatus.builder()
                .status(Status.UNHEALTHY)
                .description(description)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 是否健康
     */
    public boolean isHealthy() {
        return status == Status.HEALTHY;
    }
    
    /**
     * 是否不健康
     */
    public boolean isUnhealthy() {
        return status == Status.UNHEALTHY;
    }
}