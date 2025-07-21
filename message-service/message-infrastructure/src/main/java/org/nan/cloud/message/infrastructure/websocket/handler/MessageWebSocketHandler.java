package org.nan.cloud.message.infrastructure.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// 移除错误的import，WebSocketMessage应该来自Spring框架
import org.nan.cloud.message.infrastructure.websocket.manager.WebSocketConnectionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket消息处理器
 * 
 * 这是WebSocket连接的核心处理类，负责处理客户端的连接建立、断开、消息接收等事件。
 * 实现Spring WebSocket的WebSocketHandler接口，定义WebSocket生命周期的处理逻辑。
 * 
 * 主要职责：
 * 1. 连接建立：验证用户身份，建立连接映射关系
 * 2. 连接断开：清理连接资源，更新在线状态
 * 3. 消息处理：接收客户端消息，进行相应处理
 * 4. 错误处理：处理连接异常，记录错误日志
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler implements WebSocketHandler {
    
    /**
     * WebSocket连接管理器
     * 负责管理所有连接的生命周期和消息分发
     */
    private final WebSocketConnectionManager connectionManager;
    
    /**
     * JSON序列化工具
     * 用于解析客户端发送的JSON消息
     */
    private final ObjectMapper objectMapper;
    
    /**
     * WebSocket连接建立后的回调方法
     * 
     * 当客户端成功建立WebSocket连接时，Spring会调用此方法。
     * 在这里我们需要：
     * 1. 从连接URL中提取用户信息
     * 2. 验证用户权限
     * 3. 将连接注册到连接管理器中
     * 
     * @param session WebSocket会话对象，代表一个客户端连接
     * @throws Exception 连接建立过程中的异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            log.info("===== WebSocket连接建立开始 =====");
            log.info("连接ID: {}", session.getId());
            log.info("远程地址: {}", session.getRemoteAddress());
            log.info("连接URI: {}", session.getUri());
            
            // 1. 从连接URI中提取用户信息
            UserConnectionInfo userInfo = extractUserInfo(session);
            
            if (userInfo == null) {
                log.error("❌ 用户信息提取失败，关闭连接 - 连接ID: {}", session.getId());
                session.close(CloseStatus.BAD_DATA.withReason("缺少用户信息"));
                return;
            }
            
            log.info("✅ 用户信息提取成功 - 用户ID: {}, 组织ID: {}, Token: {}", 
                    userInfo.getUserId(), userInfo.getOrganizationId(), 
                    userInfo.getToken() != null ? "有" : "无");
            
            // 2. 验证用户权限
            boolean permissionValid = validateUserPermission(userInfo);
            log.info("权限验证结果: {}", permissionValid ? "通过" : "失败");
            
            if (!permissionValid) {
                log.error("❌ 权限验证失败 - 用户ID: {}, 连接ID: {}", 
                        userInfo.getUserId(), session.getId());
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("权限验证失败"));
                return;
            }
            
            // 3. 将连接注册到连接管理器
            log.info("开始注册连接到连接管理器...");
            connectionManager.addConnection(userInfo.getUserId(), userInfo.getOrganizationId(), session);
            
            log.info("🎉 WebSocket连接建立成功！");
            log.info("用户: {}, 组织: {}, 连接ID: {}", 
                    userInfo.getUserId(), userInfo.getOrganizationId(), session.getId());
            log.info("===== WebSocket连接建立完成 =====");
            
        } catch (Exception e) {
            log.error("💥 WebSocket连接建立过程中发生异常！");
            log.error("连接ID: {}, 错误类型: {}, 错误信息: {}", 
                    session.getId(), e.getClass().getSimpleName(), e.getMessage());
            log.error("完整异常堆栈:", e);
            
            try {
                // 发生异常时关闭连接
                session.close(CloseStatus.SERVER_ERROR.withReason("服务器内部错误: " + e.getMessage()));
            } catch (Exception closeEx) {
                log.error("关闭连接时也发生异常: {}", closeEx.getMessage());
            }
        }
    }
    
    /**
     * 接收到WebSocket消息时的回调方法
     * 
     * 当客户端向服务器发送消息时，Spring会调用此方法。
     * 注意：这里的WebSocketMessage是Spring框架的类型，不是我们自定义的消息类型。
     * 
     * 客户端可能发送的消息类型：
     * 1. TextMessage - 文本消息（JSON格式）
     * 2. BinaryMessage - 二进制消息
     * 3. PingMessage - 心跳包
     * 4. PongMessage - 心跳响应
     * 
     * @param session WebSocket会话对象
     * @param message 接收到的Spring WebSocketMessage对象
     * @throws Exception 消息处理过程中的异常
     */
    @Override
    public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
        try {
            log.debug("收到WebSocket消息 - 连接ID: {}, 消息类型: {}", session.getId(), message.getClass().getSimpleName());
            
            // 根据消息类型进行不同处理
            if (message instanceof TextMessage) {
                // 处理文本消息（通常是JSON格式的业务消息）
                TextMessage textMessage = (TextMessage) message;
                String payload = textMessage.getPayload();
                
                log.debug("收到文本消息 - 连接ID: {}, 内容: {}", session.getId(), payload);
                handleTextMessage(session, payload);
                
            } else if (message instanceof BinaryMessage) {
                // 处理二进制消息（如文件传输）
                log.debug("收到二进制消息 - 连接ID: {}, 大小: {} bytes", session.getId(), message.getPayloadLength());
                handleBinaryMessage(session, (BinaryMessage) message);
                
            } else if (message instanceof PingMessage) {
                // 处理Ping消息（心跳检测）
                log.debug("收到Ping消息 - 连接ID: {}", session.getId());
                handlePingMessage(session, (PingMessage) message);
                
            } else if (message instanceof PongMessage) {
                // 处理Pong消息（心跳响应）
                log.debug("收到Pong消息 - 连接ID: {}", session.getId());
                handlePongMessage(session, (PongMessage) message);
                
            } else {
                log.warn("收到不支持的消息类型 - 连接ID: {}, 类型: {}", session.getId(), message.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket消息失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * WebSocket连接发生错误时的回调方法
     * 
     * 当WebSocket连接出现异常时（如网络错误、协议错误等），Spring会调用此方法。
     * 我们需要记录错误信息，并进行相应的处理。
     * 
     * @param session WebSocket会话对象
     * @param exception 发生的异常
     * @throws Exception 异常处理过程中的异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket连接发生错误 - 连接ID: {}, 错误: {}", 
                session.getId(), exception.getMessage(), exception);
        
        try {
            // 发生错误时主动关闭连接，避免资源泄露
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR.withReason("连接异常"));
            }
        } catch (Exception e) {
            log.error("关闭异常WebSocket连接失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage());
        }
    }
    
    /**
     * WebSocket连接关闭后的回调方法
     * 
     * 当WebSocket连接关闭时（正常关闭或异常关闭），Spring会调用此方法。
     * 我们需要清理连接相关的资源和数据。
     * 
     * @param session WebSocket会话对象
     * @param closeStatus 连接关闭状态，包含关闭原因
     * @throws Exception 连接关闭处理过程中的异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        try {
            log.info("WebSocket连接关闭 - 连接ID: {}, 关闭状态: {}, 原因: {}", 
                    session.getId(), closeStatus.getCode(), closeStatus.getReason());
            
            // 从连接管理器中移除连接
            connectionManager.removeConnection(session, closeStatus);
            
        } catch (Exception e) {
            log.error("WebSocket连接关闭处理失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否支持消息分片
     * 
     * WebSocket协议支持将大消息分成多个片段传输。
     * 对于我们的消息中心，通常消息都比较小，不需要分片。
     * 
     * @return false表示不支持分片
     */
    @Override
    public boolean supportsPartialMessages() {
        return false; // 我们不支持消息分片
    }
    
    /**
     * 处理文本消息的内部方法
     * 
     * @param session WebSocket会话对象
     * @param payload 消息内容（JSON格式）
     */
    private void handleTextMessage(WebSocketSession session, String payload) {
        try {
            // 解析JSON消息为Map对象
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            
            String messageType = (String) messageData.get("type");
            
            // 根据消息类型进行不同处理
            switch (messageType) {
                case "ping":
                    // 应用层心跳包：回复pong
                    handleAppPingMessage(session);
                    break;
                    
                case "ack":
                    // 消息确认：记录确认状态
                    handleAckMessage(session, messageData);
                    break;
                    
                case "status":
                    // 状态更新：更新用户状态
                    handleStatusMessage(session, messageData);
                    break;
                    
                default:
                    log.warn("收到未知类型的消息 - 连接ID: {}, 类型: {}", session.getId(), messageType);
            }
            
        } catch (Exception e) {
            log.error("解析WebSocket文本消息失败 - 连接ID: {}, 消息: {}, 错误: {}", 
                    session.getId(), payload, e.getMessage());
        }
    }
    
    /**
     * 处理二进制消息
     * 
     * @param session WebSocket会话对象
     * @param message 二进制消息
     */
    private void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
            log.info("收到二进制消息 - 连接ID: {}, 大小: {} bytes", session.getId(), message.getPayloadLength());
            
            // TODO: 实现二进制消息处理逻辑
            // 例如：文件上传、图片传输等
            
        } catch (Exception e) {
            log.error("处理二进制消息失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage());
        }
    }
    
    /**
     * 处理Ping消息（WebSocket协议级别的心跳）
     * 
     * @param session WebSocket会话对象
     * @param pingMessage Ping消息
     */
    private void handlePingMessage(WebSocketSession session, PingMessage pingMessage) {
        try {
            log.debug("收到Ping消息 - 连接ID: {}", session.getId());
            
            // WebSocket协议要求收到Ping后发送Pong响应
            session.sendMessage(new PongMessage());
            
        } catch (Exception e) {
            log.error("处理Ping消息失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage());
        }
    }
    
    /**
     * 处理Pong消息（WebSocket协议级别的心跳响应）
     * 
     * @param session WebSocket会话对象
     * @param pongMessage Pong消息
     */
    private void handlePongMessage(WebSocketSession session, PongMessage pongMessage) {
        try {
            log.debug("收到Pong消息 - 连接ID: {}", session.getId());
            
            // Pong消息通常用于确认连接仍然活跃
            // 可以更新连接的最后活跃时间
            
        } catch (Exception e) {
            log.error("处理Pong消息失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage());
        }
    }
    
    /**
     * 处理应用层心跳包消息
     * 客户端通过文本消息发送的业务层心跳
     * 
     * @param session WebSocket会话对象
     */
    private void handleAppPingMessage(WebSocketSession session) {
        try {
            // 回复pong消息
            String pongResponse = "{\"type\":\"pong\",\"timestamp\":" + System.currentTimeMillis() + "}";
            session.sendMessage(new TextMessage(pongResponse));
            
            log.debug("回复应用层心跳包 - 连接ID: {}", session.getId());
            
        } catch (Exception e) {
            log.error("回复应用层心跳包失败 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage());
        }
    }
    
    /**
     * 处理消息确认
     * 客户端收到消息后发送确认，用于可靠消息传输
     * 
     * @param session WebSocket会话对象
     * @param messageData 消息数据
     */
    private void handleAckMessage(WebSocketSession session, Map<String, Object> messageData) {
        String messageId = (String) messageData.get("messageId");
        log.debug("收到消息确认 - 连接ID: {}, 消息ID: {}", session.getId(), messageId);
        
        // TODO: 这里可以更新消息状态为已确认，用于消息可靠性保证
    }
    
    /**
     * 处理状态更新消息
     * 客户端可以发送状态更新，如在线状态、位置信息等
     * 
     * @param session WebSocket会话对象
     * @param messageData 消息数据
     */
    private void handleStatusMessage(WebSocketSession session, Map<String, Object> messageData) {
        String status = (String) messageData.get("status");
        log.debug("收到状态更新 - 连接ID: {}, 状态: {}", session.getId(), status);
        
        // TODO: 这里可以更新用户的在线状态信息
    }
    
    /**
     * 从WebSocket连接中提取用户信息
     * 
     * @param session WebSocket会话对象
     * @return 用户连接信息，提取失败时返回null
     */
    private UserConnectionInfo extractUserInfo(WebSocketSession session) {
        try {
            log.debug("开始提取用户信息...");
            
            URI uri = session.getUri();
            log.debug("连接URI: {}", uri);
            
            if (uri == null) {
                log.warn("连接URI为空");
                return null;
            }
            
            String query = uri.getQuery();
            log.debug("查询参数: {}", query);
            
            if (query == null || query.isEmpty()) {
                log.warn("查询参数为空");
                return null;
            }
            
            // 解析URL查询参数
            Map<String, String> params = parseQueryParams(query);
            log.debug("解析后的参数: {}", params);
            
            String userId = params.get("userId");
            String organizationId = params.get("orgId");
            String token = params.get("token");
            
            log.debug("提取的参数 - userId: {}, orgId: {}, token: {}", 
                    userId, organizationId, token != null ? "有" : "无");
            
            if (userId == null || organizationId == null) {
                log.warn("必需参数缺失 - userId: {}, orgId: {}", userId, organizationId);
                return null;
            }
            
            UserConnectionInfo userInfo = new UserConnectionInfo(userId, organizationId, token);
            log.debug("用户信息提取成功: {}", userInfo);
            return userInfo;
            
        } catch (Exception e) {
            log.error("提取WebSocket用户信息异常 - 连接ID: {}, 错误: {}", session.getId(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解析URL查询参数
     * 
     * @param query 查询字符串
     * @return 参数Map
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new java.util.HashMap<>();
        
        if (query == null || query.trim().isEmpty()) {
            log.warn("查询参数为空");
            return params;
        }
        
        log.debug("解析查询参数: {}", query);
        
        String[] pairs = query.split("&");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    // URL解码参数值
                    String key = java.net.URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                    log.debug("解析参数: {} = {}", key, value);
                } catch (Exception e) {
                    log.warn("参数解码失败: {}", pair, e);
                }
            } else {
                log.warn("参数格式错误: {}", pair);
            }
        }
        
        log.debug("解析完成，参数数量: {}", params.size());
        return params;
    }
    
    /**
     * 验证用户权限
     * 
     * @param userInfo 用户连接信息
     * @return true表示验证通过，false表示验证失败
     */
    private boolean validateUserPermission(UserConnectionInfo userInfo) {
        // TODO: 这里应该验证JWT token的有效性
        // 1. 验证token格式
        // 2. 验证token签名
        // 3. 验证token是否过期
        // 4. 验证用户权限
        
        // 暂时简化处理，只检查基本信息
        return userInfo.getUserId() != null && userInfo.getOrganizationId() != null;
    }
    
    /**
     * 用户连接信息内部类
     * 封装从连接URL中提取的用户信息
     */
    private static class UserConnectionInfo {
        private final String userId;
        private final String organizationId;
        private final String token;
        
        public UserConnectionInfo(String userId, String organizationId, String token) {
            this.userId = userId;
            this.organizationId = organizationId;
            this.token = token;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getOrganizationId() {
            return organizationId;
        }
        
        public String getToken() {
            return token;
        }
        
        @Override
        public String toString() {
            return String.format("UserConnectionInfo{userId='%s', organizationId='%s', hasToken=%s}", 
                    userId, organizationId, token != null);
        }
    }
}