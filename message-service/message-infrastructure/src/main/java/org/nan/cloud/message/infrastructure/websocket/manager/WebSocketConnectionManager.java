package org.nan.cloud.message.infrastructure.websocket.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.infrastructure.websocket.event.WebSocketHeartbeatEvent;
import org.nan.cloud.message.infrastructure.websocket.session.WebSocketSessionInfo;
import org.nan.cloud.message.infrastructure.websocket.session.WebSocketSessionStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket连接管理器
 * 
 * 负责管理所有WebSocket连接的生命周期，包括连接建立、断开、消息发送等。
 * 使用线程安全的集合来存储连接信息，支持并发操作。
 * 
 * 主要功能：
 * 1. 连接管理：建立、维护、清理WebSocket连接
 * 2. 消息推送：向指定用户或组织推送消息
 * 3. 在线状态：维护用户在线状态信息
 * 4. 连接监控：统计连接数量和状态
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketConnectionManager {
    
    /**
     * JSON序列化工具
     * 用于将消息对象转换为JSON字符串进行传输
     */
    private final ObjectMapper objectMapper;
    
    /**
     * WebSocket会话存储器
     * 用于Redis持久化会话信息
     */
    private final WebSocketSessionStore sessionStore;
    
    /**
     * 应用事件发布器
     * 用于发布WebSocket相关事件
     */
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 用户连接映射表
     * Key: 用户ID，Value: 该用户的所有WebSocket连接（支持多设备登录）
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, Set<WebSocketSession>> userConnections = new ConcurrentHashMap<>();
    
    /**
     * 组织连接映射表  
     * Key: 组织ID，Value: 该组织下所有用户的连接集合
     * 用于组织内广播消息
     */
    private final Map<String, Set<WebSocketSession>> organizationConnections = new ConcurrentHashMap<>();
    
    /**
     * 连接用户映射表
     * Key: WebSocket连接ID，Value: 用户ID
     * 用于连接断开时快速找到对应用户
     */
    private final Map<String, String> sessionUserMapping = new ConcurrentHashMap<>();
    
    /**
     * 连接组织映射表
     * Key: WebSocket连接ID，Value: 组织ID
     * 用于连接断开时快速找到对应组织
     */
    private final Map<String, String> sessionOrganizationMapping = new ConcurrentHashMap<>();
    
    /**
     * 连接建立时间映射表
     * Key: WebSocket连接ID，Value: 连接建立时间
     * 用于连接时长统计和超时检测
     */
    private final Map<String, LocalDateTime> sessionConnectTime = new ConcurrentHashMap<>();
    
    /**
     * 当前服务节点标识
     * 用于分布式环境下的节点识别
     */
    private final String nodeId = generateNodeId();
    
    /**
     * 添加用户连接
     * 当用户建立WebSocket连接时调用此方法
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param session WebSocket连接会话
     */
    public void addConnection(String userId, String organizationId, WebSocketSession session) {
        addConnection(userId, organizationId, session, null);
    }
    
    /**
     * 添加用户连接（带token）
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param session WebSocket连接会话
     * @param token 用户认证令牌
     */
    public void addConnection(String userId, String organizationId, WebSocketSession session, String token) {
        try {
            String sessionId = session.getId();
            LocalDateTime connectTime = LocalDateTime.now();
            
            log.info("添加WebSocket连接 - 用户: {}, 组织: {}, 连接ID: {}", userId, organizationId, sessionId);
            
            // 1. 将连接添加到用户连接集合中
            userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            
            // 2. 将连接添加到组织连接集合中
            organizationConnections.computeIfAbsent(organizationId, k -> new CopyOnWriteArraySet<>()).add(session);
            
            // 3. 建立连接到用户和组织的映射关系
            sessionUserMapping.put(sessionId, userId);
            sessionOrganizationMapping.put(sessionId, organizationId);
            sessionConnectTime.put(sessionId, connectTime);
            
            // 4. 创建并保存会话信息到Redis
            WebSocketSessionInfo sessionInfo = buildSessionInfo(sessionId, userId, organizationId, token, session, connectTime);
            sessionStore.saveSession(sessionInfo);
            
            // 5. 发布连接注册事件
            eventPublisher.publishEvent(new WebSocketHeartbeatEvent(this, sessionId, 
                    WebSocketHeartbeatEvent.HeartbeatEventType.REGISTER));
            
            // 6. 记录连接统计信息
            logConnectionStats();
            
            // 7. 发送连接成功确认消息
            sendConnectionWelcomeMessage(session, userId);
            
        } catch (Exception e) {
            log.error("添加WebSocket连接失败 - 用户: {}, 错误: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 移除用户连接
     * 当WebSocket连接断开时调用此方法清理相关数据
     * 
     * @param session WebSocket连接会话
     * @param status 连接关闭状态
     */
    public void removeConnection(WebSocketSession session, CloseStatus status) {
        try {
            String sessionId = session.getId();
            String userId = sessionUserMapping.get(sessionId);
            String organizationId = sessionOrganizationMapping.get(sessionId);
            
            log.info("移除WebSocket连接 - 用户: {}, 组织: {}, 连接ID: {}, 关闭原因: {}", 
                    userId, organizationId, sessionId, status.toString());
            
            // 1. 从用户连接集合中移除
            if (userId != null) {
                Set<WebSocketSession> sessions = userConnections.get(userId);
                if (sessions != null) {
                    sessions.remove(session);
                    // 如果用户没有其他连接，移除用户记录
                    if (sessions.isEmpty()) {
                        userConnections.remove(userId);
                        log.info("用户 {} 已完全离线", userId);
                    }
                }
            }
            
            // 2. 从组织连接集合中移除
            if (organizationId != null) {
                Set<WebSocketSession> sessions = organizationConnections.get(organizationId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        organizationConnections.remove(organizationId);
                    }
                }
            }
            
            // 3. 清理映射关系
            sessionUserMapping.remove(sessionId);
            sessionOrganizationMapping.remove(sessionId);
            sessionConnectTime.remove(sessionId);
            
            // 4. 发布连接移除事件
            eventPublisher.publishEvent(new WebSocketHeartbeatEvent(this, sessionId, 
                    WebSocketHeartbeatEvent.HeartbeatEventType.UNREGISTER));
            
            // 5. 从Redis会话存储中移除
            sessionStore.removeSession(sessionId);
            
            // 6. 记录连接统计信息
            logConnectionStats();
            
        } catch (Exception e) {
            log.error("移除WebSocket连接失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 向指定用户发送消息
     * 如果用户有多个连接（多设备登录），会向所有连接发送消息
     * 
     * @param userId 目标用户ID
     * @param message 要发送的消息
     * @return 发送成功的连接数量
     */
    public int sendMessageToUser(String userId, WebSocketMessage message) {
        Set<WebSocketSession> sessions = userConnections.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.warn("用户 {} 未在线，无法发送消息: {}", userId, message.getTitle());
            return 0;
        }
        
        log.info("向用户 {} 发送消息: {} (连接数: {})", userId, message.getTitle(), sessions.size());
        
        int successCount = 0;
        // 遍历用户的所有连接，逐个发送消息
        for (WebSocketSession session : sessions) {
            if (sendMessage(session, message)) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    /**
     * 向指定组织广播消息
     * 向组织内所有在线用户发送消息
     * 
     * @param organizationId 目标组织ID
     * @param message 要广播的消息
     * @return 发送成功的连接数量
     */
    public int broadcastToOrganization(String organizationId, WebSocketMessage message) {
        Set<WebSocketSession> sessions = organizationConnections.get(organizationId);
        if (sessions == null || sessions.isEmpty()) {
            log.warn("组织 {} 无在线用户，无法广播消息: {}", organizationId, message.getTitle());
            return 0;
        }
        
        log.info("向组织 {} 广播消息: {} (连接数: {})", organizationId, message.getTitle(), sessions.size());
        
        int successCount = 0;
        // 遍历组织的所有连接，逐个发送消息
        for (WebSocketSession session : sessions) {
            if (sendMessage(session, message)) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    /**
     * 向所有在线用户广播消息
     * 用于系统级重要通知
     * 
     * @param message 要广播的消息
     * @return 发送成功的连接数量
     */
    public int broadcastToAll(WebSocketMessage message) {
        log.info("向所有用户广播消息: {}", message.getTitle());
        
        int successCount = 0;
        // 遍历所有用户的连接
        for (Set<WebSocketSession> sessions : userConnections.values()) {
            for (WebSocketSession session : sessions) {
                if (sendMessage(session, message)) {
                    successCount++;
                }
            }
        }
        
        return successCount;
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return true表示用户在线，false表示离线
     */
    public boolean isUserOnline(String userId) {
        Set<WebSocketSession> sessions = userConnections.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
    
    /**
     * 获取用户连接数量
     * 
     * @param userId 用户ID
     * @return 用户的连接数量（支持多设备）
     */
    public int getUserConnectionCount(String userId) {
        Set<WebSocketSession> sessions = userConnections.get(userId);
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * 获取总连接数
     * 
     * @return 当前总连接数
     */
    public int getTotalConnectionCount() {
        return sessionUserMapping.size();
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
     * 向单个连接发送消息的内部方法
     * 
     * @param session WebSocket连接会话
     * @param message 要发送的消息
     * @return true表示发送成功，false表示发送失败
     */
    private boolean sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            // 检查连接是否有效
            if (!session.isOpen()) {
                log.warn("WebSocket连接已关闭，无法发送消息 - 连接ID: {}", session.getId());
                return false;
            }
            
            // 将消息对象转换为JSON字符串
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            // 通过WebSocket发送文本消息
            session.sendMessage(new TextMessage(jsonMessage));
            
            log.debug("消息发送成功 - 连接ID: {}, 消息ID: {}", session.getId(), message.getMessageId());
            return true;
            
        } catch (IOException e) {
            log.error("发送WebSocket消息失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("发送WebSocket消息异常 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送连接欢迎消息
     * 
     * @param session WebSocket连接会话
     * @param userId 用户ID
     */
    private void sendConnectionWelcomeMessage(WebSocketSession session, String userId) {
        WebSocketMessage welcomeMessage = WebSocketMessage.builder()
                .messageId("welcome-" + System.currentTimeMillis())
                .type(org.nan.cloud.message.api.enums.MessageType.SYSTEM_NOTIFICATION)
                .title("连接成功")
                .content("WebSocket连接已建立，您可以接收实时消息了")
                .priority(org.nan.cloud.message.api.enums.Priority.LOW)
                .timestamp(LocalDateTime.now())
                .requireAck(false)
                .build();
        
        sendMessage(session, welcomeMessage);
    }
    
    /**
     * 记录连接统计信息
     */
    private void logConnectionStats() {
        log.info("WebSocket连接统计 - 总连接数: {}, 在线用户数: {}, 活跃组织数: {}", 
                getTotalConnectionCount(), getOnlineUserCount(), organizationConnections.size());
    }
    
    /**
     * 构建会话信息对象
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param token 认证令牌
     * @param session WebSocket会话
     * @param connectTime 连接时间
     * @return 会话信息对象
     */
    private WebSocketSessionInfo buildSessionInfo(String sessionId, String userId, String organizationId, 
                                                String token, WebSocketSession session, LocalDateTime connectTime) {
        // 获取客户端IP地址
        String clientIp = getClientIp(session);
        
        // 获取User-Agent信息
        String userAgent = getUserAgent(session);
        
        // 检测设备类型
        WebSocketSessionInfo.DeviceType deviceType = detectDeviceType(userAgent);
        
        return WebSocketSessionInfo.builder()
                .sessionId(sessionId)
                .userId(userId)
                .organizationId(organizationId)
                .token(token)
                .connectTime(connectTime)
                .lastActivityTime(connectTime)
                .lastHeartbeatTime(connectTime)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .status(WebSocketSessionInfo.ConnectionStatus.CONNECTED)
                .nodeId(nodeId)
                .deviceType(deviceType)
                .deviceId(generateDeviceId(userAgent, clientIp))
                .protocolVersion("1.0")
                .heartbeatInterval(30)
                .retryCount(0)
                .build();
    }
    
    /**
     * 获取客户端IP地址
     * 
     * @param session WebSocket会话
     * @return 客户端IP地址
     */
    private String getClientIp(WebSocketSession session) {
        try {
            InetSocketAddress remoteAddress = session.getRemoteAddress();
            if (remoteAddress != null) {
                return remoteAddress.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            log.warn("获取客户端IP失败: {}", e.getMessage());
        }
        return "unknown";
    }
    
    /**
     * 获取User-Agent信息
     * 
     * @param session WebSocket会话
     * @return User-Agent字符串
     */
    private String getUserAgent(WebSocketSession session) {
        try {
            return session.getHandshakeHeaders().getFirst("User-Agent");
        } catch (Exception e) {
            log.warn("获取User-Agent失败: {}", e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * 检测设备类型
     * 
     * @param userAgent User-Agent字符串
     * @return 设备类型
     */
    private WebSocketSessionInfo.DeviceType detectDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return WebSocketSessionInfo.DeviceType.UNKNOWN;
        }
        
        String ua = userAgent.toLowerCase();
        
        // 移动端检测
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || 
            ua.contains("ipad") || ua.contains("ipod")) {
            if (ua.contains("ipad") || ua.contains("tablet")) {
                return WebSocketSessionInfo.DeviceType.TABLET;
            }
            return WebSocketSessionInfo.DeviceType.MOBILE;
        }
        
        // 桌面端检测
        if (ua.contains("electron") || ua.contains("desktop")) {
            return WebSocketSessionInfo.DeviceType.DESKTOP;
        }
        
        // 默认认为是Web浏览器
        return WebSocketSessionInfo.DeviceType.WEB;
    }
    
    /**
     * 生成设备标识
     * 
     * @param userAgent User-Agent字符串
     * @param clientIp 客户端IP
     * @return 设备标识
     */
    private String generateDeviceId(String userAgent, String clientIp) {
        try {
            String input = (userAgent != null ? userAgent : "unknown") + "_" + clientIp;
            return "device_" + Math.abs(input.hashCode());
        } catch (Exception e) {
            return "device_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 生成节点ID
     * 
     * @return 节点ID
     */
    private String generateNodeId() {
        try {
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            return "node_" + hostName + "_" + System.currentTimeMillis();
        } catch (Exception e) {
            return "node_unknown_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 更新会话活跃时间
     * 当会话有活动时调用此方法
     * 
     * @param sessionId 会话ID
     */
    public void updateSessionActivity(String sessionId) {
        sessionStore.updateSessionActivity(sessionId);
    }
    
    /**
     * 处理心跳消息
     * 当收到客户端心跳时调用此方法
     * 
     * @param sessionId 会话ID
     */
    public void handleHeartbeat(String sessionId) {
        // 发布心跳更新事件
        eventPublisher.publishEvent(new WebSocketHeartbeatEvent(this, sessionId, 
                WebSocketHeartbeatEvent.HeartbeatEventType.UPDATE));
        updateSessionActivity(sessionId);
    }
}