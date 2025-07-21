package org.nan.cloud.message.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户上线推送结果DTO
 * 
 * 封装用户上线时消息推送的详细结果信息，包括推送的消息数量、
 * 任务结果数量、推送状态等统计信息。
 * 
 * 主要用途：
 * - 推送结果的统一返回格式
 * - 推送性能监控和分析
 * - 异步推送的结果查询
 * - 推送日志和审计
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
public class UserOnlinePushResult {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 推送是否成功
     */
    private Boolean success;
    
    /**
     * 推送开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 推送完成时间
     */
    private LocalDateTime endTime;
    
    /**
     * 推送总耗时（毫秒）
     */
    private Long durationMs;
    
    // ==================== 消息推送统计 ====================
    
    /**
     * 推送的未读消息数量
     */
    private Integer pushedMessageCount;
    
    /**
     * 推送的高优先级消息数量
     */
    private Integer pushedHighPriorityMessageCount;
    
    /**
     * 推送失败的消息数量
     */
    private Integer failedMessageCount;
    
    /**
     * 推送的消息ID列表
     */
    private List<String> pushedMessageIds;
    
    /**
     * 推送失败的消息ID列表
     */
    private List<String> failedMessageIds;
    
    // ==================== 任务结果推送统计 ====================
    
    /**
     * 推送的未查看任务结果数量
     */
    private Integer pushedTaskResultCount;
    
    /**
     * 推送失败的任务结果数量
     */
    private Integer failedTaskResultCount;
    
    /**
     * 推送的任务ID列表
     */
    private List<String> pushedTaskIds;
    
    /**
     * 推送失败的任务ID列表
     */
    private List<String> failedTaskIds;
    
    // ==================== 统计信息推送 ====================
    
    /**
     * 是否推送了统计信息
     */
    private Boolean statisticsPushed;
    
    /**
     * 推送的统计信息内容
     */
    private Map<String, Object> statisticsData;
    
    // ==================== 错误和异常信息 ====================
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 详细错误列表
     */
    private List<String> errorDetails;
    
    /**
     * 警告信息列表
     */
    private List<String> warnings;
    
    // ==================== 推送策略信息 ====================
    
    /**
     * 使用的推送策略
     */
    private Map<String, Object> pushStrategy;
    
    /**
     * 最大推送消息数限制
     */
    private Integer maxMessageLimit;
    
    /**
     * 实际查询到的未读消息总数
     */
    private Integer totalUnreadMessageCount;
    
    /**
     * 实际查询到的未查看任务总数
     */
    private Integer totalUnviewedTaskCount;
    
    // ==================== 性能指标 ====================
    
    /**
     * 消息查询耗时（毫秒）
     */
    private Long messageQueryDurationMs;
    
    /**
     * 任务查询耗时（毫秒）
     */
    private Long taskQueryDurationMs;
    
    /**
     * 消息推送耗时（毫秒）
     */
    private Long messagePushDurationMs;
    
    /**
     * 任务推送耗时（毫秒）
     */
    private Long taskPushDurationMs;
    
    /**
     * 统计推送耗时（毫秒）
     */
    private Long statisticsPushDurationMs;
    
    // ==================== 异步推送相关 ====================
    
    /**
     * 异步任务ID（如果是异步推送）
     */
    private String asyncTaskId;
    
    /**
     * 推送状态：PENDING, RUNNING, COMPLETED, FAILED
     */
    private String status;
    
    /**
     * 推送进度（0-100）
     */
    private Integer progress;
    
    // ==================== 辅助方法 ====================
    
    /**
     * 计算推送成功率
     */
    public Double getSuccessRate() {
        int total = (pushedMessageCount != null ? pushedMessageCount : 0) + 
                   (failedMessageCount != null ? failedMessageCount : 0) + 
                   (pushedTaskResultCount != null ? pushedTaskResultCount : 0) + 
                   (failedTaskResultCount != null ? failedTaskResultCount : 0);
        
        if (total == 0) {
            return 100.0;
        }
        
        int successful = (pushedMessageCount != null ? pushedMessageCount : 0) + 
                        (pushedTaskResultCount != null ? pushedTaskResultCount : 0);
        
        return (successful * 100.0) / total;
    }
    
    /**
     * 获取推送总数
     */
    public Integer getTotalPushedCount() {
        return (pushedMessageCount != null ? pushedMessageCount : 0) + 
               (pushedTaskResultCount != null ? pushedTaskResultCount : 0);
    }
    
    /**
     * 获取失败总数
     */
    public Integer getTotalFailedCount() {
        return (failedMessageCount != null ? failedMessageCount : 0) + 
               (failedTaskResultCount != null ? failedTaskResultCount : 0);
    }
    
    /**
     * 是否有推送内容
     */
    public Boolean hasPushContent() {
        return getTotalPushedCount() > 0;
    }
    
    /**
     * 是否有推送失败
     */
    public Boolean hasPushFailures() {
        return getTotalFailedCount() > 0;
    }
    
    /**
     * 获取推送摘要信息
     */
    public String getSummary() {
        return String.format("用户[%s]上线推送: 消息%d条, 任务%d个, 成功率%.1f%%, 耗时%dms", 
                            userId, 
                            pushedMessageCount != null ? pushedMessageCount : 0,
                            pushedTaskResultCount != null ? pushedTaskResultCount : 0,
                            getSuccessRate(),
                            durationMs != null ? durationMs : 0);
    }
}