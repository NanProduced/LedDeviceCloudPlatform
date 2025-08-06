package org.nan.cloud.common.basic.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 通知消息阅读统计
 */
@Document("broadcast_message_read_status")
@Data
@Builder
@CompoundIndex(def = "{'userId': 1, 'messageId': 1}", unique = true)
@CompoundIndex(def = "{'messageId': 1}")
@CompoundIndex(def = "{'userId': 1, 'readAt': -1}")
public class BroadcastMessageReadStatus {
    @Id
    private String id; // 格式: {messageId}_{userId}

    @Indexed
    private String messageId;     // 消息ID

    @Indexed
    private Long userId;          // 用户ID

    @Indexed
    private Long orgId;           // 组织ID（查询优化）

    private LocalDateTime readAt;  // 已读时间
}


