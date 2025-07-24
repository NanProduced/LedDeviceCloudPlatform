package org.nan.cloud.terminal.config.security.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 终端设备用户主体
 * 
 * 实现UserDetails接口，用于Spring Security认证和授权
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
public class TerminalUserPrincipal implements UserDetails {

    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String organizationId;
    private String organizationName;
    private String status;

    /**
     * 获取权限列表
     * 终端设备统一拥有ROLE_TERMINAL权限
     */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_TERMINAL"));
    }

    /**
     * 获取密码 - 对于终端设备，这里不返回实际密码
     */
    @Override
    @JsonIgnore
    public String getPassword() {
        return null;
    }

    /**
     * 获取用户名 - 使用设备ID作为用户名
     */
    @Override
    @JsonIgnore
    public String getUsername() {
        return deviceId;
    }

    /**
     * 账号是否未过期 - 终端设备账号不会过期
     */
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账号是否未锁定 - 根据状态判断
     */
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return "ACTIVE".equals(status);
    }

    /**
     * 凭证是否未过期 - 终端设备凭证不会过期
     */
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账号是否可用 - 根据状态判断
     */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }

    /**
     * 检查是否有指定权限
     */
    public boolean hasAuthority(String authority) {
        return getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    /**
     * 检查是否属于指定组织
     */
    public boolean belongsToOrganization(String orgId) {
        return organizationId != null && organizationId.equals(orgId);
    }

    /**
     * 获取设备显示名称
     */
    public String getDisplayName() {
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            return deviceName;
        }
        return deviceId;
    }

    /**
     * 检查设备类型
     */
    public boolean isDeviceType(String type) {
        return deviceType != null && deviceType.equals(type);
    }
}