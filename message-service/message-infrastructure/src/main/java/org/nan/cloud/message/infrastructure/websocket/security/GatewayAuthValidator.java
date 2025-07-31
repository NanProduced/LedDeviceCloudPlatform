package org.nan.cloud.message.infrastructure.websocket.security;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import org.springframework.http.HttpHeaders;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Gateway认证验证器
 * 
 * 从WebSocket握手请求的CLOUD-AUTH头中解析Gateway传递的用户信息。
 * 这种方式适用于Gateway代理的WebSocket连接。
 * 
 * 认证流程：
 * 1. Gateway完成OAuth2/OIDC认证
 * 2. Gateway在转发WebSocket请求时添加CLOUD-AUTH头
 * 3. Message-Service从头中解析用户信息
 * 4. 信任Gateway的认证结果（内网安全）
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
public class GatewayAuthValidator {
    
    /**
     * CLOUD-AUTH请求头名称
     */
    private static final String CLOUD_AUTH_HEADER = "CLOUD-AUTH";
    
    /**
     * 解析CLOUD-AUTH头中的用户信息
     * 
     * @param authHeader Base64编码的JSON字符串
     * @return Gateway用户信息，解析失败时返回null
     */
    private GatewayUserInfo parseCloudAuthHeader(String authHeader) {
        try {
            // Base64 URL解码
            byte[] decodedBytes = Base64.getUrlDecoder().decode(authHeader);
            String jsonString = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            
            log.debug("解码CLOUD-AUTH头: {}", jsonString);
            
            // 解析JSON
            JsonNode jsonNode = JsonUtils.fromJson(jsonString);
            
            // 提取用户信息字段
            Long uid = jsonNode.has("uid") ? jsonNode.get("uid").asLong() : null;
            Long oid = jsonNode.has("oid") ? jsonNode.get("oid").asLong() : null;
            Long ugid = jsonNode.has("ugid") ? jsonNode.get("ugid").asLong() : null;
            Integer userType = jsonNode.has("userType") ? jsonNode.get("userType").asInt() : null;
            
            // 验证必需字段
            if (uid == null || oid == null) {
                log.warn("CLOUD-AUTH头缺少必需字段 - uid: {}, oid: {}", uid, oid);
                return null;
            }
            
            return GatewayUserInfo.builder()
                    .uid(uid)
                    .oid(oid)
                    .ugid(ugid)
                    .userType(userType)
                    .build();
                    
        } catch (IllegalArgumentException e) {
            log.error("CLOUD-AUTH头Base64解码失败: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("解析CLOUD-AUTH头失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 验证用户身份（兼容方法，用于向后兼容）
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 是否验证通过
     */
    public boolean validateUser(String userId, String organizationId) {
        // 由于通过Gateway验证，这里只做基本的非空检查
        boolean valid = userId != null && !userId.trim().isEmpty() && 
                       organizationId != null && !organizationId.trim().isEmpty();
        
        if (valid) {
            log.debug("用户身份验证通过 - 用户ID: {}, 组织ID: {}", userId, organizationId);
        } else {
            log.warn("用户身份验证失败 - 用户ID: {}, 组织ID: {}", userId, organizationId);
        }
        
        return valid;
    }
    
    /**
     * 从HTTP头中验证Gateway用户身份（用于STOMP握手拦截器）
     * 
     * @param headers HTTP请求头
     * @return Gateway用户信息，验证失败时返回null
     */
    public GatewayUserInfo validateUserFromHeaders(HttpHeaders headers) {
        try {
            log.debug("开始从HTTP头中验证Gateway用户身份");
            
            // 从HTTP头中获取CLOUD-AUTH头
            List<String> authHeaders = headers.get(CLOUD_AUTH_HEADER);
            if (authHeaders == null || authHeaders.isEmpty()) {
                log.warn("HTTP请求头缺少CLOUD-AUTH头，可能未通过Gateway代理");
                return null;
            }
            
            String authHeader = authHeaders.get(0);
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("CLOUD-AUTH头为空");
                return null;
            }
            
            // 解析CLOUD-AUTH头中的用户信息
            GatewayUserInfo userInfo = parseCloudAuthHeader(authHeader);
            if (userInfo == null) {
                log.warn("无法解析CLOUD-AUTH头中的用户信息");
                return null;
            }
            
            log.info("Gateway用户验证成功（HTTP头） - 用户ID: {}, 组织ID: {}, 用户类型: {}", 
                    userInfo.getUid(), userInfo.getOid(), userInfo.getUserType());
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("Gateway用户验证异常（HTTP头）: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查WebSocket连接权限
     * 由于Gateway已经完成认证，这里主要做业务级权限检查
     * 
     * @param userInfo Gateway用户信息
     * @return true表示有WebSocket连接权限
     */
    public boolean hasWebSocketPermission(GatewayUserInfo userInfo) {
        if (userInfo == null) {
            return false;
        }
        
        // 所有类型的用户都允许WebSocket连接（因为这是用于浏览器交互）
        log.debug("WebSocket权限检查通过 - 用户ID: {}", userInfo.getUid());
        return true;
    }
    
    
}