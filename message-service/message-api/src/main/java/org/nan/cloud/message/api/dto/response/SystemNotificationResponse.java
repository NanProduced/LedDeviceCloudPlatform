package org.nan.cloud.message.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统通知响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "系统通知响应")
public class SystemNotificationResponse {
    
    @Schema(description = "是否成功")
    private boolean success;
    
    @Schema(description = "响应消息")
    private String message;
}