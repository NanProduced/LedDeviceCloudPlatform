package org.nan.cloud.terminal.config.security.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 终端设备用户主体
 * 
 * 实现UserDetails接口，用于Spring Security认证和授权
 * 封装终端设备的身份信息和权限信息
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
public class TerminalPrincipal implements UserDetails {

    /**
     * 终端ID - 主键
     */
    private Long tid;

    /**
     * 终端名称
     */
    private String terminalName;

    /**
     * 组织ID
     */
    private Long oid;

    /**
     * 终端组ID
     */
    private Long tgid;

    /**
     * 终端状态：0-正常，1-禁用
     */
    private Integer status;

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
     * 获取用户名 - 使用终端ID作为用户名
     */
    @Override
    @JsonIgnore
    public String getUsername() {
        return tid != null ? tid.toString() : null;
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
        return status != null && status == 0;
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
        return status != null && status == 0;
    }

    /**
     * 获取终端显示名称
     */
    public String getDisplayName() {
        return terminalName != null ? terminalName : "Terminal-" + tid;
    }

    /**
     * 检查终端是否可用
     */
    public boolean isTerminalAvailable() {
        return isEnabled() && isAccountNonLocked();
    }

    @Override
    public String toString() {
        return String.format("TerminalPrincipal{tid=%d, terminalName='%s', oid=%d, tgid=%d, status=%d}", 
            tid, terminalName, oid, tgid, status);
    }
}