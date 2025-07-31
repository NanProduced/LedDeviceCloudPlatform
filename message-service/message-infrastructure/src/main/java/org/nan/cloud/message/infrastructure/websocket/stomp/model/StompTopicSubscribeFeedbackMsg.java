package org.nan.cloud.message.infrastructure.websocket.stomp.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.routing.SubscriptionLevel;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * STOMP主题订阅反馈消息
 * 
 * 用于向客户端反馈主题订阅操作的结果，包括成功订阅、订阅失败、
 * 权限不足等情况的反馈信息。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StompTopicSubscribeFeedbackMsg extends CommonStompMessage {

    public StompTopicSubscribeFeedbackMsg() {
        super();
    }
    
    public StompTopicSubscribeFeedbackMsg(String messageId, LocalDateTime timestamp, StompMessageTypes messageType, 
                                         String subType_1, String subType_2, String subType_3, 
                                         Source source, Target target, Object payload, String message, Metadata metadata) {
        super(messageId, timestamp, messageType, subType_1, subType_2, subType_3, source, target, payload, message, metadata);
    }

    /**
     * 主题订阅反馈数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicSubscribeFeedback {

        /**
         * 用户ID
         */
        private Long uid;

        /**
         * 订阅层次
         */
        private SubscriptionLevel subscriptionLevel;

        /**
         * 主题路径
         */
        private String topic;

        /**
         * 订阅是否成功
         */
        private boolean success;

        /**
         * 错误消息（失败时）
         */
        private String errorMsg;


        /**
         * 操作类型
         */
        private String operation; // SUBSCRIBE, UNSUBSCRIBE

    }


    /**
     * 创建成功订阅反馈消息
     */
    public static StompTopicSubscribeFeedbackMsg successFeedback(Long uid, SubscriptionLevel level, String topicPath) {
        TopicSubscribeFeedback feedback = TopicSubscribeFeedback.builder()
                .uid(uid)
                .subscriptionLevel(level)
                .topic(topicPath)
                .success(true)
                .operation("SUBSCRIBE")
                .build();

        StompTopicSubscribeFeedbackMsg msg = new StompTopicSubscribeFeedbackMsg();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setTimestamp(LocalDateTime.now());
        msg.setMessageType(StompMessageTypes.TOPIC_SUBSCRIBE_FEEDBACK);
        msg.setPayload(feedback);
        msg.setMessage("订阅成功");
        
        // 设置目标用户
        msg.setTarget(Target.builder()
                .targetType("USER")
                .uids(List.of(uid))
                .build());
        
        // 设置元数据
        msg.setMetadata(Metadata.builder()
                .priority(Priority.HIGH)
                .persistent(false)
                .requireAck(false)
                .retryCount(0)
                .ttl(30000L) // 30秒TTL
                .build());

        return msg;
    }

    /**
     * 创建失败订阅反馈消息
     */
    public static StompTopicSubscribeFeedbackMsg failureFeedback(Long uid, SubscriptionLevel level, String topicPath, String errorMsg) {
        TopicSubscribeFeedback feedback = TopicSubscribeFeedback.builder()
                .uid(uid)
                .subscriptionLevel(level)
                .topic(topicPath)
                .success(false)
                .errorMsg(errorMsg)
                .operation("SUBSCRIBE")
                .build();

        StompTopicSubscribeFeedbackMsg msg = new StompTopicSubscribeFeedbackMsg();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setTimestamp(LocalDateTime.now());
        msg.setMessageType(StompMessageTypes.TOPIC_SUBSCRIBE_FEEDBACK);
        msg.setPayload(feedback);
        msg.setMessage("订阅失败: " + errorMsg);
        
        // 设置目标用户
        msg.setTarget(Target.builder()
                .targetType("USER")
                .uids(List.of(uid))
                .build());
        
        // 设置元数据
        msg.setMetadata(Metadata.builder()
                .priority(Priority.HIGH)
                .persistent(false)
                .requireAck(false)
                .retryCount(0)
                .ttl(60000L) // 60秒TTL，失败消息保留更久
                .build());

        return msg;
    }

    /**
     * 创建取消订阅成功反馈消息
     */
    public static StompTopicSubscribeFeedbackMsg unsubscribeSuccessFeedback(Long uid, String topicPath) {
        TopicSubscribeFeedback feedback = TopicSubscribeFeedback.builder()
                .uid(uid)
                .topic(topicPath)
                .success(true)
                .operation("UNSUBSCRIBE")
                .build();

        StompTopicSubscribeFeedbackMsg msg = new StompTopicSubscribeFeedbackMsg();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setTimestamp(LocalDateTime.now());
        msg.setMessageType(StompMessageTypes.TOPIC_SUBSCRIBE_FEEDBACK);
        msg.setPayload(feedback);
        msg.setMessage("取消订阅成功");
        
        // 设置目标用户
        msg.setTarget(Target.builder()
                .targetType("USER")
                .uids(List.of(uid))
                .build());
        
        // 设置元数据
        msg.setMetadata(Metadata.builder()
                .priority(Priority.NORMAL)
                .persistent(false)
                .requireAck(false)
                .retryCount(0)
                .ttl(15000L) // 15秒TTL
                .build());

        return msg;
    }

    /**
     * 创建取消订阅失败反馈消息
     */
    public static StompTopicSubscribeFeedbackMsg unsubscribeFailureFeedback(Long uid, String topicPath, String errorMsg) {
        TopicSubscribeFeedback feedback = TopicSubscribeFeedback.builder()
                .uid(uid)
                .topic(topicPath)
                .success(false)
                .errorMsg(errorMsg)
                .operation("UNSUBSCRIBE")
                .build();

        StompTopicSubscribeFeedbackMsg msg = new StompTopicSubscribeFeedbackMsg();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setTimestamp(LocalDateTime.now());
        msg.setMessageType(StompMessageTypes.TOPIC_SUBSCRIBE_FEEDBACK);
        msg.setPayload(feedback);
        msg.setMessage("取消订阅失败: " + errorMsg);
        
        // 设置目标用户
        msg.setTarget(Target.builder()
                .targetType("USER")
                .uids(List.of(uid))
                .build());
        
        // 设置元数据
        msg.setMetadata(Metadata.builder()
                .priority(Priority.NORMAL)
                .persistent(false)
                .requireAck(false)
                .retryCount(0)
                .ttl(30000L) // 30秒TTL
                .build());

        return msg;
    }

    /**
     * 创建权限不足反馈消息
     */
    public static StompTopicSubscribeFeedbackMsg permissionDeniedFeedback(Long uid, String topicPath, String permissions) {
        TopicSubscribeFeedback feedback = TopicSubscribeFeedback.builder()
                .uid(uid)
                .topic(topicPath)
                .success(false)
                .errorMsg("权限不足")
                .operation("SUBSCRIBE")
                .build();

        StompTopicSubscribeFeedbackMsg msg = new StompTopicSubscribeFeedbackMsg();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setTimestamp(LocalDateTime.now());
        msg.setMessageType(StompMessageTypes.TOPIC_SUBSCRIBE_FEEDBACK);
        msg.setPayload(feedback);
        msg.setMessage("订阅失败: 权限不足，需要权限: " + permissions);
        
        // 设置目标用户
        msg.setTarget(Target.builder()
                .targetType("USER")
                .uids(List.of(uid))
                .build());
        
        // 设置元数据
        msg.setMetadata(Metadata.builder()
                .priority(Priority.HIGH)
                .persistent(false)
                .requireAck(false)
                .retryCount(0)
                .ttl(120000L) // 2分钟TTL，权限错误消息保留更久
                .build());

        return msg;
    }

    /**
     * 从反馈消息中提取反馈数据
     */
    public TopicSubscribeFeedback getFeedbackData() {
        if (this.getPayload() instanceof TopicSubscribeFeedback) {
            return (TopicSubscribeFeedback) this.getPayload();
        }
        return null;
    }

    /**
     * 判断是否为成功反馈
     */
    public boolean isSuccess() {
        TopicSubscribeFeedback feedback = getFeedbackData();
        return feedback != null && feedback.isSuccess();
    }

    /**
     * 判断是否为订阅操作
     */
    public boolean isSubscribeOperation() {
        TopicSubscribeFeedback feedback = getFeedbackData();
        return feedback != null && "SUBSCRIBE".equals(feedback.getOperation());
    }

    /**
     * 判断是否为取消订阅操作
     */
    public boolean isUnsubscribeOperation() {
        TopicSubscribeFeedback feedback = getFeedbackData();
        return feedback != null && "UNSUBSCRIBE".equals(feedback.getOperation());
    }

    /**
     * 获取订阅的主题路径
     */
    public String getTopicPath() {
        TopicSubscribeFeedback feedback = getFeedbackData();
        return feedback != null ? feedback.getTopic() : null;
    }

    /**
     * 获取错误消息
     */
    public String getErrorMessage() {
        TopicSubscribeFeedback feedback = getFeedbackData();
        return feedback != null ? feedback.getErrorMsg() : null;
    }
}
