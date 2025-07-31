package org.nan.cloud.message.infrastructure.websocket.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * STOMP消息发送服务
 * 
 * 职责：
 * 1. 统一的STOMP消息发送入口
 * 2. 封装SimpMessagingTemplate的使用
 * 3. 提供多种发送模式（用户、主题、广播）
 * 4. 消息发送的错误处理和日志记录
 * 
 * 设计目标：
 * - 解决循环依赖：提供独立的消息发送层
 * - 单一职责：专注于消息发送，不涉及连接管理和分发逻辑
 * - 统一接口：为其他组件提供一致的消息发送API
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StompMessageSender {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 发送消息到指定主题
     * 
     * @param topicPath 主题路径
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendToTopic(String topicPath, CommonStompMessage message) {
        try {
            enrichMessage(message);
            messagingTemplate.convertAndSend(topicPath, message);
            
            log.debug("✅ 主题消息发送成功 - 主题: {}, 消息ID: {}", topicPath, message.getMessageId());
            return true;
            
        } catch (Exception e) {
            log.error("❌ 主题消息发送失败 - 主题: {}, 消息ID: {}, 错误: {}", 
                    topicPath, message.getMessageId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送消息到指定用户
     * 
     * @param userId 用户ID
     * @param destination 目标路径
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendToUser(String userId, String destination, CommonStompMessage message) {
        try {
            enrichMessage(message);
            messagingTemplate.convertAndSendToUser(userId, destination, message);
            
            log.debug("✅ 用户消息发送成功 - 用户: {}, 目标: {}, 消息ID: {}", 
                    userId, destination, message.getMessageId());
            return true;
            
        } catch (Exception e) {
            log.error("❌ 用户消息发送失败 - 用户: {}, 目标: {}, 消息ID: {}, 错误: {}", 
                    userId, destination, message.getMessageId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送消息到指定会话
     *
     * @param userId 用户Id
     * @param sessionId 会话ID
     * @param destination 目标路径
     * @param payload 消息内容
     * @return 是否发送成功
     */
    public boolean sendToSession(String userId, String sessionId,
                                 String destination, Object payload) {
        try {
            // 1. 利用 SimpMessageHeaderAccessor 构造带有 sessionId 的 headers
            SimpMessageHeaderAccessor headerAccessor =
                    SimpMessageHeaderAccessor.create();
            headerAccessor.setSessionId(sessionId);
            // 必须设置为 mutable，否则 header 会被 Spring 丢弃
            headerAccessor.setLeaveMutable(true);

            // 2. 调用 convertAndSendToUser，传入 username 和自定义 headers
            messagingTemplate.convertAndSendToUser(
                    userId,
                    destination,
                    payload,
                    headerAccessor.getMessageHeaders()
            );

            log.debug("✅ 成功发给用户 {} 的会话 {} -> {}",
                    userId, sessionId, destination);
            return true;
        } catch (Exception e) {
            log.error("❌ 发消息失败 user={}, session={}, dest={}, err={}",
                    userId, sessionId, destination, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 丰富消息内容
     * 添加发送时间戳等元数据
     */
    private void enrichMessage(CommonStompMessage message) {
        if (message != null) {
            if (message.getTimestamp() == null) {
                message.setTimestamp(LocalDateTime.now());
            }
            
            // 可以在这里添加其他消息丰富逻辑
            // 如：发送方信息、消息追踪ID等
        }
    }
}