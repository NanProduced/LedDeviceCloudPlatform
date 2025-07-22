package org.nan.cloud.message.infrastructure.websocket.security;

import lombok.Builder;
import lombok.Data;

/**
 * Gateway传递的用户信息
 * 
 * 从Gateway的CLOUD-AUTH头中解析的用户信息，
 * 用于WebSocket连接的用户身份识别。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
public class GatewayUserInfo {
    
    /**
     * 用户ID
     */
    private Long uid;
    
    /**
     * 组织ID
     */
    private Long oid;
    
    /**
     * 用户组ID
     */
    private Long ugid;
    
    /**
     * 用户类型
     * 0: 普通用户, 1: 管理员, 2: 超级管理员
     */
    private Integer userType;
    
    /**
     * 检查是否为管理员
     * 
     * @return true表示是管理员，false表示普通用户
     */
    public boolean isAdmin() {
        return userType != null && userType >= 1;
    }
    
    /**
     * 检查是否为超级管理员
     * 
     * @return true表示是超级管理员，false表示非超级管理员
     */
    public boolean isSuperAdmin() {
        return userType != null && userType >= 2;
    }
    
    /**
     * 获取用户ID字符串
     * 
     * @return 用户ID字符串
     */
    public String getUserIdString() {
        return uid != null ? uid.toString() : null;
    }
    
    /**
     * 获取组织ID字符串
     * 
     * @return 组织ID字符串
     */
    public String getOrganizationIdString() {
        return oid != null ? oid.toString() : null;
    }
    
    /**
     * 获取用户组ID字符串
     * 
     * @return 用户组ID字符串
     */
    public String getUserGroupIdString() {
        return ugid != null ? ugid.toString() : null;
    }
    
    @Override
    public String toString() {
        return String.format("GatewayUserInfo{uid=%d, oid=%d, ugid=%d, userType=%d}", 
                uid, oid, ugid, userType);
    }
}