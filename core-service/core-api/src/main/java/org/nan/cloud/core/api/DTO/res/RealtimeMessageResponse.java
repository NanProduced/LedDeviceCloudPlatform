package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "实时消息响应类")
@Data
@Builder
public class RealtimeMessageResponse {

    @Schema(description = "用于详情查询的id")
    private String id;

    @Schema(description = "时间戳")
    private String timestamp;

    private Long oid;

    private Long uid;

    @Schema(description = "主类型")
    private String messageType;

    private String subType_1;

    private String subType_2;

    @Schema(description = "优先级")
    private String level;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "附加消息")
    private Object payload;

}
