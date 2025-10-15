package org.nan.cloud.common.basic.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;


/**
 * 一般是发送到用户个人队列的消息
 * 持久化，以便用户可以在消息中心查询
 */
@Document("realtime_message")
@Data
@Builder
public class RealtimeMessageDocument {

    @Id
    private String id;

    private String messageId;

    private String timestamp;

    private Long oid;

    // 标注用户
    private Long uid;

    // 消息分类
    private String messageType;
    @Field("subType_1")
    private String subType_1;
    @Field("subType_2")
    private String subType_2;
    private String level;

    // 交互相关

    /**
     * 是否需要点击已读
     * 未读需要以小红点提醒
     */
    private Boolean requireAsk;
    /**
     * 是否已读
     */
    private Boolean isRead;
    /**
     * 阅读时间
     */
    private String readAt;

    // 消息内容
    private String title;
    private String content;
    private Object payload;

    /**
     * 索引字段
     * {oid}_{uid}_{messageType}
     */
    @Indexed
    private String indexKey;

    @CreatedDate
    private LocalDateTime createTime;

}
