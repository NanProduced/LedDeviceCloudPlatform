package org.nan.cloud.message.infrastructure.websocket.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.interceptor.StompPrincipal;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.nan.cloud.message.infrastructure.websocket.subscription.SubscriptionManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * STOMP会话事件监听器
 * 
 * 处理STOMP会话生命周期事件，确保所有类型的断开都能正确清理订阅：
 * - 主动断开：客户端发送DISCONNECT命令
 * - 异常断开：网络故障、浏览器崩溃、超时等
 * - 隐式断开：SockJS降级失败等
 * 
 * 解决核心问题：
 * - 异常断开时订阅未清理，导致重连时重复订阅检测失败
 * - 确保所有断开类型都触发完整的清理流程
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompSessionEventListener {
    
    private final StompConnectionManager stompConnectionManager;
    private final SubscriptionManager subscriptionManager;
    
    /**
     * 处理会话连接事件
     * 
     * 当STOMP会话建立时触发，用于调试和监控
     */
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        Principal principal = event.getUser();
        
        log.debug("STOMP会话连接事件 - sessionId: {}, principal: {}", 
                sessionId, principal != null ? principal.getName() : "null");
        
        // 连接处理已在StompChannelInterceptor中完成，这里仅做监控
    }
    
    /**
     * 处理会话断开事件
     * 
     * 当STOMP会话断开时触发，无论是主动还是异常断开都会触发此事件
     * 这是确保订阅清理完整性的关键机制
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        try {
            String sessionId = event.getSessionId();
            Principal principal = event.getUser();
            
            log.info("处理STOMP会话断开事件 - sessionId: {}", sessionId);
            
            // 提取用户信息
            GatewayUserInfo userInfo = extractUserInfo(principal);
            
            if (userInfo != null) {
                String userId = userInfo.getUid().toString();
                log.info("用户会话断开 - 用户: {}, 会话: {}, 断开类型: {}", 
                        userId, sessionId, event.getCloseStatus());
                
                // 1. 清理用户会话订阅（核心修复）
                subscriptionManager.cleanupUserSessionSubscriptions(userInfo, sessionId);
                log.info("✅ 用户会话订阅已清理 - 用户: {}, 会话: {}", userId, sessionId);
                
                // 2. 清理连接管理器中的连接记录
                // 注意：这里可能会重复调用（如果StompChannelInterceptor已处理DISCONNECT），但removeConnection是幂等的
                stompConnectionManager.removeConnection(sessionId);
                log.info("✅ 连接记录已清理 - 会话: {}", sessionId);
                
            } else {
                log.warn("无法从会话断开事件中提取用户信息 - sessionId: {}", sessionId);
                
                // 即使没有用户信息，也尝试清理连接记录
                stompConnectionManager.removeConnection(sessionId);
            }
            
        } catch (Exception e) {
            log.error("处理STOMP会话断开事件失败 - sessionId: {}, 错误: {}", 
                    event.getSessionId(), e.getMessage(), e);
        }
    }
    
    /**
     * 从Principal中提取用户信息
     * 
     * @param principal 用户认证主体
     * @return Gateway用户信息，提取失败时返回null
     */
    private GatewayUserInfo extractUserInfo(Principal principal) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            GatewayUserInfo userInfo = stompPrincipal.getUserInfo();
            if (userInfo != null) {
                log.debug("从StompPrincipal成功提取用户信息 - userId: {}, orgId: {}", 
                        userInfo.getUid(), userInfo.getOid());
                return userInfo;
            } else {
                log.warn("StompPrincipal中的GatewayUserInfo为null");
                return null;
            }
        }
        
        if (principal != null) {
            log.warn("Principal不是StompPrincipal类型，无法提取GatewayUserInfo - type: {}, name: {}", 
                    principal.getClass().getSimpleName(), principal.getName());
        }
        
        return null;
    }
}