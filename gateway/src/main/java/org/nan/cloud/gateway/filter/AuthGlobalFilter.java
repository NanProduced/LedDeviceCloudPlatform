package org.nan.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nan.cloud.gateway.common.JsonUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(Jwt.class)
                .map(this::analysisUserInfo)
                .map(user -> {
                    ServerHttpRequest request = exchange.getRequest().mutate().header("CLOUD-AUTH", user).build();
                    return exchange.mutate().request(request).build();
                })
                .defaultIfEmpty(exchange)
                .then(chain.filter(exchange));
    }

    private String analysisUserInfo(Jwt jwt) {
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("uid", (long) jwt.getClaim("uid"));
        jsonNode.put("oid", (long) jwt.getClaim("oid"));
        jsonNode.put("ugid", (long) jwt.getClaim("ugid"));
        byte[] bytes = jsonNode.toString().getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.AUTHORIZATION.getOrder() + 1;
    }
}
