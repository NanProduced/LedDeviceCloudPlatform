package org.nan.cloud.auth.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.nan.cloud.auth.application.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long uid;

    private String username;

    private String password;

    private Long oid;

    private Long uGid;

    private Integer status;

    public UserPrincipal(User user) {
        this.uid = user.getUid();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.oid = user.getOid();
        this.uGid = user.getUgid();
        this.status = user.getStatus();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == 0;
    }
}
