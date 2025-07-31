package org.nan.cloud.message.infrastructure.websocket.reliability;

import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;

import java.time.LocalDateTime;

/**
 * 消息构建辅助工具
 * 
 * 提供正确构建CommonStompMessage的便捷方法，
 * 确保字段使用的一致性和正确性。
 * 
 * @author Nan
 * @since 3.1.0
 */
public class MessageHelper {
    
    /**
     * 创建基础消息构建器
     */
    public static CommonStompMessage.CommonStompMessageBuilder createBaseMessage() {
        return CommonStompMessage.builder()
            .timestamp(LocalDateTime.now())
            .metadata(CommonStompMessage.Metadata.builder()
                .priority(Priority.NORMAL)
                .requireAck(false)
                .build());
    }
    
    /**
     * 创建需要确认的消息
     */
    public static CommonStompMessage createReliableMessage(String messageId, 
                                                          StompMessageTypes messageType,
                                                          Object content,
                                                          Priority priority,
                                                          boolean requiresAck) {
        return CommonStompMessage.builder()
            .messageId(messageId)
            .messageType(messageType)
            .payload(content)
            .timestamp(LocalDateTime.now())
            .metadata(CommonStompMessage.Metadata.builder()
                .priority(priority)
                .requireAck(requiresAck)
                .build())
            .build();
    }
    
    /**
     * 创建文本消息
     */
    public static CommonStompMessage createTextMessage(String messageId,
                                                      StompMessageTypes messageType,
                                                      String textContent,
                                                      Priority priority,
                                                      boolean requiresAck) {
        return CommonStompMessage.builder()
            .messageId(messageId)
            .messageType(messageType)
            .message(textContent)
            .timestamp(LocalDateTime.now())
            .metadata(CommonStompMessage.Metadata.builder()
                .priority(priority)
                .requireAck(requiresAck)
                .build())
            .build();
    }
    
    /**
     * 从现有消息中提取内容
     */
    public static String extractMessageContent(CommonStompMessage message) {
        if (message.getPayload() != null) {
            return message.getPayload().toString();
        } else if (message.getMessage() != null) {
            return message.getMessage();
        } else {
            return ""; // 空内容
        }
    }
    
    /**
     * 检查消息是否需要确认
     */
    public static boolean requiresAck(CommonStompMessage message) {
        return message.getMetadata() != null && message.getMetadata().isRequireAck();
    }
    
    /**
     * 获取消息优先级
     */
    public static Priority getPriority(CommonStompMessage message) {
        if (message.getMetadata() != null && message.getMetadata().getPriority() != null) {
            return message.getMetadata().getPriority();
        }
        return Priority.NORMAL; // 默认优先级
    }
}