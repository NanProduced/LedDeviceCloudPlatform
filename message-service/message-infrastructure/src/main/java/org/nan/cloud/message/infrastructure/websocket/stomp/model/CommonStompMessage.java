package org.nan.cloud.message.infrastructure.websocket.stomp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * STOMP协议统一消息结构
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommonStompMessage {

    /**
     * 消息ID
     * <p>UUID</p>
     */
    private String messageId;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 消息类型-主类型
     */
    private StompMessageTypes messageType;

    /* ==== 保留三个备用子类型 ==== */

    private String subType_1;

    private String subType_2;

    private String subType_3;

    /**
     * 消息来源
     */
    private Source source;

    /**
     * 消息目标
     */
    private Target target;

    /**
     * 消息业务数据
     */
    private Object payload;

    /**
     * 消息String备用字段
     */
    private String message;

    /**
     * 元数据
     */
    private Metadata metadata;


    /**
     * 消息来源服务
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Source {

        private String serviceId;

        private String resourceType;

        private String resourceId;

        private String taskId;

        private String executionId;

        private String batchContext;
    }

    /**
     * 消息目标
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Target {

        private String targetType;

        private List<Long> uids;

        private Long oid;

        private String topicPath;
    }

    /**
     * 消息元数据
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Metadata {

        private Priority priority;

        private boolean persistent;

        private Long ttl;

        private boolean requireAck;

        private Long sequenceId;

        private String correlationId;

        private Integer retryCount;
    }

}
