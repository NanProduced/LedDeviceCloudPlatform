package org.nan.cloud.common.mq.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量消息发送结果
 * 
 * 封装批量消息的发送结果信息。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSendResult {
    
    /**
     * 批次ID
     */
    private String batchId;
    
    /**
     * 总消息数量
     */
    private int totalCount;
    
    /**
     * 成功发送数量
     */
    private int successCount;
    
    /**
     * 失败发送数量
     */
    private int failureCount;
    
    /**
     * 发送时间
     */
    @Builder.Default
    private LocalDateTime sendTime = LocalDateTime.now();
    
    /**
     * 总耗时（毫秒）
     */
    private long totalDuration;
    
    /**
     * 平均耗时（毫秒）
     */
    private long averageDuration;
    
    /**
     * 各消息的发送结果
     */
    private List<SendResult> results;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 交换机名称
     */
    private String exchange;
    
    /**
     * 路由键
     */
    private String routingKey;
    
    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return failureCount == 0 && successCount == totalCount;
    }
    
    /**
     * 是否全部失败
     */
    public boolean isAllFailure() {
        return successCount == 0 && failureCount == totalCount;
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100.0;
    }
    
    /**
     * 获取失败率
     */
    public double getFailureRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) failureCount / totalCount * 100.0;
    }
    
    /**
     * 获取吞吐量（消息/秒）
     */
    public double getThroughput() {
        if (totalDuration == 0) {
            return 0.0;
        }
        return (double) totalCount / (totalDuration / 1000.0);
    }
    
    /**
     * 获取结果摘要
     */
    public String getSummary() {
        return String.format("批量发送结果: 总数=%d, 成功=%d, 失败=%d, 成功率=%.1f%%, 总耗时=%dms, 吞吐量=%.1f msg/s", 
                totalCount, successCount, failureCount, getSuccessRate(), totalDuration, getThroughput());
    }
    
    /**
     * 创建成功结果
     */
    public static BatchSendResult success(String batchId, int totalCount, long totalDuration, List<SendResult> results) {
        return BatchSendResult.builder()
                .batchId(batchId)
                .totalCount(totalCount)
                .successCount(totalCount)
                .failureCount(0)
                .totalDuration(totalDuration)
                .averageDuration(totalDuration / totalCount)
                .results(results)
                .build();
    }
    
    /**
     * 创建部分成功结果
     */
    public static BatchSendResult partial(String batchId, int totalCount, int successCount, 
                                        long totalDuration, List<SendResult> results) {
        return BatchSendResult.builder()
                .batchId(batchId)
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(totalCount - successCount)
                .totalDuration(totalDuration)
                .averageDuration(totalCount > 0 ? totalDuration / totalCount : 0)
                .results(results)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static BatchSendResult failure(String batchId, int totalCount, String errorMessage, long totalDuration) {
        return BatchSendResult.builder()
                .batchId(batchId)
                .totalCount(totalCount)
                .successCount(0)
                .failureCount(totalCount)
                .totalDuration(totalDuration)
                .averageDuration(totalCount > 0 ? totalDuration / totalCount : 0)
                .errorMessage(errorMessage)
                .build();
    }
}