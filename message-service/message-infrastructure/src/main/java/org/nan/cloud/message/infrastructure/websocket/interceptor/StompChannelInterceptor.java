package org.nan.cloud.message.infrastructure.websocket.interceptor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
 * 1. 连接建立时自动订阅用户和组织主题
 * 2. 订阅请求的权限验证
 * 3. 消息发送的权限检查
 * 4. 连接断开时的清理工作
 * 
 * 主要功能：
 * - CONNECT: 建立连接并自动订阅默认主题
 * - SUBSCRIBE: 验证订阅权限
 * - SEND: 验证消息发送权限
 * - DISCONNECT: 清理连接资源
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final StompConnectionManager stompConnectionManager;
    
    /**
     * 消息发送前拦截处理
     * 
     * 在消息通过通道发送前进行拦截，根据STOMP命令类型执行不同的处理：
     * - CONNECT: 连接建立，执行自动订阅
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
            
            log.info("处理STOMP连接 - 会话ID: {}, 用户ID: {}, 组织ID: {}", 
                    sessionId, userId, organizationId);
            log.info("✅ StompPrincipal已设置到STOMP会话中");
            
            // 通知StompConnectionManager注册连接
            String clientInfo = getClientInfo(accessor);
            stompConnectionManager.registerConnection(sessionId, stompPrincipal, clientInfo);
            log.info("✅ 连接已注册到StompConnectionManager");
            
            // 延迟执行自动订阅，确保连接完全建立
            scheduleAutoSubscription(sessionId, userId, organizationId);
            
        } catch (Exception e) {
            log.error("处理STOMP连接失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理SUBSCRIBE命令
     * 
     * 验证用户是否有权限订阅指定的主题
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        try {
            String destination = accessor.getDestination();
            GatewayUserInfo userInfo = getUserInfo(accessor);
            
            if (userInfo == null) {
                throw new BaseException(ExceptionEnum.STOMP_ACCESS_DENIED, "未找到用户认证信息");
            }
            
            if (!hasSubscriptionPermission(userInfo, destination)) {
                throw new BaseException(ExceptionEnum.STOMP_ACCESS_DENIED, "无权限订阅主题: " + destination);
            }
            
            log.info("订阅权限验证通过 - 用户: {}, 主题: {}", userInfo.getUid(), destination);
            
        } catch (Exception e) {
            log.error("订阅权限验证失败: {}", e.getMessage());
            throw e;
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
     * 验证订阅权限
     * 
     * 检查用户是否可以订阅指定的主题
     */
    private boolean hasSubscriptionPermission(GatewayUserInfo userInfo, String destination) {
        if (destination == null) {
            return false;
        }
        
        String userId = userInfo.getUid().toString();
        String organizationId = userInfo.getOid().toString();
        
        // 用户可以订阅自己的主题
        if (destination.startsWith("/topic/user/" + userId + "/")) {
            return true;
        }
        
        // 用户可以订阅所属组织的主题
        if (destination.startsWith("/topic/organization/" + organizationId + "/")) {
            return true;
        }
        
        // 用户可以订阅有权限访问的终端主题
        if (destination.startsWith("/topic/terminal/")) {
            String terminalId = extractTerminalId(destination);
            return userCanAccessTerminal(userInfo, terminalId);
        }
        
        // 系统主题需要特殊权限
        if (destination.startsWith("/topic/system/")) {
            return userInfo.getUserType() != null && userInfo.getUserType() == 1; // 管理员
        }
        
        log.warn("未知主题订阅请求 - 用户: {}, 主题: {}", userId, destination);
        return false;
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
     * 检查用户是否可以访问指定终端
     * 
     * TODO: 这里需要集成实际的权限检查逻辑
     */
    private boolean userCanAccessTerminal(GatewayUserInfo userInfo, String terminalId) {
        // 暂时返回true，实际应该检查用户对终端的访问权限
        // 可以通过调用Core-Service的权限API来验证
        return true;
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
    
    /**
     * 调度自动订阅
     * 
     * 延迟执行自动订阅，确保连接完全建立
     */
    private void scheduleAutoSubscription(String sessionId, String userId, String organizationId) {
        // 使用异步方式延迟执行，避免阻塞连接建立
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 延迟1秒确保连接稳定
                performAutoSubscription(sessionId, userId, organizationId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("自动订阅线程被中断");
            } catch (Exception e) {
                log.error("自动订阅执行失败: {}", e.getMessage(), e);
            }
        }, "auto-subscription-" + sessionId).start();
    }
    
    /**
     * 执行自动订阅
     * 
     * 为新连接的用户自动订阅默认主题
     */
    private void performAutoSubscription(String sessionId, String userId, String organizationId) {
        try {
            log.info("开始执行自动订阅 - 会话: {}, 用户: {}, 组织: {}", sessionId, userId, organizationId);
            
            // 构建默认订阅的主题列表
            String[] defaultTopics = {
                "/topic/user/" + userId + "/notifications",           // 用户个人通知
                "/topic/organization/" + organizationId + "/announcements" // 组织公告
            };
            
            // 发送自动订阅指令给客户端
            for (String topic : defaultTopics) {
                // 向特定会话发送订阅建议消息
                messagingTemplate.convertAndSendToUser(
                    sessionId, 
                    "/queue/auto-subscribe", 
                    new AutoSubscribeMessage(topic, "系统推荐订阅")
                );
                
                log.info("发送自动订阅建议 - 会话: {}, 主题: {}", sessionId, topic);
            }
            
            // 发送欢迎消息
            messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/welcome",
                new WelcomeMessage("欢迎连接到消息中心", "您已成功建立STOMP连接，可以开始接收实时消息了")
            );
            
            log.info("自动订阅执行完成 - 会话: {}", sessionId);
            
        } catch (Exception e) {
            log.error("执行自动订阅失败 - 会话: {}, 错误: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 自动订阅消息
     */
    public static class AutoSubscribeMessage {
        public final String topic;
        public final String description;
        
        public AutoSubscribeMessage(String topic, String description) {
            this.topic = topic;
            this.description = description;
        }
    }
    
    /**
     * 欢迎消息
     */
    public static class WelcomeMessage {
        public final String title;
        public final String content;
        public final long timestamp;
        
        public WelcomeMessage(String title, String content) {
            this.title = title;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
}

