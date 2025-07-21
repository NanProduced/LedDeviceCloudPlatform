package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务通知结果
 * 
 * 表示单个任务结果通知的执行结果，包含通知状态、发送详情和错误信息。
 * 用于跟踪任务完成后的通知发送情况。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotificationResult {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 通知是否成功
     */
    private boolean success;
    
    /**
     * 会话级通知是否成功发送
     */
    private boolean sessionSent;
    
    /**
     * 用户级通知发送的会话数
     */
    private int userSessionsSent;
    
    /**
     * 是否已保存为离线通知
     */
    private boolean offlineSaved;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 通知时间戳
     */
    private LocalDateTime timestamp;
}