package org.nan.cloud.message.infrastructure.websocket.heartbeat;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.event.WebSocketHeartbeatEvent;
import org.nan.cloud.message.infrastructure.websocket.session.WebSocketSessionInfo;
import org.nan.cloud.message.infrastructure.websocket.session.WebSocketSessionStore;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket心跳检测管理器
 * 
 * 负责管理WebSocket连接的心跳检测，确保连接的有效性。
 * 通过定期检查连接状态，及时清理无效连接，维护系统的稳定性。
 * 
 * 心跳机制说明：
 * 1. 客户端定期发送心跳包（ping）
 * 2. 服务端收到心跳包后更新最后心跳时间
 * 3. 服务端定期检查心跳超时的连接
 * 4. 超时连接会被主动关闭并清理
 * 
 * 心跳检测策略：
 * - 心跳间隔：30秒（可配置）
 * - 心跳超时：90秒（3个心跳周期）
 * - 检测频率：每60秒检查一次
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Lazy
public class WebSocketHeartbeatManager {
    
    private final WebSocketSessionStore sessionStore;
    
    // 心跳超时时间（分钟）
    private static final long HEARTBEAT_TIMEOUT_MINUTES = 3;
    
    
    /**
     * 处理WebSocket心跳事件
     * 通过事件监听器处理连接注册、移除和心跳更新
     * 
     * @param event 心跳事件
     */
    @EventListener
    public void handleHeartbeatEvent(WebSocketHeartbeatEvent event) {
        String sessionId = event.getSessionId();
        
        switch (event.getEventType()) {
            case REGISTER:
                handleRegisterConnection(sessionId);
                break;
            case UNREGISTER:
                handleUnregisterConnection(sessionId);
                break;
            case UPDATE:
                handleUpdateHeartbeat(sessionId);
                break;
        }
    }
    
    /**
     * 处理连接注册
     * 
     * @param sessionId 会话ID
     */
    private void handleRegisterConnection(String sessionId) {
        // 由于解耦，这里不再直接管理WebSocketSession对象
        // 只记录会话ID用于心跳检测
        log.debug("注册WebSocket连接用于心跳检测 - 会话: {}", sessionId);
    }
    
    /**
     * 处理连接移除
     * 
     * @param sessionId 会话ID
     */
    private void handleUnregisterConnection(String sessionId) {
        log.debug("从心跳检测中移除WebSocket连接 - 会话: {}", sessionId);
    }
    
    /**
     * 处理心跳更新
     * 
     * @param sessionId 会话ID
     */
    private void handleUpdateHeartbeat(String sessionId) {
        try {
            // 更新Redis中的心跳时间
            WebSocketSessionInfo sessionInfo = sessionStore.getSession(sessionId);
            if (sessionInfo != null) {
                sessionInfo.updateHeartbeat();
                sessionStore.saveSession(sessionInfo);
                
                log.debug("更新WebSocket心跳 - 会话: {}, 用户: {}", 
                        sessionId, sessionInfo.getUserId());
            }
            
        } catch (Exception e) {
            log.error("更新WebSocket心跳失败 - 会话: {}, 错误: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 定期心跳检测任务
     * 每60秒执行一次，检查并清理超时连接
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void checkHeartbeat() {
        try {
            log.debug("开始WebSocket心跳检测...");
            
            int timeoutCount = 0;
            int checkedCount = 0;
            
            // 从Redis获取所有活跃会话进行检查
            Set<String> allSessionIds = sessionStore.getAllSessionIds();
            
            for (String sessionId : allSessionIds) {
                checkedCount++;
                
                WebSocketSessionInfo sessionInfo = sessionStore.getSession(sessionId);
                if (sessionInfo == null) {
                    // 会话信息已过期
                    timeoutCount++;
                    continue;
                }
                
                // 检查心跳超时
                if (sessionInfo.isHeartbeatTimeout(HEARTBEAT_TIMEOUT_MINUTES)) {
                    log.warn("WebSocket心跳超时 - 会话: {}, 用户: {}, 最后心跳: {}", 
                            sessionId, sessionInfo.getUserId(), sessionInfo.getLastHeartbeatTime());
                    
                    // 标记会话为超时，由连接管理器处理具体的连接关闭
                    sessionStore.removeSession(sessionId);
                    timeoutCount++;
                }
            }
            
            if (timeoutCount > 0 || checkedCount > 0) {
                log.info("WebSocket心跳检测完成 - 检查数量: {}, 超时清理: {}", 
                        checkedCount, timeoutCount);
            }
            
            // 定期清理Redis中的过期会话
            sessionStore.cleanupExpiredSessions();
            
        } catch (Exception e) {
            log.error("WebSocket心跳检测失败: {}", e.getMessage(), e);
        }
    }
    
    
    /**
     * 获取心跳统计信息
     * 
     * @return 心跳统计信息
     */
    public HeartbeatStats getHeartbeatStats() {
        Set<String> allSessionIds = sessionStore.getAllSessionIds();
        int totalConnections = allSessionIds.size();
        int timeoutConnections = 0;
        int activeConnections = 0;
        
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(HEARTBEAT_TIMEOUT_MINUTES);
        
        for (String sessionId : allSessionIds) {
            WebSocketSessionInfo sessionInfo = sessionStore.getSession(sessionId);
            if (sessionInfo != null) {
                if (sessionInfo.getLastHeartbeatTime() != null && 
                    sessionInfo.getLastHeartbeatTime().isBefore(timeoutThreshold)) {
                    timeoutConnections++;
                } else {
                    activeConnections++;
                }
            }
        }
        
        return new HeartbeatStats(totalConnections, activeConnections, timeoutConnections);
    }
    
    /**
     * 心跳统计信息
     */
    public static class HeartbeatStats {
        private final int totalConnections;
        private final int activeConnections;
        private final int timeoutConnections;
        
        public HeartbeatStats(int totalConnections, int activeConnections, int timeoutConnections) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.timeoutConnections = timeoutConnections;
        }
        
        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getTimeoutConnections() { return timeoutConnections; }
        
        @Override
        public String toString() {
            return String.format("HeartbeatStats{total=%d, active=%d, timeout=%d}", 
                    totalConnections, activeConnections, timeoutConnections);
        }
    }
}