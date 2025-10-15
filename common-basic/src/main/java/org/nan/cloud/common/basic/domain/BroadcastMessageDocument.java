package org.nan.cloud.common.basic.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document("broadcast_message")
@Data
@Builder
public class BroadcastMessageDocument {

    @Id
    private String id;

    private String messageId;

    private String timestamp;

    private Long oid;

    // 消息分类
    private String messageType;
    @Field("subType_1")
    private String subType_1;
    @Field("subType_2")
    private String subType_2;
    private String level;
    /**
     * SYSTEM : 系统
     * ORG : 组织
     */
    private String scope;

    /**
     * 目标组织列表
     * 系统级参数，不对外显示
     */
    private List<Long> targetOid;

    // 消息内容
    private String title;
    private String content;
    private Object payload;


    // 消息状态
    private LocalDateTime expiredAt; // 过期时间

    /**
     * 系统发布为null
     */
    private Long publisherId; // 发布者ID


}

