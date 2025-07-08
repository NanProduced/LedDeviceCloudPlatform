package org.nan.cloud.auth.boot.login;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

import java.io.Serial;
import java.util.Collection;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonIgnoreProperties("authParams")
public class ExtendedLoginAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    private Object principal;

    private Object credentials;

    @Getter
    @Setter
    private Map<String, String> authParams;

    public ExtendedLoginAuthenticationToken() {
        super(null);
    }

    /**
     * 未验证的token
     * @param authParams
     */
    public ExtendedLoginAuthenticationToken(Map<String, String> authParams) {
        super(null);
        this.authParams = authParams;
        super.setAuthenticated(false);
    }

    /**
     * 通过认证的token 包含完整验证信息
     * @param principal
     * @param credentials
     * @param authorities
     * @param authParams
     */
    public ExtendedLoginAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities, Map<String, String> authParams) {
        super(authorities);
        Assert.notNull(principal, "Authentication Principal must not be null!");
        this.principal = principal;
        this.credentials = credentials;
        this.authParams = authParams;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.authParams = null;
    }


}

