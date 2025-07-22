package org.nan.cloud.message.infrastructure.websocket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.config.WebSocketProperties;
import org.nan.cloud.message.infrastructure.websocket.handler.MessageWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 
 * 这个配置类负责设置WebSocket的相关配置，包括：
 * 1. 注册WebSocket处理器
 * 2. 配置WebSocket端点路径
 * 3. 设置跨域访问策略
 * 4. 配置WebSocket传输选项
 * 
 * @EnableWebSocket 注解启用Spring WebSocket支持
 * 实现WebSocketConfigurer接口来定制WebSocket配置
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableWebSocket  // 启用Spring WebSocket功能
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    /**
     * WebSocket消息处理器
     * 负责处理WebSocket连接的生命周期和消息
     */
    private final MessageWebSocketHandler messageWebSocketHandler;
    
    /**
     * WebSocket配置属性
     * 从application.yml中读取websocket相关配置
     */
    private final WebSocketProperties webSocketProperties;
    
    /**
     * 注册WebSocket处理器
     * 
     * 这个方法由Spring框架调用，用于注册WebSocket处理器和配置端点。
     * 我们在这里定义：
     * 1. WebSocket的访问路径
     * 2. 跨域访问策略
     * 3. SockJS支持（可选）
     * 
     * @param registry WebSocket处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("开始注册WebSocket处理器和端点配置");
        log.info("WebSocket配置属性: endpoint={}, allowedOrigins={}, sockjsEnabled={}", 
                webSocketProperties.getEndpoint(), 
                webSocketProperties.getAllowedOrigins(),
                webSocketProperties.getSockjsEnabled());
        
        /*
         * 注册WebSocket端点
         * 
         * 使用配置文件中的设置来配置WebSocket，提供更好的可维护性和环境适配性
         */
        
        // 处理跨域配置 - 避免allowCredentials和allowedOrigins="*"冲突
        String[] allowedOrigins;
        if (webSocketProperties.getAllowedOrigins().contains("*")) {
            log.warn("检测到allowedOrigins包含'*'，为避免CORS冲突，本地开发环境使用allowedOriginPatterns");
            // 本地开发环境，使用具体的域名模式而不是"*"
            allowedOrigins = new String[]{
                "http://localhost:*",
                "http://127.0.0.1:*", 
                "http://192.168.1.*:*",
                "ws://localhost:*",
                "ws://127.0.0.1:*",
                "ws://192.168.1.*:*"
            };
        } else {
            allowedOrigins = webSocketProperties.getAllowedOrigins().toArray(new String[0]);
        }
        
        // 注册原生WebSocket处理器
        log.info("注册原生WebSocket处理器");
        registry
            .addHandler(messageWebSocketHandler, webSocketProperties.getEndpoint())
            .setAllowedOriginPatterns(allowedOrigins);
        
        // 根据配置决定是否启用SockJS支持
        if (webSocketProperties.getSockjsEnabled()) {
            log.info("启用SockJS支持");
            registry
                .addHandler(messageWebSocketHandler, webSocketProperties.getEndpoint())
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS()
                // 配置SockJS选项，使用配置文件中的值
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
                .setDisconnectDelay(webSocketProperties.getSockjsDisconnectDelay().intValue())
                .setHeartbeatTime(webSocketProperties.getSockjsHeartbeatTime().intValue())
                .setHttpMessageCacheSize(webSocketProperties.getSockjsHttpMessageCacheSize());
        } else {
            log.info("禁用SockJS支持，仅使用原生WebSocket");
        }
        
        log.info("WebSocket端点注册完成 - 访问路径: {}, 跨域策略: {}, SockJS: {}", 
                webSocketProperties.getEndpoint(), 
                webSocketProperties.getAllowedOrigins(),
                webSocketProperties.getSockjsEnabled() ? "已启用" : "已禁用");
    }
}

/*
 * WebSocket连接URL示例（Gateway + Cookie架构）：
 * 
 * 1. 原生WebSocket连接：
 *    ws://localhost:8082/message-service/ws  (通过Gateway代理)
 * 
 * 2. SockJS连接：
 *    http://localhost:8082/message-service/ws  (通过Gateway代理)
 * 
 * 3. 直连Message-Service（仅开发环境）：
 *    ws://localhost:8084/ws
 * 
 * 
 * 前端JavaScript连接示例（Gateway架构）：
 * 
 * // 使用原生WebSocket（通过Gateway，自动携带Cookie）
 * const socket = new WebSocket('ws://localhost:8082/message-service/ws');
 * 
 * // 使用SockJS（通过Gateway，自动携带Cookie）
 * const socket = new SockJS('http://localhost:8082/message-service/ws');
 * 
 * 注意：不再需要URL参数，认证信息通过Cookie传递给Gateway
 * 
 * 
 * 跨域配置说明：
 * 
 * 开发环境：setAllowedOrigins("*") - 允许所有域名访问
 * 生产环境：setAllowedOrigins("https://yourdomain.com", "https://admin.yourdomain.com") - 限制具体域名
 * 
 * 
 * SockJS降级方案：
 * 
 * 1. WebSocket (最优)
 * 2. XHR Streaming
 * 3. XDR Streaming (IE)
 * 4. IFrame EventSource
 * 5. IFrame HTML文件
 * 6. XHR Polling (最后的选择)
 */