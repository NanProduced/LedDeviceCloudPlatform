package org.nan.cloud.message.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 任务通知响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "任务通知响应")
public class TaskNotificationResponse {
    
    @Schema(description = "是否成功")
    private boolean success;
    
    @Schema(description = "响应消息")
    private String message;
    
    @Schema(description = "任务ID")
    private String taskId;
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "通知结果详情")
    private Object resultData;
}