package org.nan.cloud.message.infrastructure.websocket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.config.WebSocketProperties;
import org.nan.cloud.message.infrastructure.websocket.interceptor.StompChannelInterceptor;
import org.nan.cloud.message.infrastructure.websocket.interceptor.StompHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket 配置类
 * 
 * 替换原有的原生WebSocket实现，使用STOMP协议提供更强大的发布订阅功能：
 * 1. 支持主题订阅模式，按需推送消息
 * 2. 标准化的消息路由和过滤
 * 3. 更好的前端集成体验
 * 4. 保持原有的Gateway认证方式
 * 
 * 主要特性：
 * - 使用内存消息代理（适合单节点部署）
 * - 支持SockJS降级方案
 * - Gateway CLOUD-AUTH头认证
 * - 自动订阅用户和组织主题
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketProperties webSocketProperties;
    private final StompHandshakeInterceptor handshakeInterceptor;
    private final StompChannelInterceptor channelInterceptor;
    
    /**
     * 配置消息代理
     * 
     * 定义消息路由规则和代理行为：
     * - /topic: 主题订阅，支持多对多发布订阅
     * - /queue: 点对点队列，支持一对一消息
     * - /app: 客户端发送消息的目的地前缀
     * - /user: 用户特定目的地前缀
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("配置STOMP消息代理...");
        
        // 启用内存消息代理
        // 生产环境可以考虑使用 RabbitMQ: config.enableStompBrokerRelay("/topic", "/queue")
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{webSocketProperties.getHeartbeatInterval(), webSocketProperties.getHeartbeatInterval()});
        
        // 客户端发送消息的目的地前缀
        config.setApplicationDestinationPrefixes("/app");
        
        // 用户特定目的地前缀（用于点对点消息）
        config.setUserDestinationPrefix("/user");
        
        log.info("STOMP消息代理配置完成 - 主题前缀: [/topic, /queue], 应用前缀: /app, 用户前缀: /user");
    }
    
    /**
     * 注册STOMP端点
     * 
     * 配置WebSocket连接端点和相关选项：
     * - 端点路径和跨域策略
     * - SockJS降级支持
     * - 握手拦截器（认证处理）
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("注册STOMP端点: {}", webSocketProperties.getEndpoint());
        
        // 处理跨域配置
        String[] allowedOrigins = resolveAllowedOrigins();
        
        // 注册主要的STOMP端点
        registry.addEndpoint(webSocketProperties.getEndpoint())
                .setAllowedOriginPatterns(allowedOrigins)
                .addInterceptors(handshakeInterceptor);
        
        // 如果启用SockJS，注册SockJS端点
        if (webSocketProperties.getSockjsEnabled()) {
            log.info("启用SockJS支持");
            registry.addEndpoint(webSocketProperties.getEndpoint())
                    .setAllowedOriginPatterns(allowedOrigins)
                    .addInterceptors(handshakeInterceptor)
                    .withSockJS()
                    .setDisconnectDelay(webSocketProperties.getSockjsDisconnectDelay())
                    .setHeartbeatTime(webSocketProperties.getSockjsHeartbeatTime())
                    .setHttpMessageCacheSize(webSocketProperties.getSockjsHttpMessageCacheSize());
        } else {
            log.info("SockJS支持已禁用");
        }
        
        log.info("STOMP端点注册完成 - 路径: {}, 跨域: {}, SockJS: {}", 
                webSocketProperties.getEndpoint(), 
                String.join(",", allowedOrigins), 
                webSocketProperties.getSockjsEnabled());
    }
    
    /**
     * 配置客户端入站通道
     * 
     * 添加通道拦截器用于：
     * - 订阅权限验证
     * - 消息发送权限检查
     * - 连接状态跟踪
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("配置客户端入站通道拦截器");
        registration.interceptors(channelInterceptor);
    }
    
    /**
     * 解析允许的跨域源
     * 
     * 处理配置中的通配符"*"，避免与allowCredentials冲突
     */
    private String[] resolveAllowedOrigins() {
        if (webSocketProperties.getAllowedOrigins().contains("*")) {
            log.warn("检测到allowedOrigins包含'*'，为避免CORS冲突，使用具体的域名模式");
            // 开发环境使用具体的域名模式
            return new String[]{
                "http://localhost:*",
                "http://127.0.0.1:*", 
                "http://192.168.1.*:*",
                "ws://localhost:*",
                "ws://127.0.0.1:*",
                "ws://192.168.1.*:*"
            };
        } else {
            return webSocketProperties.getAllowedOrigins().toArray(new String[0]);
        }
    }
}

/*
 * STOMP主题设计说明：
 * 
 * 1. 用户个人通知：
 *    /topic/user/{uid}/notifications
 *    - 个人系统通知
 *    - 任务状态变更
 *    - 私人消息
 * 
 * 2. 组织公告：
 *    /topic/organization/{oid}/announcements  
 *    - 组织内公告
 *    - 组织级别的系统通知
 *    - 管理员消息
 * 
 * 3. 终端状态：
 *    /topic/terminal/{terminalId}/status
 *    - 终端在线状态变化
 *    - 设备健康状态
 *    - 连接状态更新
 * 
 * 4. 指令执行结果：
 *    /topic/terminal/{terminalId}/command-result
 *    - 设备指令执行反馈
 *    - 操作结果通知
 *    - 错误信息反馈
 * 
 * 
 * 客户端使用示例：
 * 
 * // 连接STOMP
 * const stompClient = new StompJs.Client({
 *     brokerURL: 'ws://localhost:8082/message-service/ws'
 * });
 * 
 * // 连接成功后会自动订阅用户和组织主题
 * stompClient.onConnect = (frame) => {
 *     // 手动订阅特定终端
 *     stompClient.subscribe('/topic/terminal/123/status', (message) => {
 *         const status = JSON.parse(message.body);
 *         updateTerminalStatus(123, status);
 *     });
 * };
 * 
 * // 发送消息
 * stompClient.publish({
 *     destination: '/app/terminal/command',
 *     body: JSON.stringify({terminalId: 123, command: 'start'})
 * });
 */