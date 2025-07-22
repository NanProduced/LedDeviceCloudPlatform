package org.nan.cloud.message.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 广播消息响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "广播消息响应")
public class BroadcastResponse {
    
    @Schema(description = "是否广播成功")
    private boolean success;
    
    @Schema(description = "消息ID")
    private String messageId;
    
    @Schema(description = "组织ID（组织广播时使用）")
    private String organizationId;
    
    @Schema(description = "成功推送数量")
    private int successCount;
    
    @Schema(description = "响应消息")
    private String message;
}