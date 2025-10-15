package org.nan.cloud.gateway.handler;

import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import reactor.core.publisher.Mono;

/**
 * OAuth2 Client refresh_token过期后触发登录（避免返回500）
 *
 * @see <a href="https://github.com/spring-projects/spring-security/issues/11015#issuecomment-1081050844}">
 * Spring Cloud Gateway Getting a 500 Exception while trying to refresh_token using expired access_token and refresh_token #11015</a>
 * @see RefreshTokenReactiveOAuth2AuthorizedClientProvider
 * @see AuthorizationCodeReactiveOAuth2AuthorizedClientProvider
 */
public class RefreshTokenErrorMapReactiveOAuth2AuthorizedClientManager implements ReactiveOAuth2AuthorizedClientManager {

    private ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager;

    private ServerSecurityContextRepository securityContextRepository;

    public RefreshTokenErrorMapReactiveOAuth2AuthorizedClientManager(ReactiveClientRegistrationRepository clientRegistrationRepository,
                                                                     ServerOAuth2AuthorizedClientRepository authorizedClientRepository,
                                                                     ServerSecurityContextRepository securityContextRepository) {
        this.reactiveOAuth2AuthorizedClientManager = this.buildDefaultReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        this.securityContextRepository =  securityContextRepository;
    }
    @Override
    public Mono<OAuth2AuthorizedClient> authorize(OAuth2AuthorizeRequest authorizeRequest) {
        return this.reactiveOAuth2AuthorizedClientManager.authorize(authorizeRequest)
                //刷新token过程，如果refresh_token过期，
                //AuthServer返回400 invalid_grant，导致gateway返回500，
                //RefreshTokenReactiveOAuth2AuthorizedClientProvider转换OAuth2AuthorizationException为ClientAuthorizationException，导致返回500，
                //此处将ClientAuthorizationException转换为CredentialsExpiredException，可以触发登录（避免返回500）
                .onErrorMap(ClientAuthorizationException.class,
                        //使用如下异常避免直接重定向到authServer（CORS异常），而是重定向到scg域名下登录页
                        //默认登录页/login?error，需在Security Oauth2 Login配置ServerAuthenticationEntryPointFailureHandler
                        //即重定向到/oauth2/authorization/{clientRegId}
                        (clientAuthorizationException) -> new CredentialsExpiredException("refresh_token expired!"));
        //使用如下异常，则直接重定向302到OAuth2 authorization_endpoint -> 导致浏览器端CORS异常，即从scg域名直接重定向到authServer域名
        //(clientAuthorizationException) -> new ClientAuthorizationRequiredException(clientAuthorizationException.getClientRegistrationId()));
    }

    /**
     * 构建默认ReactiveOAuth2AuthorizedClientManager（支持authorization_code, refresh_token）
     *
     * @param clientRegistrationRepository client信息注册信息仓库
     * @param authorizedClientRepository   授权客户端信息管理仓库
     * @return 默认ReactiveOAuth2AuthorizedClientManager
     * @see org.springframework.cloud.gateway.config.GatewayReactiveOAuth2AutoConfiguration
     */
    private ReactiveOAuth2AuthorizedClientManager buildDefaultReactiveOAuth2AuthorizedClientManager(ReactiveClientRegistrationRepository clientRegistrationRepository,
                                                                                                    ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                //支持授权码流程
                .authorizationCode()
                /** 自定义RefreshToken刷新逻辑 - 支持执行refresh token后同步修改WebSession.SPRING_SECURITY_CONTEXT中idToken值 */
                .provider(new RefreshTokenReactiveOAuth2AuthorizedClientProvider(securityContextRepository))
                .build();
        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }
}
