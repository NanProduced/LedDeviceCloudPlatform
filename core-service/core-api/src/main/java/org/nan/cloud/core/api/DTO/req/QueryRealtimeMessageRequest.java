package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "查询实时消息请求DTO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryRealtimeMessageRequest {

    @Schema(description = "消息类型筛选项")
    private String messageType;

    @Schema(description = "是否只看未读消息")
    private Boolean onlyUnread = Boolean.FALSE;
}
