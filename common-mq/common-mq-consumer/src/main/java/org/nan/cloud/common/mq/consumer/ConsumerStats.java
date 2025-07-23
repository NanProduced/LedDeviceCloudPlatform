package org.nan.cloud.common.mq.consumer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消费者统计信息
 * 
 * 记录消息消费者的运行统计数据。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerStats {
    
    /**
     * 总消费消息数
     */
    private long totalConsumedCount;
    
    /**
     * 成功消费消息数
     */
    private long successConsumedCount;
    
    /**
     * 失败消费消息数
     */
    private long failedConsumedCount;
    
    /**
     * 重试次数
     */
    private long retryCount;
    
    /**
     * 注册的消费者数量
     */
    private int registeredConsumerCount;
    
    /**
     * 平均消费耗时（毫秒）
     */
    private double averageConsumeDuration;
    
    /**
     * 最大消费耗时（毫秒）
     */
    private long maxConsumeDuration;
    
    /**
     * 最小消费耗时（毫秒）
     */
    private long minConsumeDuration;
    
    /**
     * 当前消费速率（消息/秒）
     */
    private double currentConsumeRate;
    
    /**
     * 统计开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalConsumedCount == 0) {
            return 0.0;
        }
        return (double) successConsumedCount / totalConsumedCount * 100.0;
    }
    
    /**
     * 获取失败率
     */
    public double getFailureRate() {
        if (totalConsumedCount == 0) {
            return 0.0;
        }
        return (double) failedConsumedCount / totalConsumedCount * 100.0;
    }
    
    /**
     * 获取重试率
     */
    public double getRetryRate() {
        if (totalConsumedCount == 0) {
            return 0.0;
        }
        return (double) retryCount / totalConsumedCount * 100.0;
    }
}