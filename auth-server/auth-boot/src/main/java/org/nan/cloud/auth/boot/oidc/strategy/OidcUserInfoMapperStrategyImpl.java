package org.nan.cloud.auth.boot.oidc.strategy;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.auth.boot.oidc.OidcUserInfoMapperStrategy;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class OidcUserInfoMapperStrategyImpl implements OidcUserInfoMapperStrategy {

    @Override
    public void extendClaims(Map<String, Object> claims, OidcUserInfoAuthenticationContext authenticationContext) {
        OAuth2Authorization authorization = authenticationContext.getAuthorization();
        String userId = authenticationContext.getAuthorization().getPrincipalName();
        OidcIdToken idToken = authorization.getToken(OidcIdToken.class).getToken();
        OAuth2AccessToken accessToken = authenticationContext.getAccessToken();
        Set<String> accessTokenScopes = accessToken.getScopes();

        log.info("extend OIDC userInfo from {}: {}", idToken, accessToken);
    }

}
