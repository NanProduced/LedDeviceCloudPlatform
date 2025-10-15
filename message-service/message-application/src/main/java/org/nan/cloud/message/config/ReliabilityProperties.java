package org.nan.cloud.message.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * STOMP可靠性功能配置属性
 * 
 * 对应配置前缀：message.reliability
 * 
 * @author Nan
 * @since 3.1.0
 */
@Data
@ConfigurationProperties(prefix = "message.reliability")
public class ReliabilityProperties {
    
    /**
     * 是否启用可靠性功能
     */
    private boolean enabled = true;
    
    /**
     * 自动清理配置
     */
    private Cleanup cleanup = new Cleanup();
    
    /**
     * 监控统计配置
     */
    private Monitoring monitoring = new Monitoring();
    
    /**
     * 重试配置
     */
    private Retry retry = new Retry();
    
    @Data
    public static class Cleanup {
        /**
         * 是否启用自动清理
         */
        private boolean enabled = true;
        
        /**
         * 投递记录保留时间（小时）
         */
        private int deliveryRecordRetentionHours = 24;
        
        /**
         * 连接状态超时时间（分钟）
         */
        private int connectionTimeoutMinutes = 60;
    }
    
    @Data
    public static class Monitoring {
        /**
         * 是否启用监控统计
         */
        private boolean enabled = true;
        
        /**
         * 统计输出间隔（分钟）
         */
        private int statsIntervalMinutes = 10;
    }
    
    @Data
    public static class Retry {
        /**
         * 默认最大重试次数
         */
        private int defaultMaxRetries = 3;
        
        /**
         * 默认初始延迟（秒）
         */
        private long defaultInitialDelaySeconds = 5;
        
        /**
         * 默认最大延迟（秒）
         */
        private long defaultMaxDelaySeconds = 300;
    }
}