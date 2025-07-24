package org.nan.cloud.terminal.config.security.auth;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 终端设备用户主体
 * 
 * 实现Spring Security的UserDetails接口，提供终端设备的身份信息：
 * 1. 设备标识：设备ID、设备名称、组织归属
 * 2. 认证信息：密码（加密存储）、账号状态
 * 3. 权限信息：设备操作权限（暂未使用，预留扩展）
 * 4. 会话信息：最后登录时间、登录IP等
 * 
 * 与普通用户认证的区别：
 * - 无复杂的角色权限体系，设备权限相对简单
 * - 账号永不过期，但可以被管理员禁用
 * - 凭据（密码）可以有过期时间，用于强制更新设备密钥
 * - 设备通常不需要复杂的权限检查，主要是连接认证
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
public class TerminalUserPrincipal implements UserDetails {

    /**
     * 设备ID（业务主键）
     */
    private String deviceId;
    
    /**
     * 设备名称（显示名称）
     */
    private String deviceName;
    
    /**
     * 所属组织ID
     */
    private String organizationId;
    
    /**
     * 设备密码（加密后）
     */
    private String password;
    
    /**
     * 设备是否启用
     */
    private Boolean enabled = true;
    
    /**
     * 账号是否未过期
     */
    private Boolean accountNonExpired = true;
    
    /**
     * 账号是否未锁定
     */
    private Boolean accountNonLocked = true;
    
    /**
     * 凭据是否未过期
     */
    private Boolean credentialsNonExpired = true;
    
    /**
     * 最后登录时间
     */
    private Long lastLoginTime;
    
    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 终端设备暂时不需要复杂的权限体系
        // 后续可以根据设备类型、组织等添加不同权限
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // 使用设备ID作为用户名
        return deviceId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired != null ? accountNonExpired : true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked != null ? accountNonLocked : true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired != null ? credentialsNonExpired : true;
    }

    @Override
    public boolean isEnabled() {
        return enabled != null ? enabled : true;
    }

    /**
     * 更新最后登录信息
     */
    public void updateLastLogin(String clientIp) {
        this.lastLoginTime = System.currentTimeMillis();
        this.lastLoginIp = clientIp;
    }

    /**
     * 检查设备是否可以正常使用
     */
    public boolean isDeviceAvailable() {
        return isEnabled() && isAccountNonExpired() && 
               isAccountNonLocked() && isCredentialsNonExpired();
    }

    /**
     * 获取设备显示信息
     */
    public String getDisplayInfo() {
        return String.format("设备[%s-%s]@%s", deviceId, deviceName, organizationId);
    }

    @Override
    public String toString() {
        return String.format("TerminalUserPrincipal{deviceId='%s', deviceName='%s', " +
            "organizationId='%s', enabled=%s, lastLogin=%s}", 
            deviceId, deviceName, organizationId, enabled, lastLoginTime);
    }
}