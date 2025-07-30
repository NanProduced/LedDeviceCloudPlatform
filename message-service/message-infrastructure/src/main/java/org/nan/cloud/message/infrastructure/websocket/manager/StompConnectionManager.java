package org.nan.cloud.message.infrastructure.websocket.manager;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.interceptor.StompChannelInterceptor;
import org.nan.cloud.message.infrastructure.websocket.interceptor.StompPrincipal;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.nan.cloud.message.infrastructure.websocket.subscription.AutoSubscriptionResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import static org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic.USER_NOTIFICATION_DESTINATION;

/**
 * STOMP连接管理器
 * 
 * 职责：
 * 1. 管理STOMP连接的生命周期（建立、维护、断开）
 * 2. 跟踪用户的STOMP会话状态和元数据
 * 3. 支持多设备登录（一个用户多个STOMP连接）
 * 4. 提供连接统计和监控能力
 * 5. 为消息推送提供基础支持
 * 6. 与订阅管理器集成
 * 
 * 区别于原WebSocketConnectionManager：
 * - 基于STOMP Principal而不是WebSocketSession
 * - 重点是用户会话管理而不是底层连接管理
 * - 消息推送通过SimpMessagingTemplate
 * - 集成Topic订阅关系管理
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompConnectionManager {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 用户连接映射表
     * Key: 用户ID，Value: 该用户的所有STOMP会话信息（支持多设备登录）
     */
    private final Map<String, Set<StompSessionInfo>> userConnections = new ConcurrentHashMap<>();
    
    /**
     * 组织连接映射表
     * Key: 组织ID，Value: 该组织下所有在线用户ID集合
     */
    private final Map<Long, Set<String>> organizationConnections = new ConcurrentHashMap<>();
    
    /**
     * 会话ID到会话信息的映射表
     * Key: STOMP会话ID，Value: 会话详细信息
     */
    private final Map<String, StompSessionInfo> sessionMapping = new ConcurrentHashMap<>();
    
    /**
     * 会话ID到用户ID的快速映射表
     * Key: STOMP会话ID，Value: 用户ID
     * 用于连接断开时快速查找对应用户
     */
    private final Map<String, String> sessionUserMapping = new ConcurrentHashMap<>();
    
    /**
     * 连接统计信息
     */
    private final AtomicLong totalConnectionCount = new AtomicLong(0);
    private final AtomicLong totalConnectedTime = new AtomicLong(0);
    
    /**
     * 当前服务节点标识
     * 用于分布式环境下的节点识别
     */
    private final String nodeId = generateNodeId();
    
    /**
     * 注册新的STOMP连接
     * 当用户成功建立STOMP连接时调用
     * 
     * @param sessionId STOMP会话ID
     * @param principal 用户认证主体（包含GatewayUserInfo）
     * @param clientInfo 客户端信息（User-Agent等）
     */
    public void registerConnection(String sessionId, Principal principal, String clientInfo) {
        try {
            // 1. 解析用户信息
            GatewayUserInfo userInfo = extractUserInfo(principal);
            if (userInfo == null) {
                log.warn("无法从Principal中提取用户信息，跳过连接注册 - sessionId: {}", sessionId);
                return;
            }
            
            String userId = userInfo.getUserIdString();
            Long organizationId = userInfo.getOid();
            
            log.info("注册STOMP连接 - 用户: {}, 组织: {}, 会话: {}", userId, organizationId, sessionId);
            
            // 2. 创建会话信息
            StompSessionInfo sessionInfo = StompSessionInfo.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .organizationId(organizationId)
                    .connectTime(LocalDateTime.now())
                    .clientInfo(clientInfo)
                    .userType(userInfo.getUserType())
                    .ugid(userInfo.getUgid())
                    .build();
            
            // 3. 更新各种映射关系
            userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(sessionInfo);
            organizationConnections.computeIfAbsent(organizationId, k -> new CopyOnWriteArraySet<>()).add(userId);
            sessionMapping.put(sessionId, sessionInfo);
            sessionUserMapping.put(sessionId, userId);
            
            // 4. 更新统计信息
            totalConnectionCount.incrementAndGet();
            
            // 5. TODO: 发布连接建立事件
            // eventPublisher.publishEvent(new StompConnectionEstablishedEvent(sessionInfo));
            
            // 6. TODO: 初始化用户默认订阅（与SubscriptionManager集成）
            // subscriptionManager.initializeDefaultSubscriptions(userId, sessionId);
            
            // 7. 发送连接成功确认消息
            sendConnectionWelcomeMessage(userId, sessionId);
            
            // 8. 记录连接统计信息
            logConnectionStats();
            
        } catch (Exception e) {
            log.error("注册STOMP连接失败 - sessionId: {}, 错误: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 移除STOMP连接
     * 当用户断开STOMP连接时调用
     * 
     * @param sessionId STOMP会话ID
     */
    public void removeConnection(String sessionId) {
        try {
            // 1. 获取会话信息
            StompSessionInfo sessionInfo = sessionMapping.get(sessionId);
            if (sessionInfo == null) {
                log.warn("尝试移除不存在的STOMP连接 - sessionId: {}", sessionId);
                return;
            }
            
            String userId = sessionInfo.getUserId();
            Long organizationId = sessionInfo.getOrganizationId();
            
            log.info("移除STOMP连接 - 用户: {}, 组织: {}, 会话: {}", userId, organizationId, sessionId);
            
            // 2. 从用户连接集合中移除
            Set<StompSessionInfo> userSessions = userConnections.get(userId);
            if (userSessions != null) {
                userSessions.remove(sessionInfo);
                // 如果用户没有其他连接，移除用户记录
                if (userSessions.isEmpty()) {
                    userConnections.remove(userId);
                    log.info("用户 {} 已完全离线", userId);
                    
                    // 从组织连接中移除用户
                    Set<String> orgUsers = organizationConnections.get(organizationId);
                    if (orgUsers != null) {
                        orgUsers.remove(userId);
                        if (orgUsers.isEmpty()) {
                            organizationConnections.remove(organizationId);
                        }
                    }
                }
            }
            
            // 3. 清理映射关系
            sessionMapping.remove(sessionId);
            sessionUserMapping.remove(sessionId);
            
            // 4. 更新统计信息
            totalConnectionCount.decrementAndGet();
            LocalDateTime connectTime = sessionInfo.getConnectTime();
            if (connectTime != null) {
                long connectionDuration = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8)) - connectTime.toEpochSecond(ZoneOffset.ofHours(8));
                totalConnectedTime.addAndGet(connectionDuration);
            }
            
            // 5. TODO: 清理用户订阅（与SubscriptionManager集成）
            // subscriptionManager.cleanupUserSubscriptions(userId, sessionId);
            
            // 6. TODO: 发布连接断开事件
            // eventPublisher.publishEvent(new StompConnectionDisconnectedEvent(sessionInfo));
            
            // 7. 记录连接统计信息
            logConnectionStats();
            
        } catch (Exception e) {
            log.error("移除STOMP连接失败 - sessionId: {}, 错误: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return true表示用户在线，false表示离线
     */
    public boolean isUserOnline(String userId) {
        Set<StompSessionInfo> sessions = userConnections.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
    
    /**
     * 获取用户的所有STOMP会话
     * 
     * @param userId 用户ID
     * @return 用户的会话信息集合，如果用户离线则返回空集合
     */
    public Set<StompSessionInfo> getUserSessions(String userId) {
        Set<StompSessionInfo> sessions = userConnections.get(userId);
        return sessions != null ? new HashSet<>(sessions) : Collections.emptySet();
    }
    
    /**
     * 获取用户连接数量
     * 
     * @param userId 用户ID
     * @return 用户的连接数量（支持多设备）
     */
    public int getUserConnectionCount(String userId) {
        Set<StompSessionInfo> sessions = userConnections.get(userId);
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * 获取组织内的在线用户列表
     * 
     * @param organizationId 组织ID
     * @return 在线用户ID集合
     */
    public Set<String> getOrganizationOnlineUsers(Long organizationId) {
        Set<String> users = organizationConnections.get(organizationId);
        return users != null ? new HashSet<>(users) : Collections.emptySet();
    }
    
    /**
     * 获取总连接数
     * 
     * @return 当前总STOMP连接数
     */
    public long getTotalConnectionCount() {
        return totalConnectionCount.get();
    }
    
    /**
     * 获取在线用户数
     * 
     * @return 当前在线用户数
     */
    public int getOnlineUserCount() {
        return userConnections.size();
    }
    
    /**
     * 获取在线组织数
     * 
     * @return 当前有用户在线的组织数量
     */
    public int getOnlineOrganizationCount() {
        return organizationConnections.size();
    }
    
    /**
     * 向指定用户发送STOMP消息
     * 如果用户有多个连接（多设备登录），会向所有连接发送消息
     * 
     * @param userId 目标用户ID
     * @param destination 消息目标（如 "/queue/notifications"）
     * @param message 要发送的消息
     * @return 是否发送成功
     */
    public boolean sendToUser(String userId, String destination, Object message) {
        try {
            if (!isUserOnline(userId)) {
                log.debug("用户 {} 不在线，无法发送STOMP消息", userId);
                return false;
            }
            
            // 使用SimpMessagingTemplate发送给用户
            messagingTemplate.convertAndSendToUser(userId, destination, message);
            
            log.debug("向用户 {} 发送STOMP消息成功 - destination: {}", userId, destination);
            return true;
            
        } catch (Exception e) {
            log.error("向用户 {} 发送STOMP消息失败 - destination: {}, 错误: {}", 
                    userId, destination, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 向指定Topic发送消息
     * 
     * @param destination Topic目标路径
     * @param message 要发送的消息
     * @return 是否发送成功
     */
    public boolean sendToTopic(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("向Topic发送STOMP消息成功 - destination: {}", destination);
            return true;
            
        } catch (Exception e) {
            log.error("向Topic发送STOMP消息失败 - destination: {}, 错误: {}", 
                    destination, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 向组织内所有在线用户广播消息
     * 
     * @param organizationId 组织ID
     * @param destination 消息目标
     * @param message 消息内容
     * @return 成功发送的用户数量
     */
    public int broadcastToOrganization(Long organizationId, String destination, Object message) {
        Set<String> onlineUsers = getOrganizationOnlineUsers(organizationId);
        if (onlineUsers.isEmpty()) {
            log.debug("组织 {} 无在线用户，跳过广播", organizationId);
            return 0;
        }
        
        int successCount = 0;
        for (String userId : onlineUsers) {
            if (sendToUser(userId, destination, message)) {
                successCount++;
            }
        }
        
        log.info("向组织 {} 广播STOMP消息完成 - 目标用户: {}, 成功发送: {}", 
                organizationId, onlineUsers.size(), successCount);
        
        return successCount;
    }
    
    /**
     * 从Principal中提取GatewayUserInfo
     * 
     * @param principal 用户认证主体
     * @return Gateway用户信息，提取失败时返回null
     */
    private GatewayUserInfo extractUserInfo(Principal principal) {
        if (principal == null) {
            log.warn("Principal为null，无法提取用户信息");
            return null;
        }
        
        // 检查是否为StompPrincipal类型
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
        
        // 如果不是StompPrincipal类型，记录警告并返回null
        log.warn("Principal不是StompPrincipal类型，无法提取GatewayUserInfo - type: {}, name: {}", 
                principal.getClass().getSimpleName(), principal.getName());
        
        return null;
    }
    
    /**
     * 记录连接统计信息
     */
    private void logConnectionStats() {
        log.info("STOMP连接统计 - 总连接数: {}, 在线用户数: {}, 活跃组织数: {}, 节点: {}", 
                getTotalConnectionCount(), getOnlineUserCount(), getOnlineOrganizationCount(), nodeId);
    }
    
    /**
     * 生成节点ID
     * 
     * @return 节点ID
     */
    private String generateNodeId() {
        try {
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            return "stomp-node-" + hostName + "-" + System.currentTimeMillis() % 10000;
        } catch (Exception e) {
            return "stomp-node-unknown-" + System.currentTimeMillis() % 10000;
        }
    }
    
    /**
     * 发送连接欢迎消息
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    private void sendConnectionWelcomeMessage(String userId, String sessionId) {
        try {
            // 创建欢迎消息
            CommonStompMessage welcomeMessage = new CommonStompMessage();
            welcomeMessage.setMessageId("welcome-" + System.currentTimeMillis());
            welcomeMessage.setTimestamp(LocalDateTime.now());
            welcomeMessage.setMessageType(StompMessageTypes.CONNECTION_STATUS);
            welcomeMessage.setMessage("WebSocket STOMP连接已建立");
            
            // 设置消息来源
            CommonStompMessage.Source source = new CommonStompMessage.Source();
            source.setServiceId("message-service");
            source.setResourceType("connection");
            source.setResourceId(sessionId);
            welcomeMessage.setSource(source);
            
            // 设置消息目标
            CommonStompMessage.Target target = new CommonStompMessage.Target();
            target.setTargetType("user");
            target.setUids(List.of(Long.valueOf(userId)));
            welcomeMessage.setTarget(target);
            
            // 设置元数据
            CommonStompMessage.Metadata metadata = new CommonStompMessage.Metadata();
            metadata.setPriority(Priority.LOW);
            metadata.setPersistent(false);
            metadata.setRequireAck(false);
            welcomeMessage.setMetadata(metadata);
            
            // 发送欢迎消息到用户的通知队列
            sendToUser(userId, USER_NOTIFICATION_DESTINATION, welcomeMessage);
            
            log.debug("为用户 {} 发送连接欢迎消息成功 - sessionId: {}", userId, sessionId);
            
        } catch (Exception e) {
            log.error("发送连接欢迎消息失败 - userId: {}, sessionId: {}, 错误: {}", 
                    userId, sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 更新会话活跃时间
     * 当会话有活动时调用此方法
     * 
     * @param sessionId 会话ID
     */
    public void updateSessionActivity(String sessionId) {
        StompSessionInfo sessionInfo = sessionMapping.get(sessionId);
        if (sessionInfo != null) {
            // 更新会话最后活跃时间
            sessionInfo.updateLastActiveTime();
            log.debug("更新会话活跃时间 - sessionId: {}, lastActiveTime: {}", 
                    sessionId, sessionInfo.getLastActiveTime());
        } else {
            log.debug("尝试更新不存在的会话活跃时间 - sessionId: {}", sessionId);
        }
    }
    
    /**
     * 处理心跳消息
     * 当收到客户端心跳时调用此方法
     * 
     * @param sessionId 会话ID
     */
    public void handleHeartbeat(String sessionId) {
        updateSessionActivity(sessionId);
        
        // TODO: 发布心跳事件
        // eventPublisher.publishEvent(new StompHeartbeatEvent(sessionId));
        
        log.debug("处理STOMP心跳 - sessionId: {}", sessionId);
    }
    
    /**
     * 获取会话详细信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息，如果不存在则返回null
     */
    public StompSessionInfo getSessionInfo(String sessionId) {
        return sessionMapping.get(sessionId);
    }
    
    /**
     * 获取平均连接时长（秒）
     * 
     * @return 平均连接时长
     */
    public long getAverageConnectionDuration() {
        long totalCount = totalConnectionCount.get();
        return totalCount > 0 ? totalConnectedTime.get() / totalCount : 0;
    }
    
    /**
     * 获取所有在线用户ID列表
     * 
     * @return 在线用户ID列表
     */
    public List<String> getAllOnlineUserIds() {
        return new ArrayList<>(userConnections.keySet());
    }
    
    /**
     * 向指定用户发送STOMP消息（CommonStompMessage格式）
     * 如果用户有多个连接（多设备登录），会向所有连接发送消息
     * 
     * @param userId 目标用户ID
     * @param message 要发送的CommonStompMessage消息
     * @return 是否发送成功
     */
    public boolean sendToUser(String userId, CommonStompMessage message) {
        return sendToUser(userId, USER_NOTIFICATION_DESTINATION, message);
    }
    
    /**
     * 向组织内所有在线用户广播消息（CommonStompMessage格式）
     * 
     * @param organizationId 组织ID
     * @param message CommonStompMessage消息内容
     * @return 成功发送的用户数量
     */
    public int broadcastToOrganization(Long organizationId, CommonStompMessage message) {
        return broadcastToOrganization(organizationId, USER_NOTIFICATION_DESTINATION, message);
    }

    /**
     * 发送欢迎消息
     *
     * 向用户发送连接成功和自动订阅状态的欢迎消息
     */
    public void sendWelcomeMessage(String sessionId, AutoSubscriptionResult result) {
        try {
            // 构建欢迎消息内容
            String welcomeContent = String.format(
                    "🎉 欢迎连接到LED设备云平台消息中心！\\n" +
                            "✅ STOMP连接已建立\\n" +
                            "📡 自动订阅完成：成功 %d 个主题\\n" +
                            "💡 您现在可以接收实时消息推送了",
                    result.getSuccessfulSubscriptions() != null ? result.getSuccessfulSubscriptions().size() : 0
            );

            WelcomeMessage welcomeMessage = WelcomeMessage.builder()
                    .title("连接成功")
                    .content(welcomeContent)
                    .subscriptionSummary(result.getSummary())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 发送欢迎消息到用户的欢迎队列
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/welcome",
                    welcomeMessage
            );

            log.debug("✅ 欢迎消息已发送 - 会话: {}", sessionId);

        } catch (Exception e) {
            log.warn("发送欢迎消息失败 - 会话: {}, 错误: {}", sessionId, e.getMessage());
        }
    }

    /**
     * 欢迎消息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WelcomeMessage {
        private String title;
        private String content;
        private String subscriptionSummary;
        private long timestamp;
    }
}