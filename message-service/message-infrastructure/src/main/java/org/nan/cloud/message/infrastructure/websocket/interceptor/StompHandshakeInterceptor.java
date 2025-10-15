package org.nan.cloud.message.infrastructure.websocket.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayAuthValidator;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * STOMP握手拦截器
 * 
 * 在WebSocket握手阶段处理Gateway认证：
 * 1. 从HTTP请求头中解析CLOUD-AUTH信息
 * 2. 验证用户身份和权限
 * 3. 将用户信息存储到WebSocket会话属性中
 * 4. 为后续的STOMP通道拦截器提供认证上下文
 * 
 * 认证流程：
 * 1. Gateway完成OAuth2/OIDC认证
 * 2. Gateway转发请求时添加CLOUD-AUTH头
 * 3. 本拦截器解析并验证用户信息
 * 4. 认证成功则允许握手，失败则拒绝连接
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandshakeInterceptor implements HandshakeInterceptor {
    
    private final GatewayAuthValidator gatewayAuthValidator;
    
    /**
     * WebSocket会话属性键 - 用户信息
     */
    public static final String USER_INFO_ATTRIBUTE = "userInfo";
    
    /**
     * WebSocket会话属性键 - 用户ID
     */
    public static final String USER_ID_ATTRIBUTE = "userId";
    
    /**
     * WebSocket会话属性键 - 组织ID
     */
    public static final String ORGANIZATION_ID_ATTRIBUTE = "organizationId";
    
    /**
     * 握手前处理
     * 
     * 在WebSocket握手建立前执行认证验证：
     * 1. 解析Gateway传递的CLOUD-AUTH头
     * 2. 验证用户身份和WebSocket连接权限
     * 3. 将认证信息存储到会话属性中供后续使用
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象  
     * @param wsHandler WebSocket处理器
     * @param attributes 会话属性映射，用于存储认证信息
     * @return true表示允许握手，false表示拒绝连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            log.info("===== STOMP握手认证开始 =====");
            log.info("请求URI: {}", request.getURI());
            log.info("远程地址: {}", request.getRemoteAddress());
            log.info("请求头数量: {}", request.getHeaders().size());
            
            // 从请求头中解析Gateway用户信息
            GatewayUserInfo userInfo = gatewayAuthValidator.validateUserFromHeaders(request.getHeaders());
            
            if (userInfo == null) {
                log.error("❌ Gateway用户验证失败，拒绝握手");
                return false;
            }
            
            // 检查WebSocket连接权限
            if (!gatewayAuthValidator.hasWebSocketPermission(userInfo)) {
                log.error("❌ 用户无WebSocket连接权限 - 用户ID: {}", userInfo.getUid());
                return false;
            }
            
            // 将用户信息存储到会话属性中
            attributes.put(USER_INFO_ATTRIBUTE, userInfo);
            attributes.put(USER_ID_ATTRIBUTE, userInfo.getUid().toString());
            attributes.put(ORGANIZATION_ID_ATTRIBUTE, userInfo.getOid().toString());
            
            // 存储客户端信息用于后续的连接管理
            String userAgent = request.getHeaders().getFirst("User-Agent");
            request.getRemoteAddress();
            String remoteAddress = request.getRemoteAddress().getAddress().getHostAddress();
            
            attributes.put("user-agent", userAgent != null ? userAgent : "unknown");
            attributes.put("remote-address", remoteAddress);
            
            log.info("✅ STOMP握手认证成功");
            log.info("用户ID: {}, 组织ID: {}, 用户类型: {}", 
                    userInfo.getUid(), userInfo.getOid(), userInfo.getUserType());
            log.info("===== STOMP握手认证完成 =====");
            
            return true;
            
        } catch (Exception e) {
            log.error("💥 STOMP握手认证过程中发生异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 握手后处理
     * 
     * 在WebSocket握手成功建立后执行：
     * 1. 记录连接建立日志
     * 2. 可以在这里添加额外的初始化逻辑
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param wsHandler WebSocket处理器  
     * @param exception 握手过程中的异常（如果有）
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                             WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("STOMP握手完成，但发生异常: {}", exception.getMessage());
        }
    }
}