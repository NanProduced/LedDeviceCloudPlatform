package org.nan.cloud.message.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket配置属性类
 * 
 * 位于application层，定义WebSocket相关的配置属性。
 * Infrastructure层通过依赖注入使用这些配置，遵循DDD分层架构。
 * 
 * 配置示例：
 * websocket:
 *   endpoint: /ws
 *   allowed-origins:
 *     - "*"
 *   heartbeat-interval: 30000
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@RefreshScope
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {
    
    /**
     * WebSocket端点路径
     * 默认值: /ws
     */
    private String endpoint = "/ws";
    
    /**
     * 允许跨域访问的源列表
     * 默认值: ["*"] (允许所有源)
     */
    private List<String> allowedOrigins = List.of("*");
    
    /**
     * 心跳间隔时间（毫秒）
     * 默认值: 30000 (30秒)
     */
    private Long heartbeatInterval = 30000L;
    
    /**
     * 连接超时时间（毫秒）
     * 默认值: 60000 (60秒)
     */
    private Long connectionTimeout = 60000L;
    
    /**
     * 最大连接数
     * 默认值: 10000
     */
    private Integer maxConnections = 10000;
    
    /**
     * 消息缓冲区大小（字节）
     * 默认值: 8192 (8KB)
     */
    private Integer messageBufferSize = 8192;
    
    /**
     * 是否启用SockJS支持
     * 默认值: true
     */
    private Boolean sockjsEnabled = true;
    
    /**
     * SockJS断开连接延迟时间（毫秒）
     * 默认值: 30000 (30秒)
     */
    private Long sockjsDisconnectDelay = 30000L;
    
    /**
     * SockJS心跳时间（毫秒）
     * 默认值: 25000 (25秒)
     */
    private Long sockjsHeartbeatTime = 25000L;
    
    /**
     * SockJS HTTP消息缓存大小
     * 默认值: 1000
     */
    private Integer sockjsHttpMessageCacheSize = 1000;
}