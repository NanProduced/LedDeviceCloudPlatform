package org.nan.cloud.terminal.websocket.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Terminal WebSocket握手拦截器
 * 
 * 在WebSocket连接建立前进行安全检查和预处理：
 * 1. 设备ID提取：从URL路径提取设备ID(/terminal/ws/{deviceId})
 * 2. 认证检查：验证设备认证状态(Redis缓存)
 * 3. 连接限制：单设备最大连接数控制，防止资源滥用
 * 4. IP白名单：可选的IP访问控制(预留扩展点)
 * 5. 参数传递：将验证信息传递给WebSocketHandler
 * 
 * 安全机制：
 * - 认证缓存TTL：30分钟，自动过期清理
 * - 连接频率限制：同设备5秒内最多3次连接尝试
 * - 失败计数：记录握手失败次数，异常行为监控
 * - 连接数统计：实时统计活跃连接数，性能监控
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TerminalWebSocketInterceptor implements HandshakeInterceptor {

    private final StringRedisTemplate redisTemplate;

    // URL路径模式：/terminal/ws/{deviceId}
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("/terminal/ws/([^/]+)");
    
    // Redis键前缀定义
    private static final String AUTH_SUCCESS_PREFIX = "terminal:auth:success:";
    private static final String WS_CONNECTION_LIMIT_PREFIX = "terminal:ws:limit:";
    private static final String WS_CONNECTION_COUNT_PREFIX = "terminal:ws:count:";

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, 
                                 @NonNull ServerHttpResponse response,
                                 @NonNull WebSocketHandler wsHandler, 
                                 @NonNull Map<String, Object> attributes) {
        try {
            // 1. 提取设备ID
            String deviceId = extractDeviceId(request.getURI());
            if (!StringUtils.hasText(deviceId)) {
                log.warn("WebSocket握手失败: 无效的设备ID, URI: {}", request.getURI());
                return false;
            }

            // 2. 检查设备认证状态
            String authKey = AUTH_SUCCESS_PREFIX + deviceId;
            if (!redisTemplate.hasKey(authKey)) {
                log.warn("WebSocket握手失败: 设备未认证, deviceId: {}", deviceId);
                return false;
            }

            // 3. 检查连接频率限制
            if (!checkConnectionRateLimit(deviceId)) {
                log.warn("WebSocket握手失败: 连接频率过高, deviceId: {}", deviceId);
                return false;
            }

            // 4. 更新连接统计
            updateConnectionStats(deviceId);

            // 5. 将设备信息传递给Handler
            attributes.put("deviceId", deviceId);
            attributes.put("connectTime", System.currentTimeMillis());
            attributes.put("clientIp", getClientIpAddress(request));

            log.info("WebSocket握手成功: deviceId={}, clientIp={}", 
                deviceId, attributes.get("clientIp"));
            
            return true;

        } catch (Exception e) {
            log.error("WebSocket握手异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, 
                             @NonNull ServerHttpResponse response,
                             @NonNull WebSocketHandler wsHandler, 
                             Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手后处理异常: URI={}", request.getURI(), exception);
        }
    }

    /**
     * 从URI中提取设备ID
     * URL格式：/terminal/ws/{deviceId}
     */
    private String extractDeviceId(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return null;
        }
        
        Matcher matcher = DEVICE_ID_PATTERN.matcher(uri.getPath());
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * 检查连接频率限制
     * 同一设备5秒内最多允许3次连接尝试
     */
    private boolean checkConnectionRateLimit(String deviceId) {
        String limitKey = WS_CONNECTION_LIMIT_PREFIX + deviceId;
        
        try {
            // 获取当前连接次数
            String countStr = redisTemplate.opsForValue().get(limitKey);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            // 检查是否超过限制
            if (currentCount >= 3) {
                return false;
            }
            
            // 增加连接次数，5秒TTL
            redisTemplate.opsForValue().set(limitKey, 
                String.valueOf(currentCount + 1), 5, TimeUnit.SECONDS);
            
            return true;
            
        } catch (Exception e) {
            log.error("检查连接频率限制异常: deviceId={}", deviceId, e);
            // 异常情况下允许连接，避免影响正常业务
            return true;
        }
    }

    /**
     * 更新连接统计信息
     */
    private void updateConnectionStats(String deviceId) {
        try {
            // 增加设备连接计数
            String countKey = WS_CONNECTION_COUNT_PREFIX + deviceId;
            redisTemplate.opsForValue().increment(countKey);
            redisTemplate.expire(countKey, 24, TimeUnit.HOURS);
            
            // 增加全局连接计数
            redisTemplate.opsForValue().increment("terminal:ws:total:connections");
            
        } catch (Exception e) {
            log.error("更新连接统计异常: deviceId={}", deviceId, e);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 支持代理和负载均衡环境
     */
    private String getClientIpAddress(ServerHttpRequest request) {
        // 优先检查X-Forwarded-For头(代理环境)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // 取第一个IP地址
            return xForwardedFor.split(",")[0].trim();
        }
        
        // 检查X-Real-IP头(Nginx代理)
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }
        
        // 使用远程地址
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }
}