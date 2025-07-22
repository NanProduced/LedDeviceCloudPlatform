package org.nan.cloud.message.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户状态响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "用户状态响应")
public class UserStatusResponse {
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "是否在线")
    private boolean online;
    
    @Schema(description = "连接数量")
    private int connectionCount;
    
    @Schema(description = "状态信息")
    private String message;
}