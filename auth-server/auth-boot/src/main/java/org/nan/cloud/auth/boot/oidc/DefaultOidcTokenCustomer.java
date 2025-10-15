package org.nan.cloud.auth.boot.oidc;

import org.nan.cloud.auth.boot.config.OAuth2Constants;
import org.nan.cloud.auth.boot.oidc.template.AbstractOidcTokenCustomer;
import org.nan.cloud.auth.infrastructure.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 默认OIDC Token定制实现<br>
 * 策略+模板
 */
public class DefaultOidcTokenCustomer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    /**
     * 不实现具体的策略即用默认
     */
    private AbstractOidcTokenCustomer abstractOidcTokenCustomer = new AbstractOidcTokenCustomer() {
    };

    private Map<String, Consumer<JwtEncodingContext>> tokenTypeValue2ImplFuncMap = new HashMap<>(3);


    public DefaultOidcTokenCustomer(AbstractOidcTokenCustomer abstractOidcTokenCustomer) {
        if (null != abstractOidcTokenCustomer) {
            this.abstractOidcTokenCustomer = abstractOidcTokenCustomer;
        }
        this.tokenTypeValue2ImplFuncMap.put(OAuth2TokenType.ACCESS_TOKEN.getValue(), this::extendIdTokenInner);
        this.tokenTypeValue2ImplFuncMap.put(OAuth2TokenType.REFRESH_TOKEN.getValue(), this.abstractOidcTokenCustomer::extendRefreshToken);
        this.tokenTypeValue2ImplFuncMap.put(OidcParameterNames.ID_TOKEN, this::extendIdTokenInner);

    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getPrincipal() != null) {
            Authentication auth = context.getPrincipal();
            if (auth == null || auth.getPrincipal() == null) {
                return;
            }

            Object p = auth.getPrincipal();

            if (p instanceof UserPrincipal user) {
                context.getClaims()
                        .claim("uid", user.getUid().toString())
                        .claim("oid", user.getOid().toString())
                        .claim("ugid", user.getUGid().toString())
                        .claim("userType", user.getUserType().toString());
            }
        }
    }

    private void extendAccessTokenInner(JwtEncodingContext jwtEncodingContext) {
        String uid = jwtEncodingContext.getPrincipal().getName();
        // 第三方登录，调用第三方用户自动注册逻辑（非OAuth2 Client第三方登录的情况均为UniLoginAuthenticationToken）
        if (jwtEncodingContext.getPrincipal().getClass().getName().equals("org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken")) {
            String newRegUid = this.abstractOidcTokenCustomer.registerThirdUser(jwtEncodingContext);
            // 将新注册的账户覆盖原三方账户
            this.resetNewRegUserIdInJwtContext(newRegUid, jwtEncodingContext);
        }
        this.abstractOidcTokenCustomer.extendAccessToken(jwtEncodingContext);
    }

    private void resetNewRegUserIdInJwtContext(String newRegUid, JwtEncodingContext jwtEncodingContext) {
        try {
            // 将sub设置为新注册的三方用户账户id
            jwtEncodingContext.getClaims().claim("sub", newRegUid);

            OAuth2Authorization oAuth2Authorization = jwtEncodingContext.getAuthorization();
            Field principalNameField = OAuth2Authorization.class.getDeclaredField("principalName");
            principalNameField.setAccessible(true);
            principalNameField.set(oAuth2Authorization, newRegUid);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AuthenticationServiceException(e.getMessage());
        }
    }

    /**
     * 拓展内部idToken(添加sessionId)
     * @param jwtEncodingContext token上下文
     */
    private void extendIdTokenInner(JwtEncodingContext jwtEncodingContext) {
        String loginSessionId = jwtEncodingContext.getAuthorization().getAttribute(OAuth2Constants.AUTHORIZATION_ATTRS.SESSION_ID);
        jwtEncodingContext.getClaims().claim(OAuth2Constants.SID, loginSessionId);
        this.abstractOidcTokenCustomer.extendIdToken(jwtEncodingContext);
    }
}


