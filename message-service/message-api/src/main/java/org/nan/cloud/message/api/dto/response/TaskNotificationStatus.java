package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务通知状态
 * 
 * 跟踪任务通知的详细状态信息，包括发送状态、查看状态和重试信息。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotificationStatus {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 通知是否已发送
     */
    private boolean notificationSent;
    
    /**
     * 发送时间
     */
    private LocalDateTime sentTime;
    
    /**
     * 用户是否已查看
     */
    private boolean userViewed;
    
    /**
     * 查看时间
     */
    private LocalDateTime viewedTime;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}