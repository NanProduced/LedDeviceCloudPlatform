package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询广播消息请求DTO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Schema(description = "查询广播消息请求DTO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryBroadcastMessageRequest {

    @Schema(description = "消息类型筛选项")
    private String messageType;

    @Schema(description = "消息子类型筛选项")
    private String subType_1;

    @Schema(description = "是否只看未读消息")
    private Boolean onlyUnread = Boolean.FALSE;

    @Schema(description = "消息范围筛选项（SYSTEM/ORG）")
    private String scope;
}