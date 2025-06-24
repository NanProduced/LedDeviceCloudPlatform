package org.nan.cloud.auth.boot.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Value("${custom.security.jwt-key}")
    private String keyStorePwd;

    @Value("${custom.security.jwt-alias}")
    private String keyAlias;

    @Bean
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {

        // 开启默认OAuth2端点 (/oauth2/authorize /token /jwks)
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        // 自定义登录入口（未登录跳转 /login）
        http.exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public InMemoryRegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {

        RegisteredClient frontEnd = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("front_end_client")
                .clientSecret(passwordEncoder.encode("Nan12091209"))
                .scope("terminal").scope("group").scope("user")
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .clientSettings(ClientSettings.builder().build())
                .build();

        RegisteredClient codeClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("client_grant")
                .clientSecret(passwordEncoder.encode("Nan12091209"))
                .scope("terminal").scope("group").scope("user")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://www.baidu.com")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(frontEnd, codeClient);

    }

    // Jwt编码器
    @Bean
    public JwtEncoder jwtEncoder() {
        KeyStoreKeyFactory keyFactory = new KeyStoreKeyFactory(
                new ClassPathResource("demo-jwt.jks"), keyStorePwd.toCharArray());
        KeyPair keyPair = keyFactory.getKeyPair(keyAlias, keyStorePwd.toCharArray());
        JWK jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSource<SecurityContext> jwkSource = ((jwkSelector, securityContext) -> jwkSelector.select(new JWKSet(jwk)));
        return new NimbusJwtEncoder(jwkSource);
    }
}
