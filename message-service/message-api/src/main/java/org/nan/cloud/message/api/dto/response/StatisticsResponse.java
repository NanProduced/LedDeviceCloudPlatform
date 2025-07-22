package org.nan.cloud.message.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统计信息响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "统计信息响应")
public class StatisticsResponse {
    
    @Schema(description = "在线用户总数")
    private int totalOnlineUsers;
    
    @Schema(description = "时间戳")
    private long timestamp;
    
    @Schema(description = "响应消息")
    private String message;
}