package org.nan.cloud.auth.boot.oidc.template;

import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

public abstract class AbstractOidcTokenCustomer {

    /**
     * 注册第三方用户为当前系统用户，并返回注册后的当前系统用户ID<br/>
     * 用注册后的用户ID作为token.claim.sub
     */
    public String registerThirdUser(JwtEncodingContext jwtEncodingContext) {
        return jwtEncodingContext.getPrincipal().getName();
    }

    /**
     * 扩展access_token
     * @param jwtEncodingContext token上下文
     */
    public void extendAccessToken(JwtEncodingContext jwtEncodingContext) {

    }

    /**
     * 拓展refreshToken
     * @param jwtEncodingContext token上下文
     */
    public void extendRefreshToken(JwtEncodingContext jwtEncodingContext) {

    }

    /**
     * 拓展IdToken
     * @param jwtEncodingContext token上下文
     */
    public void extendIdToken(JwtEncodingContext jwtEncodingContext) {

    }
}
