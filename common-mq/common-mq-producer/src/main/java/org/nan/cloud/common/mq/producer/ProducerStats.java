package org.nan.cloud.common.mq.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 生产者统计信息
 * 
 * 记录消息生产者的运行统计数据。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducerStats {
    
    /**
     * 总发送消息数
     */
    private long totalSentCount;
    
    /**
     * 成功发送消息数
     */
    private long successSentCount;
    
    /**
     * 失败发送消息数
     */
    private long failedSentCount;
    
    /**
     * 平均发送耗时（毫秒）
     */
    private double averageSendDuration;
    
    /**
     * 最大发送耗时（毫秒）
     */
    private long maxSendDuration;
    
    /**
     * 最小发送耗时（毫秒）
     */
    private long minSendDuration;
    
    /**
     * 当前发送速率（消息/秒）
     */
    private double currentSendRate;
    
    /**
     * 总重试次数
     */
    private long totalRetryCount;
    
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
        if (totalSentCount == 0) {
            return 0.0;
        }
        return (double) successSentCount / totalSentCount * 100.0;
    }
    
    /**
     * 获取失败率
     */
    public double getFailureRate() {
        if (totalSentCount == 0) {
            return 0.0;
        }
        return (double) failedSentCount / totalSentCount * 100.0;
    }
}