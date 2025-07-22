package org.nan.cloud.message.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送消息响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "发送消息响应")
public class SendMessageResponse {
    
    @Schema(description = "是否发送成功")
    private boolean success;
    
    @Schema(description = "消息ID")
    private String messageId;
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "响应消息")
    private String message;
}