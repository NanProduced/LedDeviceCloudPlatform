package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广播消息响应类
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Schema(description = "广播消息响应类")
@Data
@Builder
public class BroadcastMessageResponse {

    @Schema(description = "MongoDB文档ID")
    private String id;

    @Schema(description = "消息ID")
    private String messageId;

    @Schema(description = "时间戳")
    private String timestamp;

    @Schema(description = "组织ID")
    private Long oid;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "消息子类型1")
    private String subType_1;

    @Schema(description = "消息子类型2")
    private String subType_2;

    @Schema(description = "消息级别")
    private String level;

    @Schema(description = "消息范围（SYSTEM/ORG）")
    private String scope;

    @Schema(description = "目标组织列表")
    private List<Long> targetOid;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "附加数据")
    private Object payload;

    @Schema(description = "过期时间")
    private LocalDateTime expiredAt;

    @Schema(description = "发布者ID")
    private Long publisherId;

    @Schema(description = "用户是否已读该消息")
    private Boolean isRead;

    @Schema(description = "用户已读时间")
    private LocalDateTime readAt;
}