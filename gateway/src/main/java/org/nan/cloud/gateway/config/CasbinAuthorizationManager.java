package org.nan.cloud.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CasbinAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final Enforcer enforcer;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext ctx) {
        String path = ctx.getExchange().getRequest().getPath().value();
        String method = ctx.getExchange().getRequest().getMethod().name();
//        return authentication
//                .filter(Authentication::isAuthenticated)
//                .map(auth -> {
//                    final Jwt token = ((JwtAuthenticationToken) auth).getToken();
//                    long uid = token.getClaim("uid");
//                    long oid = token.getClaim("oid");
//                    boolean pass = enforcer.enforce(uid, oid, path, method);
//                    if (!pass) throw new AccessDeniedException("Access denied");
//                    return new AuthorizationDecision(true);
//                })
//                .switchIfEmpty(Mono.just(new AuthorizationDecision(false)));
        return authentication
                .filter(Authentication::isAuthenticated)
                .flatMap(auth -> {
                    String uid;
                    String oid;
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();
                        uid = jwt.getClaim("uid");
                        oid = jwt.getClaim("oid");
                    }
                    else if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
                        Map<String, Object> attrs = oauth2Auth.getPrincipal().getAttributes();
                        Object u = attrs.get("uid");
                        Object o = attrs.get("oid");
                        if (!(u instanceof Number) || !(o instanceof Number)) {
                            // 如果没拿到，就认为没权限
                            return Mono.just(new AuthorizationDecision(false));
                        }
                        uid = u.toString();
                        oid = o.toString();
                    }
                    else {
                        return Mono.just(new AuthorizationDecision(false));
                    }

                    boolean allowed = enforcer.enforce(uid, oid, path, method);
                    if (allowed) {
                        return Mono.just(new AuthorizationDecision(true));
                    }
                    // 拒绝时抛出异常，后续可由 ExceptionTranslationWebFilter 转成 403
                    return Mono.error(new AccessDeniedException("Access denied"));
                })
                // 如果根本没有 Authentication，或者没通过 isAuthenticated，直接拒绝
                .switchIfEmpty(Mono.just(new AuthorizationDecision(false)));
    }
}
