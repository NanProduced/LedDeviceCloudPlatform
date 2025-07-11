package org.nan.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nan.cloud.gateway.common.JsonUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Mono<ClaimAccessor> tokenMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    // Bearer Token 场景
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        return Mono.just(jwtAuth.getToken());
                    }
                    // OIDC Login 场景：DefaultOidcUser
                    if (auth instanceof OAuth2AuthenticationToken oidcAuth
                            && oidcAuth.getPrincipal() instanceof DefaultOidcUser oidcUser) {
                        // 直接拿到它的 idToken（OidcIdToken 也实现 ClaimAccessor）
                        return Mono.just(oidcUser.getIdToken());
                    }
                    return Mono.empty();
                });
        return tokenMono
                .flatMap(token -> {
                    String hdr = buildUserHeader(token.getClaims());
                    ServerHttpRequest rq = exchange.getRequest()
                            .mutate().header("CLOUD-AUTH", hdr).build();
                    return chain.filter(exchange.mutate().request(rq).build());
                });
    }

    private String buildUserHeader(Map<String,Object> claims) {
        ObjectNode jsonNode = JsonUtils.getObjectMapper().createObjectNode();
        jsonNode.put("uid", Long.parseLong((String) claims.get("uid")));
        jsonNode.put("oid", Long.parseLong((String) claims.get("oid")));
        jsonNode.put("ugid", Long.parseLong((String) claims.get("ugid")));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(jsonNode.toString().getBytes(UTF_8));
    }

    private String analysisUserInfo(Jwt jwt) {
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("uid", Long.parseLong(jwt.getClaim("uid")));
        jsonNode.put("oid", Long.parseLong(jwt.getClaim("oid")));
        jsonNode.put("ugid", Long.parseLong(jwt.getClaim("ugid")));
        byte[] bytes = jsonNode.toString().getBytes(UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.AUTHORIZATION.getOrder() + 1;
    }
}
