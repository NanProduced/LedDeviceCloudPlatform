package org.nan.cloud.message.infrastructure.websocket.stomp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    // ==================== 快速构造方法 ====================
    
    /**
     * 创建简单文本消息
     */
    public static CommonStompMessage simpleText(StompMessageTypes messageType, String text) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(messageType)
                .message(text)
                .metadata(Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .requireAck(false)
                        .retryCount(0)
                        .build())
                .build();
    }
    
    /**
     * 创建带目标用户的消息
     */
    public static CommonStompMessage toUser(StompMessageTypes messageType, Long userId, Object payload) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(messageType)
                .payload(payload)
                .target(Target.builder()
                        .targetType("USER")
                        .uids(List.of(userId))
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .requireAck(false)
                        .retryCount(0)
                        .build())
                .build();
    }
    
    /**
     * 创建带目标用户和消息文本的消息
     */
    public static CommonStompMessage toUser(StompMessageTypes messageType, Long userId, Object payload, String message) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(messageType)
                .payload(payload)
                .message(message)
                .target(Target.builder()
                        .targetType("USER")
                        .uids(List.of(userId))
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .requireAck(false)
                        .retryCount(0)
                        .build())
                .build();
    }
    
    /**
     * 创建多用户广播消息
     */
    public static CommonStompMessage toUsers(StompMessageTypes messageType, List<Long> userIds, Object payload) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(messageType)
                .payload(payload)
                .target(Target.builder()
                        .targetType("USERS")
                        .uids(userIds)
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .requireAck(false)
                        .retryCount(0)
                        .build())
                .build();
    }
    
    /**
     * 创建组织广播消息
     */
    public static CommonStompMessage toOrganization(StompMessageTypes messageType, Long orgId, Object payload) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(messageType)
                .payload(payload)
                .target(Target.builder()
                        .targetType("ORGANIZATION")
                        .oid(orgId)
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .requireAck(false)
                        .retryCount(0)
                        .build())
                .build();
    }
    
    /**
     * 创建主题消息
     */
    public static CommonStompMessage toTopic(StompMessageTypes messageType, String topicPath, Object payload) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(messageType)
                .payload(payload)
                .target(Target.builder()
                        .targetType("TOPIC")
                        .topicPath(topicPath)
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .requireAck(false)
                        .retryCount(0)
                        .build())
                .build();
    }
    
    /**
     * 创建需要确认的可靠消息
     */
    public static CommonStompMessage reliable(StompMessageTypes messageType, Object payload, Priority priority) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(messageType)
                .payload(payload)
                .metadata(Metadata.builder()
                        .priority(priority != null ? priority : Priority.NORMAL)
                        .persistent(true)
                        .requireAck(true)
                        .retryCount(0)
                        .ttl(300000L) // 5分钟TTL
                        .build())
                .build();
    }
    
    /**
     * 创建系统通知消息
     */
    public static CommonStompMessage systemNotification(String title, String content, Long orgId) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.NOTIFICATION)
                .message(title)
                .payload(content)
                .target(Target.builder()
                        .targetType("ORGANIZATION")
                        .oid(orgId)
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.HIGH)
                        .persistent(true)
                        .requireAck(false)
                        .retryCount(0)
                        .ttl(1800000L) // 30分钟TTL
                        .build())
                .build();
    }
    
    /**
     * 创建设备状态消息
     */
    public static CommonStompMessage deviceStatus(String deviceId, Long orgId, Object statusData) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.TERMINAL_STATUS_CHANGE)
                .payload(statusData)
                .source(Source.builder()
                        .serviceId("core-service")
                        .resourceType("DEVICE")
                        .resourceId(deviceId)
                        .build())
                .target(Target.builder()
                        .targetType("ORGANIZATION")
                        .oid(orgId)
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .requireAck(false)
                        .retryCount(0)
                        .ttl(30000L) // 30秒TTL
                        .build())
                .build();
    }
    
    /**
     * 创建指令结果消息
     */
    public static CommonStompMessage commandResult(String commandId, String deviceId, Long userId, Object resultData) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.COMMAND_FEEDBACK)
                .subType_1("SINGLE")
                .payload(resultData)
                .source(Source.builder()
                        .serviceId("core-service")
                        .resourceType("COMMAND")
                        .resourceId(commandId)
                        .taskId(commandId)
                        .build())
                .target(Target.builder()
                        .targetType("USER")
                        .uids(List.of(userId))
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.HIGH)
                        .persistent(true)
                        .requireAck(false)
                        .retryCount(0)
                        .ttl(300000L) // 5分钟TTL
                        .build())
                .build();
    }
    
    /**
     * 创建批量进度消息
     */
    public static CommonStompMessage batchProgress(String batchId, Long userId, Object progressData) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.COMMAND_FEEDBACK)
                .subType_1("BATCH")
                .payload(progressData)
                .source(Source.builder()
                        .serviceId("core-service")
                        .resourceType("BATCH")
                        .resourceId(batchId)
                        .batchContext(batchId)
                        .build())
                .target(Target.builder()
                        .targetType("USER")
                        .uids(List.of(userId))
                        .build())
                .metadata(Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .requireAck(false)
                        .retryCount(0)
                        .ttl(180000L) // 3分钟TTL
                        .build())
                .build();
    }
    
    /**
     * 基于现有消息创建副本，允许修改部分字段
     */
    public static CommonStompMessageBuilder copyFrom(CommonStompMessage original) {
        return CommonStompMessage.builder()
                .messageId(original.getMessageId())
                .timestamp(original.getTimestamp())
                .messageType(original.getMessageType())
                .subType_1(original.getSubType_1())
                .subType_2(original.getSubType_2())
                .subType_3(original.getSubType_3())
                .source(original.getSource())
                .target(original.getTarget())
                .payload(original.getPayload())
                .message(original.getMessage())
                .metadata(original.getMetadata());
    }
    
    /**
     * 为现有消息生成新的messageId（用于重试等场景）
     */
    public CommonStompMessage withNewId() {
        return copyFrom(this)
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 增加重试次数
     */
    public CommonStompMessage withIncrementedRetry() {
        Metadata newMetadata = Metadata.builder()
                .priority(this.metadata.getPriority())
                .persistent(this.metadata.isPersistent())
                .ttl(this.metadata.getTtl())
                .requireAck(this.metadata.isRequireAck())
                .sequenceId(this.metadata.getSequenceId())
                .correlationId(this.metadata.getCorrelationId())
                .retryCount((this.metadata.getRetryCount() != null ? this.metadata.getRetryCount() : 0) + 1)
                .build();
                
        return copyFrom(this)
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .metadata(newMetadata)
                .build();
    }

}
