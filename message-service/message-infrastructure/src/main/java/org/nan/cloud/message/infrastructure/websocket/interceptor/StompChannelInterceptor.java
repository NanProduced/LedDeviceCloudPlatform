package org.nan.cloud.message.infrastructure.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.nan.cloud.message.infrastructure.websocket.subscription.SubscriptionManager;
import org.nan.cloud.message.infrastructure.websocket.subscription.SubscriptionResult;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

/**
 * STOMP通道拦截器
 * 
 * 处理STOMP协议层面的消息拦截和权限控制：
 * 1. 连接建立时发送连接成功消息
 * 2. 订阅请求的权限验证
 * 3. 消息发送的权限检查
 * 4. 连接断开时的清理工作
 * 
 * 主要功能：
 * - CONNECT: 建立连接并发送连接成功消息
 * - SUBSCRIBE: 验证订阅权限
 * - SEND: 验证消息发送权限
 * - DISCONNECT: 清理连接资源
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private final StompConnectionManager stompConnectionManager;
    private final SubscriptionManager subscriptionManager;

    public StompChannelInterceptor(
            @Lazy StompConnectionManager stompConnectionManager,  // 暂时解决循环依赖问题
            SubscriptionManager subscriptionManager
    ) {
        this.stompConnectionManager = stompConnectionManager;
        this.subscriptionManager = subscriptionManager;
    }
    
    /**
     * 消息发送前拦截处理
     * 
     * 在消息通过通道发送前进行拦截，根据STOMP命令类型执行不同的处理：
     * - CONNECT: 连接建立，发送连接成功消息
     * - SUBSCRIBE: 订阅权限验证
     * - SEND: 消息发送权限检查
     * - DISCONNECT: 连接断开清理
     * 
     * @param message STOMP消息
     * @param channel 消息通道
     * @return 处理后的消息，返回null表示阻止消息继续传递
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            
            if (command != null) {
                switch (command) {
                    case CONNECT:
                        handleConnect(accessor);
                        break;
                    case SUBSCRIBE:
                        handleSubscribe(accessor);
                        break;
                    case SEND:
                        handleSend(accessor);
                        break;
                    case UNSUBSCRIBE:
                        handleUnsubscribe(accessor);
                        break;
                    case DISCONNECT:
                        handleDisconnect(accessor);
                        break;
                    default:
                        // 其他命令不需要特殊处理
                        break;
                }
            }
        }
        
        return message;
    }
    
    /**
     * 消息发送后处理
     * 
     * 在消息发送完成后执行，主要用于日志记录和统计
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        if (!sent) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null && accessor.getCommand() != null) {
                log.warn("STOMP消息发送失败 - 命令: {}, 目的地: {}", 
                        accessor.getCommand(), accessor.getDestination());
            }
        }
    }
    
    /**
     * 处理CONNECT命令
     * 
     * 当客户端连接成功后：
     * 1. 设置StompPrincipal到STOMP会话中
     * 2. 通知StompConnectionManager注册连接
     * 3. 自动为其订阅默认主题
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        try {
            // 从会话属性中获取用户信息
            GatewayUserInfo userInfo = getUserInfoFromSession(accessor);
            if (userInfo == null) {
                log.error("连接处理失败：无法获取用户信息");
                return;
            }
            
            // 创建StompPrincipal并设置到STOMP会话中
            StompPrincipal stompPrincipal = new StompPrincipal(userInfo);
            accessor.setUser(stompPrincipal);
            
            String sessionId = accessor.getSessionId();
            String userId = userInfo.getUid().toString();
            String organizationId = userInfo.getOid().toString();
            
            log.debug("处理STOMP连接 - 会话ID: {}, 用户ID: {}, 组织ID: {}",
                    sessionId, userId, organizationId);
            log.debug("✅ StompPrincipal已设置到STOMP会话中");
            
            // 通知StompConnectionManager注册连接
            String clientInfo = getClientInfo(accessor);
            stompConnectionManager.registerConnection(sessionId, stompPrincipal, clientInfo);
            log.debug("✅ 连接已注册到StompConnectionManager");
            
            // 注意：欢迎消息已改为在客户端订阅 /user/queue/messages 后由监听器发送
            // 这样可以确保客户端能够接收到欢迎消息
            
        } catch (Exception e) {
            log.error("处理STOMP连接失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理SUBSCRIBE命令
     * 
     * 使用SubscriptionManager进行权限验证和订阅管理
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        try {
            String destination = accessor.getDestination();
            String sessionId = accessor.getSessionId();
            GatewayUserInfo userInfo = getUserInfo(accessor);
            
            if (userInfo == null) {
                throw new BaseException(ExceptionEnum.STOMP_ACCESS_DENIED, "未找到用户认证信息");
            }
            
            // 使用SubscriptionManager处理订阅
            SubscriptionResult result = subscriptionManager.handleSubscription(userInfo, destination, sessionId);
            
            if (!result.isSuccess()) {
                throw new BaseException(ExceptionEnum.STOMP_ACCESS_DENIED, result.getMessage());
            }
            
            log.debug("✅ 订阅处理成功 - 用户: {}, 主题: {}, 层次: {}",
                    userInfo.getUid(), destination, result.getSubscriptionLevel());
            
        } catch (Exception e) {
            log.error("订阅处理失败: {}", e.getMessage());
        }
    }

    /**
     * 处理取消订阅UNSUBSCRIBE命令
     * @param accessor
     */
    private void handleUnsubscribe(StompHeaderAccessor accessor) {
        try {
            String destination = accessor.getDestination();
            String sessionId = accessor.getSessionId();
            GatewayUserInfo userInfo = getUserInfo(accessor);

            SubscriptionResult subscriptionResult = subscriptionManager.handleUnsubscription(userInfo, destination, sessionId);

            if (!subscriptionResult.isSuccess()) {
                throw new BaseException(ExceptionEnum.STOMP_ACCESS_DENIED, subscriptionResult.getMessage());
            }

            log.debug("✅ 取消订阅处理成功 - 用户: {}, 主题: {}, 层次: {}",
                    userInfo.getUid(), destination, subscriptionResult.getSubscriptionLevel());

        } catch (Exception e) {
            log.error("取消订阅失败: {}", e.getMessage());
        }
    }
    
    /**
     * 处理SEND命令
     * 
     * 验证用户是否有权限发送消息到指定目的地
     */
    private void handleSend(StompHeaderAccessor accessor) {
        try {
            String destination = accessor.getDestination();
            GatewayUserInfo userInfo = getUserInfo(accessor);
            
            if (userInfo == null) {
                throw new BaseException(ExceptionEnum.STOMP_ACCESS_DENIED, "未找到用户认证信息");
            }
            
            if (!hasSendPermission(userInfo, destination)) {
                throw new BaseException(ExceptionEnum.STOMP_ACCESS_DENIED, "无权限发送消息到: " + destination);
            }
            
            log.debug("发送权限验证通过 - 用户: {}, 目的地: {}", userInfo.getUid(), destination);
            
        } catch (Exception e) {
            log.error("发送权限验证失败: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 处理DISCONNECT命令
     * 
     * 清理连接相关资源
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        try {
            String sessionId = accessor.getSessionId();
            GatewayUserInfo userInfo = getUserInfo(accessor);
            
            if (userInfo != null) {
                log.info("用户断开STOMP连接 - 用户ID: {}, 会话ID: {}", 
                        userInfo.getUid(), sessionId);
                
                // 清理用户会话订阅
                subscriptionManager.cleanupUserSessionSubscriptions(userInfo, sessionId);
                log.info("✅ 用户会话订阅已清理 - 用户ID: {}, 会话ID: {}", 
                        userInfo.getUid(), sessionId);
            }
            
            // 🔗 通知StompConnectionManager移除连接
            stompConnectionManager.removeConnection(sessionId);
            log.info("✅ 连接已从StompConnectionManager中移除 - sessionId: {}", sessionId);
            
        } catch (Exception e) {
            log.error("处理STOMP断开连接失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从STOMP访问器中获取用户信息
     * 
     * 优先从Principal中获取，如果没有则从会话属性中获取
     */
    private GatewayUserInfo getUserInfo(StompHeaderAccessor accessor) {
        // 首先尝试从Principal中获取（CONNECT之后应该有）
        Principal user = accessor.getUser();
        if (user instanceof StompPrincipal) {
            return ((StompPrincipal) user).getUserInfo();
        }
        
        // 如果Principal中没有，尝试从会话属性中获取（CONNECT时使用）
        return getUserInfoFromSession(accessor);
    }
    
    /**
     * 从会话属性中获取用户信息
     * 
     * 主要在CONNECT阶段使用，此时Principal还未设置
     */
    private GatewayUserInfo getUserInfoFromSession(StompHeaderAccessor accessor) {
        Object userInfo = accessor.getSessionAttributes().get(StompHandshakeInterceptor.USER_INFO_ATTRIBUTE);
        if (userInfo instanceof GatewayUserInfo) {
            return (GatewayUserInfo) userInfo;
        }
        
        return null;
    }
    
    /**
     * 获取客户端信息
     * 
     * @param accessor STOMP头访问器
     * @return 客户端信息字符串
     */
    private String getClientInfo(StompHeaderAccessor accessor) {
        try {
            // 尝试从会话属性或头信息中获取客户端信息
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                // 可以从握手时存储的信息中获取
                Object userAgent = sessionAttributes.get("user-agent");
                Object remoteAddress = sessionAttributes.get("remote-address");
                
                if (userAgent != null || remoteAddress != null) {
                    return String.format("UserAgent: %s, RemoteAddress: %s", 
                            userAgent != null ? userAgent.toString() : "unknown",
                            remoteAddress != null ? remoteAddress.toString() : "unknown");
                }
            }
            
            // 如果没有详细信息，返回会话ID作为标识
            return "SessionId: " + accessor.getSessionId();
            
        } catch (Exception e) {
            log.warn("获取客户端信息失败: {}", e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * 验证发送权限
     * 
     * 检查用户是否可以发送消息到指定目的地
     */
    private boolean hasSendPermission(GatewayUserInfo userInfo, String destination) {
        if (destination == null) {
            return false;
        }
        
        // 用户可以发送到应用处理器
        if (destination.startsWith("/app/")) {
            return true;
        }
        
        // 其他目的地暂时不允许直接发送
        log.warn("不允许的发送目的地 - 用户: {}, 目的地: {}", userInfo.getUid(), destination);
        return false;
    }

    
    /**
     * 从终端主题中提取终端ID
     */
    private String extractTerminalId(String destination) {
        // /topic/terminal/{terminalId}/status -> terminalId
        String[] parts = destination.split("/");
        if (parts.length >= 4 && "topic".equals(parts[1]) && "terminal".equals(parts[2])) {
            return parts[3];
        }
        return null;
    }
    
    

}

