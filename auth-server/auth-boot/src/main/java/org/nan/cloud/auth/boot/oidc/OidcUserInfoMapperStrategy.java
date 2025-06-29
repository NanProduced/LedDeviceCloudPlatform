package org.nan.cloud.auth.boot.oidc;

import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;

import java.util.Map;

/**
 * 用于自定义Oidc UserInfo 拓展信息
 */
public interface OidcUserInfoMapperStrategy {

    void extendClaims(Map<String, Object> claims, OidcUserInfoAuthenticationContext authenticationContext);
}
