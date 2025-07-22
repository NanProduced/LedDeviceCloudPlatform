package org.nan.cloud.message.infrastructure.websocket.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

/**
 * Cookie提取器
 * 
 * 从WebSocket握手请求中提取Cookie信息，用于Gateway认证。
 * 处理各种Cookie格式和编码问题。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
public class CookieExtractor {
    
    /**
     * 从WebSocket会话中提取Cookie字符串
     * 
     * @param session WebSocket会话
     * @return Cookie字符串，提取失败时返回null
     */
    public String extractCookies(WebSocketSession session) {
        try {
            log.debug("开始提取WebSocket会话Cookie");
            
            // 从握手头中获取Cookie
            List<String> cookieHeaders = session.getHandshakeHeaders().get("Cookie");
            if (cookieHeaders == null || cookieHeaders.isEmpty()) {
                log.debug("WebSocket握手请求中未找到Cookie头");
                return null;
            }
            
            // 合并多个Cookie头（如果存在）
            StringBuilder cookiesBuilder = new StringBuilder();
            for (int i = 0; i < cookieHeaders.size(); i++) {
                if (i > 0) {
                    cookiesBuilder.append("; ");
                }
                cookiesBuilder.append(cookieHeaders.get(i));
            }
            
            String cookies = cookiesBuilder.toString();
            log.debug("提取到Cookie: {}", maskSensitiveCookies(cookies));
            
            return cookies;
            
        } catch (Exception e) {
            log.error("提取WebSocket Cookie失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从WebSocket会话中提取指定名称的Cookie值
     * 
     * @param session WebSocket会话
     * @param cookieName Cookie名称
     * @return Cookie值，未找到时返回null
     */
    public String extractCookieValue(WebSocketSession session, String cookieName) {
        try {
            String cookies = extractCookies(session);
            if (cookies == null || cookies.trim().isEmpty()) {
                return null;
            }
            
            return parseCookieValue(cookies, cookieName);
            
        } catch (Exception e) {
            log.error("提取Cookie值失败 - Cookie名称: {}, 错误: {}", cookieName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析Cookie字符串，提取指定名称的Cookie值
     * 
     * @param cookies Cookie字符串
     * @param cookieName Cookie名称
     * @return Cookie值，未找到时返回null
     */
    private String parseCookieValue(String cookies, String cookieName) {
        if (cookies == null || cookieName == null) {
            return null;
        }
        
        try {
            // 分割Cookie字符串
            String[] cookiePairs = cookies.split(";");
            
            for (String cookiePair : cookiePairs) {
                String trimmedPair = cookiePair.trim();
                String[] keyValue = trimmedPair.split("=", 2);
                
                if (keyValue.length == 2 && cookieName.equals(keyValue[0].trim())) {
                    String value = keyValue[1].trim();
                    
                    // 移除可能的引号
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    return value;
                }
            }
            
        } catch (Exception e) {
            log.error("解析Cookie值失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 验证Cookie是否包含必要的会话信息
     * 
     * @param session WebSocket会话
     * @return true表示包含有效的会话Cookie，false表示无效
     */
    public boolean hasValidSessionCookie(WebSocketSession session) {
        try {
            String cookies = extractCookies(session);
            if (cookies == null || cookies.trim().isEmpty()) {
                return false;
            }
            
            // 检查常见的会话Cookie名称
            String[] sessionCookieNames = {"JSESSIONID", "SESSION", "session_id", "auth_token"};
            
            for (String cookieName : sessionCookieNames) {
                String cookieValue = parseCookieValue(cookies, cookieName);
                if (cookieValue != null && !cookieValue.trim().isEmpty()) {
                    log.debug("找到有效的会话Cookie: {}", cookieName);
                    return true;
                }
            }
            
            log.debug("未找到有效的会话Cookie");
            return false;
            
        } catch (Exception e) {
            log.error("验证会话Cookie失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取会话标识（从各种可能的Cookie中提取）
     * 
     * @param session WebSocket会话
     * @return 会话标识，未找到时返回null
     */
    public String getSessionIdentifier(WebSocketSession session) {
        try {
            // 按优先级尝试不同的会话Cookie
            String[] sessionCookieNames = {"JSESSIONID", "SESSION", "session_id", "auth_token"};
            
            for (String cookieName : sessionCookieNames) {
                String value = extractCookieValue(session, cookieName);
                if (value != null && !value.trim().isEmpty()) {
                    log.debug("使用{}作为会话标识", cookieName);
                    return value;
                }
            }
            
            log.warn("未找到会话标识Cookie");
            return null;
            
        } catch (Exception e) {
            log.error("获取会话标识失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 遮蔽敏感Cookie信息（用于日志记录）
     * 
     * @param cookies Cookie字符串
     * @return 遮蔽敏感信息后的Cookie字符串
     */
    private String maskSensitiveCookies(String cookies) {
        if (cookies == null) {
            return "null";
        }
        
        try {
            // 遮蔽敏感的Cookie值
            String[] sensitiveNames = {"JSESSIONID", "SESSION", "session_id", "auth_token", "access_token"};
            String masked = cookies;
            
            for (String sensitiveName : sensitiveNames) {
                masked = masked.replaceAll(
                    "(" + sensitiveName + "=)[^;]+", 
                    "$1****"
                );
            }
            
            return masked;
            
        } catch (Exception e) {
            return "cookie-mask-error";
        }
    }
    
    /**
     * 提取所有Cookie信息（调试用）
     * 
     * @param session WebSocket会话
     * @return Cookie信息Map
     */
    public Map<String, String> extractAllCookies(WebSocketSession session) {
        Map<String, String> cookieMap = new java.util.HashMap<>();
        
        try {
            String cookies = extractCookies(session);
            if (cookies == null) {
                return cookieMap;
            }
            
            String[] cookiePairs = cookies.split(";");
            
            for (String cookiePair : cookiePairs) {
                String trimmedPair = cookiePair.trim();
                String[] keyValue = trimmedPair.split("=", 2);
                
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    
                    // 移除可能的引号
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    cookieMap.put(key, value);
                }
            }
            
        } catch (Exception e) {
            log.error("提取所有Cookie失败: {}", e.getMessage());
        }
        
        return cookieMap;
    }
}