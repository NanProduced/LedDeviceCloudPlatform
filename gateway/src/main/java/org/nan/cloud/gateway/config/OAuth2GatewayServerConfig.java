package org.nan.cloud.gateway.config;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.gateway.filter.RemoveJwtFilter;
import org.nan.cloud.gateway.handler.AccessDeniedHandler;
import org.nan.cloud.gateway.handler.AuthenticationEntryPoint;
import org.nan.cloud.gateway.handler.SaveRequestServerOauth2AuthorizationRequestResolver;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
public class OAuth2GatewayServerConfig {

    private final AccessDeniedHandler accessDeniedHandler;

    private final RemoveJwtFilter removeJwtFilter;

    private final IgnoreUrlsConfig ignoreUrlsConfig;

    private final CasbinAuthorizationManager casbinAuthorizationManager;

    // todo: 注意修改重定向端点
    private final static String auth_url = "/oauth2/authorization/gateway-server";

    private final static ServerWebExchangeMatcher oauth2AuthMatchers = ServerWebExchangeMatchers
            .pathMatchers(
                    "/oauth2/authorization/**",
                    "/login/oauth2/code/**");

    private static final String[] AUTH_WHITELIST = {"/", "/css/**", "/js/**", "/webjars/**", "/img/**", "/favicon.ico"};

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain oauth2AuthChain(ServerHttpSecurity http,
                                                         AuthenticationEntryPoint authenticationEntryPoint,
                                                         SaveRequestServerOauth2AuthorizationRequestResolver saveRequestServerOauth2AuthorizationRequestResolver,
                                                         ServerSecurityContextRepository securityContextRepository
                                                         ) {
        http
            .securityMatcher(oauth2AuthMatchers)
            //禁用CSRF token，否则post请求会被拦截返回403
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .exceptionHandling(e -> e
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityContextRepository(securityContextRepository)
                // 认证失败默认重定向至 /login?error
//            .oauth2Login(Customizer.withDefaults())
            .oauth2Login(login -> login
                    // 现设置为认证入口点
                    .authenticationFailureHandler(new ServerAuthenticationEntryPointFailureHandler(authenticationEntryPoint))
                    // 提取redirect_uri保存到session
                    .authorizationRequestResolver(saveRequestServerOauth2AuthorizationRequestResolver)
            )
            .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public SecurityWebFilterChain backendServiceChain(ServerHttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint,
                                                      ServerSecurityContextRepository securityContextRepository) {
        http
            .securityMatcher(new NegatedServerWebExchangeMatcher(oauth2AuthMatchers))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .exceptionHandling(e -> e
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint)
            )
            .authorizeExchange(e -> e
                    .pathMatchers(AUTH_WHITELIST).authenticated()
                    // 过滤白名单
                    .pathMatchers(ignoreUrlsConfig.getUrls().toArray(String[]::new)).permitAll()
                    // Casbin RBAC 接口权限校验
                    .anyExchange().access(casbinAuthorizationManager)
            )
            .securityContextRepository(securityContextRepository)
            .oauth2ResourceServer(oauth2 -> oauth2
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .jwt(Customizer.withDefaults()))
            .addFilterAfter(removeJwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }

    @Bean
    public ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new AuthenticationEntryPoint(auth_url);
    }

    @Bean
    @Primary
    public SaveRequestServerOauth2AuthorizationRequestResolver saveRequestServerOauth2AuthorizationRequestResolver(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        return new SaveRequestServerOauth2AuthorizationRequestResolver(clientRegistrationRepository);
    }


}
