package org.nan.cloud.auth.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.Resource;

/**
 * OAuth2 Authorization Server属性
 */
@ConfigurationProperties(prefix = OAuth2ServerProps.PREFIX)
@RefreshScope
@Data
public class OAuth2ServerProps {

    /**
     * 配置前缀
     */
    public static final String PREFIX = "spring.security.oauth2.auth-server";

    /**
     * OAuth2 issuer - 发布者（对应认证服务器URI）
     */
    private String issuer;

    private String loginPageTitle = "Common Login Center";

    /**
     * 认证服务登录页面URL（对应GET请求）
     */
    private String loginPageUrl = "/login";
    /**
     * 登录表单action（对应POST请求）
     */
    private String loginProcessingUrl = "/login";
    /**
     * 认证服务登录页面View
     */
    private String loginPageView = "login";
    /**
     * OAuth2 JSON Web Key公钥获取接口URI
     * 注:替换默认/oauth2/jwks
     */
    private String jwkSetEndpoint = "/rsa/publicKey";
    /**
     * OAuth2认证接口URI
     */
    private String authorizationEndpoint = "/oauth2/authorize";
    /**
     * OAuth2 令牌接口URI
     */
    private String tokenEndpoint = "/oauth2/token";
    /**
     * OIDC 获取用户信息接口URI
     */
    private String oidcUserInfoEndpoint = "/userinfo";
    /**
     * OAuth2 检查令牌接口URI
     */
    private String tokenIntrospectionEndpoint = "/oauth2/introspect";
    /**
     * OAuth2 吊销令牌接口URI
     */
    private String tokenRevocationEndpoint = "/oauth2/revoke";
    /**
     * 认证服务JWT解密RSA公钥
     */
    private Resource rsaPublicKey;
    /**
     * 认证服务JWT加密RSA公钥
     */
    private Resource rsaPrivateKey;
}
