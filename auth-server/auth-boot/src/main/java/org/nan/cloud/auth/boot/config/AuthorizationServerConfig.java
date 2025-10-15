package org.nan.cloud.auth.boot.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.auth.boot.oauth.JdbcOidcAuthorizationService;
import org.nan.cloud.auth.boot.oauth.OidcAuthorizationService;
import org.nan.cloud.auth.boot.oidc.*;
import org.nan.cloud.auth.boot.oidc.template.AbstractOidcTokenCustomer;
import org.nan.cloud.auth.boot.oidc.strategy.OidcUserInfoMapperStrategy;
import org.nan.cloud.auth.boot.utils.Jwks;
import org.nan.cloud.auth.boot.utils.ObjectPostProcessorUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.web.OidcProviderConfigurationEndpointFilter;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.UUID;

import static org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration.applyDefaultSecurity;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuth2ServerProps.class)
public class AuthorizationServerConfig {

    private final OAuth2ServerProps oAuth2ServerProps;

    private final OidcUserInfoMapperStrategy oidcUserInfoMapperStrategy;

    private final AbstractOidcTokenCustomer oidcTokenCustomerImpl;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http,
                                                             RegisteredClientRepository registeredClientRepository,
                                                             OidcAuthorizationService oidcAuthorizationService,
                                                             OAuth2ServerProps oAuth2ServerProps) throws Exception {

        // 注册所有 Authorization Server 的端点安全拦截规则
        applyDefaultSecurity(http);
        // 开启 formLogin，让 /oauth2/authorize 未登录时重定向到 /login
        http
            // 支持OIDC的/userinfo
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(Customizer.withDefaults())
            )
            .getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(oidc -> oidc
                    .userInfoEndpoint(userInfo -> userInfo
                            .userInfoMapper(new DefaultOidcUserInfoMapper(oidcUserInfoMapperStrategy)))
                    .logoutEndpoint(logout -> logout
                            .logoutResponseHandler(new BackChannelLogoutHandler(registeredClientRepository, oidcAuthorizationService, oAuth2ServerProps)))
            )
            .withObjectPostProcessor(ObjectPostProcessorUtils.objectPostReturnNewObj(
                    OncePerRequestFilter.class,
                    OidcProviderConfigurationEndpointFilter.class,
                    new OidcCustomProviderConfigurationEndpointFilter(authorizationServerSettings())
            ));
        return http
            .formLogin(form -> form
                .loginPage("/login")).build();

    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        JdbcRegisteredClientRepository jdbcRegisteredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);
        if (null == jdbcRegisteredClientRepository.findByClientId("gateway-server-client")) {
            jdbcRegisteredClientRepository.save(createDefaultGatewayClient(passwordEncoder));
        }
        return jdbcRegisteredClientRepository;
    }

    private RegisteredClient createDefaultGatewayClient(PasswordEncoder passwordEncoder) {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("gateway-server-client")
                .clientSecret(passwordEncoder.encode("nanproduced"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://192.168.1.222:8082/login/oauth2/code/gateway-server")
                .postLogoutRedirectUri("http://192.168.1.222:8082/logout_status")
                .scope("openid")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        // 反向通道（Back-Channel）接收端点
                        .setting("settings.client.backchannel-logout-uri",
                                "http://192.168.1.222:8082/connect/back-channel/logout")
                        // 是否要求 RP 按 session 维度登出（可选，默认为 false）
                        .setting("settings.client.backchannel-logout-session-required", Boolean.TRUE)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .refreshTokenTimeToLive(Duration.ofHours(12))
                        .build())
                .build();
    }

    /** 授权信息持久化 */
    @Bean
    public OidcAuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository repo) {
        return new JdbcOidcAuthorizationService(jdbcTemplate, repo);
    }

    /** 授权同意持久化 */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomer() {
        return new DefaultOidcTokenCustomer(this.oidcTokenCustomerImpl);
    }

    /**
     * 自定义JWK秘钥对
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        RSAKey rsaKey = Jwks.convertRsaKey(oAuth2ServerProps);
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    /**
     * 自定义JwtEncoder（兼容原逻辑），解决当前配置类中无法获取JwtEncoder问题
     * @param jwkSource 自定义JWK秘钥对
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }


    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(oAuth2ServerProps.getIssuer())
                .authorizationEndpoint(oAuth2ServerProps.getAuthorizationEndpoint())
                .tokenEndpoint(oAuth2ServerProps.getTokenEndpoint())
                .jwkSetEndpoint(oAuth2ServerProps.getJwkSetEndpoint())
                .oidcUserInfoEndpoint(oAuth2ServerProps.getOidcUserInfoEndpoint())
                .tokenIntrospectionEndpoint(oAuth2ServerProps.getTokenIntrospectionEndpoint())
                .tokenRevocationEndpoint(oAuth2ServerProps.getTokenRevocationEndpoint())
                .oidcLogoutEndpoint(oAuth2ServerProps.getOidcEndSessionEndpoint())
                .build();
    }
}
