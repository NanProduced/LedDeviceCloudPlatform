package org.nan.cloud.gateway.config;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.gateway.filter.RemoveJwtFilter;
import org.nan.cloud.gateway.handler.AccessDeniedHandler;
import org.nan.cloud.gateway.handler.AuthenticationEntryPoint;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@EnableWebFluxSecurity
public class ResourceServerConfig {

    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final AccessDeniedHandler accessDeniedHandler;

    private final RemoveJwtFilter removeJwtFilter;

    private final IgnoreUrlsConfig ignoreUrlsConfig;

    private final CasbinAuthorizationManager casbinAuthorizationManager;

    @Bean
    @RefreshScope
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .authorizeExchange(ex -> ex
                    .pathMatchers(ignoreUrlsConfig.getUrls().toArray(String[]::new)).permitAll()
                    .anyExchange().access(casbinAuthorizationManager))
            .exceptionHandling(e -> e
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterAfter(removeJwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }
}
