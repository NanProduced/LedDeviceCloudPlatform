package org.nan.cloud.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * OAuth2 Client Authorization Endpoint /oauth2/authorization/{clientRegId} <br/>
 * 请求解析器扩展实现 <br/>
 * 支持前端定义一个redirect_uri，后端动态重定向到该uri
*/
@Slf4j
public class SaveRequestServerOauth2AuthorizationRequestResolver extends DefaultServerOAuth2AuthorizationRequestResolver {

    private static final String PARAM_REDIRECT_URI = "redirect_uri";

    /**
     * webSession的saveRequest属性名
     */
    private static final String DEFAULT_SAVED_REQUEST_ATTR = "SPRING_SECURITY_SAVED_REQUEST";

    public SaveRequestServerOauth2AuthorizationRequestResolver(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        super(clientRegistrationRepository);
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
        return super.resolve(exchange)
                .doOnNext(oAuth2AuthorizationRequest -> {
                    Optional.of(exchange.getRequest())
                            .map(ServerHttpRequest::getQueryParams)
                            .map(queryParams -> queryParams.get(PARAM_REDIRECT_URI))
                            .filter(CollectionUtils::isNotEmpty)
                            .map(redirectUris -> redirectUris.get(0))
                            .ifPresent(redirectUri -> {
                                // 将传入的redirect_uri存入WebSession的中断请求处
                                exchange.getSession().subscribe(webSession -> {
                                    webSession.getAttributes().put(DEFAULT_SAVED_REQUEST_ATTR, redirectUri);
                                    log.debug("Custom-Debug-log===>Gateway OAuth2 授权端点添加redirect_uri参数到WebSession: {}", redirectUri);
                                });
                            });
                });
    }
}
