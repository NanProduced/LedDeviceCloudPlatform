package org.nan.cloud.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class CasbinAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final Enforcer enforcer;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext ctx) {
        String path = ctx.getExchange().getRequest().getPath().value();
        String method = ctx.getExchange().getRequest().getMethod().name();
        return authentication
                .filter(Authentication::isAuthenticated)
                .map(auth -> {
                    final Jwt token = ((JwtAuthenticationToken) auth).getToken();
                    long uid = token.getClaim("uid");
                    long oid = token.getClaim("oid");
                    boolean pass = enforcer.enforce(uid, oid, path, method);
                    if (!pass) throw new AccessDeniedException("Access denied");
                    return new AuthorizationDecision(true);
                })
                .switchIfEmpty(Mono.just(new AuthorizationDecision(false)));
    }
}
