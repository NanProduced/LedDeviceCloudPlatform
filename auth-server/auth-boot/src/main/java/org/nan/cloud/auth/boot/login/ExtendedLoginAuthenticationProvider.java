package org.nan.cloud.auth.boot.login;

import org.nan.cloud.auth.infrastructure.security.ExtendedLoginUserService;
import org.nan.cloud.auth.infrastructure.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class ExtendedLoginAuthenticationProvider implements AuthenticationProvider {

    private ExtendedLoginUserService extendedLoginUserService;

    public ExtendedLoginAuthenticationProvider(ExtendedLoginUserService extendedLoginUserService) {
        this.extendedLoginUserService = extendedLoginUserService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ExtendedLoginAuthenticationToken extendedLoginAuthenticationToken = (ExtendedLoginAuthenticationToken) authentication;
        UserPrincipal userPrincipal = extendedLoginUserService.loadUserByAuthParams(extendedLoginAuthenticationToken.getAuthParams());
        extendedLoginUserService.authenticateUser(extendedLoginAuthenticationToken.getAuthParams(), userPrincipal);
        ExtendedLoginAuthenticationToken authenticatedToken = new ExtendedLoginAuthenticationToken(userPrincipal, userPrincipal.getPassword(),
                userPrincipal.getAuthorities(), extendedLoginAuthenticationToken.getAuthParams());
        authenticatedToken.setAuthParams(null);
        return authenticatedToken;
    }

    /**
     * 支持自定义的登录拓展token
     * @param authentication
     * @return
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return (ExtendedLoginAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
