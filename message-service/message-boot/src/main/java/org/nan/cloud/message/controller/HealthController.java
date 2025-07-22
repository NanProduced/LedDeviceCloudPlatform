package org.nan.cloud.message.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.config.WebSocketProperties;
import org.nan.cloud.message.infrastructure.websocket.manager.WebSocketConnectionManager;
import org.nan.cloud.common.web.IgnoreDynamicResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * 提供服务状态检查和WebSocket状态信息
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    
    private final WebSocketProperties webSocketProperties;
    private final WebSocketConnectionManager connectionManager;
    
    /**
     * 基础健康检查
     */
    @GetMapping
    @IgnoreDynamicResponse
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "message-service");
        health.put("version", "1.0.0");
        
        log.debug("健康检查请求: {}", health);
        return health;
    }
    
    /**
     * WebSocket状态检查
     */
    @GetMapping("/websocket")
    @IgnoreDynamicResponse
    public Map<String, Object> websocketHealth() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // WebSocket配置信息
            Map<String, Object> config = new HashMap<>();
            config.put("endpoint", webSocketProperties.getEndpoint());
            config.put("allowedOrigins", webSocketProperties.getAllowedOrigins());
            config.put("sockjsEnabled", webSocketProperties.getSockjsEnabled());
            config.put("maxConnections", webSocketProperties.getMaxConnections());
            
            // WebSocket连接统计
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalConnections", connectionManager.getTotalConnectionCount());
            stats.put("onlineUsers", connectionManager.getOnlineUserCount());
            
            status.put("status", "UP");
            status.put("timestamp", LocalDateTime.now());
            status.put("config", config);
            status.put("statistics", stats);
            
            log.info("WebSocket健康检查: 总连接数={}, 在线用户数={}", 
                    connectionManager.getTotalConnectionCount(),
                    connectionManager.getOnlineUserCount());
            
        } catch (Exception e) {
            log.error("WebSocket健康检查失败", e);
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("timestamp", LocalDateTime.now());
        }
        
        return status;
    }
    
    /**
     * WebSocket配置信息
     */
    @GetMapping("/websocket/config")
    @IgnoreDynamicResponse
    public Map<String, Object> websocketConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("endpoint", webSocketProperties.getEndpoint());
        config.put("allowedOrigins", webSocketProperties.getAllowedOrigins());
        config.put("sockjsEnabled", webSocketProperties.getSockjsEnabled());
        config.put("heartbeatInterval", webSocketProperties.getHeartbeatInterval());
        config.put("connectionTimeout", webSocketProperties.getConnectionTimeout());
        config.put("maxConnections", webSocketProperties.getMaxConnections());
        config.put("messageBufferSize", webSocketProperties.getMessageBufferSize());
        
        // 构建测试连接URL
        String testUrl = String.format("ws://localhost:8084%s?userId=test-user&orgId=test-org&token=test-token", 
                webSocketProperties.getEndpoint());
        config.put("testConnectionUrl", testUrl);
        
        log.debug("WebSocket配置查询: {}", config);
        return config;
    }
}