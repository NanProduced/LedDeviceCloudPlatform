package org.nan.cloud.auth.boot.oidc;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;

import java.util.*;
import java.util.function.Function;

/**
 * 默认用户信息转换器（默认根据accessToken.scope提取相关idToken中的相关claims）
 * <br/>可根据需要实现自定义扩展OidcUserInfoMapperExtend）<br/>
 * <p>
 */
public class DefaultOidcUserInfoMapper implements Function<OidcUserInfoAuthenticationContext, OidcUserInfo> {

    private final OidcUserInfoMapperStrategy oidcUserInfoMapperStrategy;

    public DefaultOidcUserInfoMapper(OidcUserInfoMapperStrategy oidcUserInfoMapperStrategy) {
        this.oidcUserInfoMapperStrategy = oidcUserInfoMapperStrategy;
    }

    @Override
    public OidcUserInfo apply(OidcUserInfoAuthenticationContext oidcUserInfoAuthenticationContext) {
        OAuth2Authorization authorization = oidcUserInfoAuthenticationContext.getAuthorization();
        OidcIdToken idToken = authorization.getToken(OidcIdToken.class).getToken();
        OAuth2AccessToken accessToken = oidcUserInfoAuthenticationContext.getAccessToken();
        Map<String, Object> scopeRequestedClaims = getClaimsRequestedByScope(idToken.getClaims(), accessToken.getScopes());
        if (null != this.oidcUserInfoMapperStrategy) {
            this.oidcUserInfoMapperStrategy.extendClaims(
                    scopeRequestedClaims, oidcUserInfoAuthenticationContext
            );
        }

        return new OidcUserInfo(scopeRequestedClaims);
    }

    /**
     * scope.profile相关claim
     */
    private static final List<String> PROFILE_CLAIMS = Arrays.asList(
            StandardClaimNames.NAME,
            StandardClaimNames.FAMILY_NAME,
            StandardClaimNames.GIVEN_NAME,
            StandardClaimNames.MIDDLE_NAME,
            StandardClaimNames.NICKNAME,
            StandardClaimNames.PREFERRED_USERNAME,
            StandardClaimNames.PROFILE,
            StandardClaimNames.PICTURE,
            StandardClaimNames.WEBSITE,
            StandardClaimNames.GENDER,
            StandardClaimNames.BIRTHDATE,
            StandardClaimNames.ZONEINFO,
            StandardClaimNames.LOCALE,
            StandardClaimNames.UPDATED_AT
    );

    private static Map<String, Object> getClaimsRequestedByScope(Map<String, Object> claims, Set<String> requestedScope) {
        Set<String> scopeRequestedClaimNames = new HashSet<>(32);
        // OIDC认证用户唯一Id
        scopeRequestedClaimNames.add(StandardClaimNames.SUB);
        if (requestedScope.contains(OidcScopes.ADDRESS)) {
            scopeRequestedClaimNames.add(StandardClaimNames.ADDRESS);
        }
        if (requestedScope.contains(OidcScopes.EMAIL)) {
            scopeRequestedClaimNames.addAll(Arrays.asList(StandardClaimNames.EMAIL,
                    StandardClaimNames.EMAIL_VERIFIED));
        }
        if (requestedScope.contains(OidcScopes.PHONE)) {
            scopeRequestedClaimNames.addAll(Arrays.asList(StandardClaimNames.PHONE_NUMBER,
                    StandardClaimNames.PHONE_NUMBER_VERIFIED));
        }
        if (requestedScope.contains(OidcScopes.PROFILE)) {
            scopeRequestedClaimNames.addAll(PROFILE_CLAIMS);
        }
        Map<String, Object> requestedClaims = new HashMap<>(claims);
        requestedClaims.keySet().removeIf(claimName -> !scopeRequestedClaimNames.contains(claimName));
        return requestedClaims;
    }





}
