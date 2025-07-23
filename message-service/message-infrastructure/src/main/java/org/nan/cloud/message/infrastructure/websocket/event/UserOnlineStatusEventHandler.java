package org.nan.cloud.message.infrastructure.websocket.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.service.UserOnlineStatusService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 用户在线状态事件处理器
 * 
 * 监听WebSocket连接事件，自动更新用户在线状态。
 * 这是用户在线状态管理的核心事件处理组件。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserOnlineStatusEventHandler {
    
    private final UserOnlineStatusService userOnlineStatusService;
    
    /**
     * 处理WebSocket连接建立事件
     * 
     * @param session WebSocket会话
     */
    public void handleWebSocketConnected(WebSocketSession session) {
        try {
            // 从会话中获取用户信息
            String userId = extractUserId(session);
            String organizationId = extractOrganizationId(session);
            String deviceType = extractDeviceType(session);
            String ipAddress = extractIpAddress(session);
            String userAgent = extractUserAgent(session);
            
            if (userId != null) {
                log.info("处理WebSocket连接建立事件: userId={}, sessionId={}, organizationId={}", 
                        userId, session.getId(), organizationId);
                
                // 标记用户上线
                userOnlineStatusService.markUserOnline(
                    userId, 
                    session.getId(), 
                    organizationId, 
                    deviceType != null ? deviceType : "WEB",
                    ipAddress,
                    userAgent
                );
                
                log.debug("用户上线状态更新成功: userId={}, sessionId={}", userId, session.getId());
            } else {
                log.warn("无法从WebSocket会话中提取用户信息: sessionId={}", session.getId());
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket连接建立事件失败: sessionId={}, error={}", 
                     session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理WebSocket连接关闭事件
     * 
     * @param session WebSocket会话
     * @param closeStatus 关闭状态
     */
    public void handleWebSocketDisconnected(WebSocketSession session, CloseStatus closeStatus) {
        try {
            String userId = extractUserId(session);
            
            if (userId != null) {
                log.info("处理WebSocket连接关闭事件: userId={}, sessionId={}, closeStatus={}", 
                        userId, session.getId(), closeStatus);
                
                // 确定离线原因
                String reason = determineOfflineReason(closeStatus);
                
                // 标记用户离线
                boolean success = userOnlineStatusService.markUserOffline(userId, session.getId(), reason);
                
                log.debug("用户离线状态更新: userId={}, sessionId={}, success={}, reason={}", 
                         userId, session.getId(), success, reason);
            } else {
                log.warn("无法从WebSocket会话中提取用户信息进行离线处理: sessionId={}", session.getId());
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket连接关闭事件失败: sessionId={}, error={}", 
                     session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理WebSocket心跳事件
     * 
     * @param session WebSocket会话
     */
    public void handleWebSocketHeartbeat(WebSocketSession session) {
        try {
            String userId = extractUserId(session);
            
            if (userId != null) {
                // 更新用户心跳
                boolean success = userOnlineStatusService.updateUserHeartbeat(userId, session.getId());
                
                log.trace("用户心跳更新: userId={}, sessionId={}, success={}", 
                         userId, session.getId(), success);
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket心跳事件失败: sessionId={}, error={}", 
                     session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理WebSocket错误事件
     * 
     * @param session WebSocket会话
     * @param exception 异常信息
     */
    public void handleWebSocketError(WebSocketSession session, Throwable exception) {
        try {
            String userId = extractUserId(session);
            
            if (userId != null) {
                log.warn("WebSocket连接发生错误: userId={}, sessionId={}, error={}", 
                        userId, session.getId(), exception.getMessage());
                
                // 记录错误活跃度
                userOnlineStatusService.recordUserActivity(
                    userId, 
                    session.getId(), 
                    "WEBSOCKET_ERROR", 
                    Map.of("error", exception.getMessage())
                );
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket错误事件失败: sessionId={}, error={}", 
                     session.getId(), e.getMessage(), e);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 从WebSocket会话中提取用户ID
     */
    private String extractUserId(WebSocketSession session) {
        try {
            // 从会话属性中获取用户ID
            Object userIdObj = session.getAttributes().get("userId");
            if (userIdObj != null) {
                return userIdObj.toString();
            }
            
            // 从principal中获取用户ID
            if (session.getPrincipal() != null) {
                return session.getPrincipal().getName();
            }
            
            // 从URI参数中获取用户ID
            String uri = session.getUri().toString();
            if (uri.contains("userId=")) {
                String[] parts = uri.split("userId=");
                if (parts.length > 1) {
                    String userIdPart = parts[1];
                    int ampIndex = userIdPart.indexOf("&");
                    return ampIndex > 0 ? userIdPart.substring(0, ampIndex) : userIdPart;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("提取用户ID失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从WebSocket会话中提取组织ID
     */
    private String extractOrganizationId(WebSocketSession session) {
        try {
            // 从会话属性中获取组织ID
            Object orgIdObj = session.getAttributes().get("organizationId");
            if (orgIdObj != null) {
                return orgIdObj.toString();
            }
            
            // 从URI参数中获取组织ID
            String uri = session.getUri().toString();
            if (uri.contains("organizationId=")) {
                String[] parts = uri.split("organizationId=");
                if (parts.length > 1) {
                    String orgIdPart = parts[1];
                    int ampIndex = orgIdPart.indexOf("&");
                    return ampIndex > 0 ? orgIdPart.substring(0, ampIndex) : orgIdPart;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("提取组织ID失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从WebSocket会话中提取设备类型
     */
    private String extractDeviceType(WebSocketSession session) {
        try {
            // 从会话属性中获取设备类型
            Object deviceTypeObj = session.getAttributes().get("deviceType");
            if (deviceTypeObj != null) {
                return deviceTypeObj.toString();
            }
            
            // 从User-Agent中推断设备类型
            String userAgent = extractUserAgent(session);
            if (userAgent != null) {
                userAgent = userAgent.toLowerCase();
                if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
                    return "MOBILE";
                } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
                    return "TABLET";
                } else if (userAgent.contains("electron")) {
                    return "DESKTOP";
                } else {
                    return "WEB";
                }
            }
            
            return "WEB"; // 默认为WEB
        } catch (Exception e) {
            log.error("提取设备类型失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            return "WEB";
        }
    }
    
    /**
     * 从WebSocket会话中提取IP地址
     */
    private String extractIpAddress(WebSocketSession session) {
        try {
            // 从会话属性中获取IP地址
            Object ipObj = session.getAttributes().get("ipAddress");
            if (ipObj != null) {
                return ipObj.toString();
            }
            
            // 从远程地址中获取IP
            if (session.getRemoteAddress() != null) {
                return session.getRemoteAddress().getAddress().getHostAddress();
            }
            
            return "unknown";
        } catch (Exception e) {
            log.error("提取IP地址失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            return "unknown";
        }
    }
    
    /**
     * 从WebSocket会话中提取User-Agent
     */
    private String extractUserAgent(WebSocketSession session) {
        try {
            // 从会话属性中获取User-Agent
            Object userAgentObj = session.getAttributes().get("userAgent");
            if (userAgentObj != null) {
                return userAgentObj.toString();
            }
            
            // 从请求头中获取User-Agent
            if (session.getHandshakeHeaders() != null) {
                java.util.List<String> userAgents = session.getHandshakeHeaders().get("User-Agent");
                if (userAgents != null && !userAgents.isEmpty()) {
                    return userAgents.get(0);
                }
            }
            
            return "unknown";
        } catch (Exception e) {
            log.error("提取User-Agent失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            return "unknown";
        }
    }
    
    /**
     * 根据关闭状态确定离线原因
     */
    private String determineOfflineReason(CloseStatus closeStatus) {
        if (closeStatus == null) {
            return "UNKNOWN";
        }
        
        switch (closeStatus.getCode()) {
            case 1000:
                return "NORMAL_CLOSURE";
            case 1001:
                return "GOING_AWAY";
            case 1002:
                return "PROTOCOL_ERROR";
            case 1003:
                return "UNSUPPORTED_DATA";
            case 1006:
                return "ABNORMAL_CLOSURE";
            case 1008:
                return "POLICY_VIOLATION";
            case 1009:
                return "MESSAGE_TOO_BIG";
            case 1011:
                return "SERVER_ERROR";
            case 1012:
                return "SERVICE_RESTART";
            case 1013:
                return "TRY_AGAIN_LATER";
            case 1015:
                return "TLS_HANDSHAKE_FAILURE";
            default:
                return "CODE_" + closeStatus.getCode();
        }
    }
}