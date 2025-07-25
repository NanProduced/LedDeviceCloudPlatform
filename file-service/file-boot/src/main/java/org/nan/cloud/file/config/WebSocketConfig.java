package org.nan.cloud.file.config;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.file.infrastructure.progress.TranscodingProgressWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 * 
 * 配置转码进度推送的WebSocket端点
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TranscodingProgressWebSocketHandler progressWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册转码进度WebSocket端点
        registry.addHandler(progressWebSocketHandler, "/file/progress")
                .setAllowedOriginPatterns("*") // 生产环境应该限制具体域名
                .withSockJS(); // 支持SockJS降级
    }
}