package org.nan.cloud.message.infrastructure.websocket.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.interceptor.StompPrincipal;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.nan.cloud.message.api.stomp.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.nan.cloud.message.infrastructure.websocket.subscription.SubscriptionManager;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户队列订阅监听器
 * 
 * 监听客户端对 /user/queue/messages 的订阅事件，
 * 当用户成功订阅个人消息队列后，发送包含当前订阅信息的欢迎消息。
 * 
 * 解决时序问题：
 * - 连接建立时不立即发送欢迎消息
 * - 等待客户端主动订阅 /user/queue/messages
 * - 订阅成功后发送欢迎消息，此时客户端已能接收消息
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserQueueSubscriptionListener {

    private final SubscriptionManager subscriptionManager;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 记录等待发送欢迎消息的用户会话
     * Key: sessionId, Value: userId
     */
    private final Map<String, String> pendingWelcomeMessages = new ConcurrentHashMap<>();
    
    /**
     * 监听订阅事件
     * 
     * 当客户端订阅任何主题时触发，检查是否为 /user/queue/messages 订阅
     */
    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        try {
            String destination = (String) event.getMessage().getHeaders().get("simpDestination");
            String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
            Principal principal = event.getUser();
            
            log.debug("收到订阅事件 - 会话: {}, 目标: {}", sessionId, destination);
            
            // 检查是否为用户个人消息队列订阅
            if (isUserQueueSubscription(destination)) {
                handleUserQueueSubscription(sessionId, principal, destination);
            }
            
        } catch (Exception e) {
            log.error("处理订阅事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 注册等待欢迎消息的会话
     * 
     * 在连接建立时调用，记录需要在订阅成功后发送欢迎消息的会话
     */
    public void registerPendingWelcomeMessage(String sessionId, String userId) {
        pendingWelcomeMessages.put(sessionId, userId);
        log.debug("注册等待欢迎消息 - 会话: {}, 用户: {}", sessionId, userId);
    }
    
    /**
     * 移除等待欢迎消息的会话
     */
    public void removePendingWelcomeMessage(String sessionId) {
        String userId = pendingWelcomeMessages.remove(sessionId);
        if (userId != null) {
            log.debug("移除等待欢迎消息 - 会话: {}, 用户: {}", sessionId, userId);
        }
    }
    
    /**
     * 检查是否为用户队列订阅
     */
    private boolean isUserQueueSubscription(String destination) {
        return destination != null && 
               (destination.equals(StompTopic.USER_MESSAGES_QUEUE) || 
                destination.startsWith("/user/queue/messages"));
    }
    
    /**
     * 处理用户队列订阅
     */
    private void handleUserQueueSubscription(String sessionId, Principal principal, String destination) {
        try {
            // 检查是否有等待发送的欢迎消息
            String userId = pendingWelcomeMessages.get(sessionId);
            if (userId == null) {
                log.debug("会话 {} 没有等待发送的欢迎消息", sessionId);
                return;
            }
            
            // 提取用户信息
            GatewayUserInfo userInfo = extractUserInfo(principal);
            if (userInfo == null) {
                log.warn("无法从Principal中提取用户信息 - sessionId: {}", sessionId);
                return;
            }
            
            log.info("✅ 用户成功订阅个人消息队列 - 用户: {}, 会话: {}, 目标: {}", 
                    userId, sessionId, destination);
            
            // 发送欢迎消息
            sendWelcomeMessageWithSubscriptions(userInfo, sessionId);
            
            // 移除等待记录
            removePendingWelcomeMessage(sessionId);
            
        } catch (Exception e) {
            log.error("处理用户队列订阅失败 - 会话: {}, 错误: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 发送包含订阅信息的欢迎消息
     */
    private void sendWelcomeMessageWithSubscriptions(GatewayUserInfo userInfo, String sessionId) {
        try {
            String userId = userInfo.getUid().toString();
            
            // 获取用户当前已订阅的主题
            Set<String> subscribedTopics = subscriptionManager.getUserSubscribedTopics(userId);
            
            // 构建推荐的订阅主题列表
            List<String> recommendedSubscriptions = List.of(
                    "/topic/org/" + userInfo.getOid(),
                    "/topic/system"
            );
            
            // 发送欢迎消息
            CommonStompMessage welcomeMessage = CommonStompMessage.builder()
                    .messageType(StompMessageTypes.CONNECTION_STATUS)
                    .timestamp(Instant.now().toString())
                    .oid(userInfo.getOid())
                    .title("连接成功并已订阅个人消息队列")
                    .content("✅ STOMP连接已建立\\n" +
                            "🔔 个人消息队列订阅成功\\n")
                    .payload(Map.of(
                            "subscribedTopics", subscribedTopics,
                            "recommendedSubscriptions", recommendedSubscriptions,
                            "sessionId", sessionId
                    ))
                    .build();
            
            // 直接发送到用户的个人消息队列
            messagingTemplate.convertAndSendToUser(userId, StompTopic.USER_MESSAGES_QUEUE, welcomeMessage);
            
            log.info("✅ 欢迎消息发送成功 - 用户: {}, 会话: {}, 已订阅主题数: {}", 
                    userId, sessionId, subscribedTopics.size());
            
        } catch (Exception e) {
            log.error("发送欢迎消息失败 - 用户: {}, 会话: {}, 错误: {}", 
                    userInfo.getUid(), sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 从Principal中提取用户信息
     */
    private GatewayUserInfo extractUserInfo(Principal principal) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal.getUserInfo();
        }
        return null;
    }
}