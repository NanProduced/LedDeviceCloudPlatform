package org.nan.cloud.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * An implementation of a {@link ReactiveOAuth2AuthorizedClientProvider} for the
 * {@link AuthorizationGrantType#REFRESH_TOKEN refresh_token} grant.<br/>
 * 扩展实现 - 刷新token后更新SecurityContext中的id_token（避免后续OIDC登出id_token对应不上）
 *
 * @see org.springframework.security.oauth2.client.RefreshTokenReactiveOAuth2AuthorizedClientProvider
 * @see OidcAuthorizationCodeAuthenticationProvider
 * @see DefaultReactiveOAuth2AuthorizedClientManager
 */
@Slf4j
public class RefreshTokenReactiveOAuth2AuthorizedClientProvider implements ReactiveOAuth2AuthorizedClientProvider {

    private ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> accessTokenResponseClient = new WebClientReactiveRefreshTokenTokenResponseClient();

    private Duration clockSkew = Duration.ofSeconds(60);

    private Clock clock = Clock.systemUTC();

    private ServerSecurityContextRepository serverSecurityContextRepository;

    /**
     * 参见 DefaultReactiveOAuth2AuthorizedClientManager
     * 获取当前context中的ServerWebExchange
     */
    private static final Mono<ServerWebExchange> currentServerWebExchange = Mono.deferContextual(Mono::just)
            .filter((c) -> c.hasKey(ServerWebExchange.class))
            .map((c) -> c.get(ServerWebExchange.class));

    public RefreshTokenReactiveOAuth2AuthorizedClientProvider(ServerSecurityContextRepository serverSecurityContextRepository) {
        this.serverSecurityContextRepository = serverSecurityContextRepository;
    }

    /**
     * Attempt to re-authorize the
     * {@link OAuth2AuthorizationContext#getClientRegistration() client} in the provided
     * {@code context}. Returns an empty {@code Mono} if re-authorization is not
     * supported, e.g. the client is not authorized OR the
     * {@link OAuth2AuthorizedClient#getRefreshToken() refresh token} is not available for
     * the authorized client OR the {@link OAuth2AuthorizedClient#getAccessToken() access
     * token} is not expired.
     *
     * <p>
     * The following {@link OAuth2AuthorizationContext#getAttributes() context attributes}
     * are supported:
     * <ol>
     * <li>{@code "org.springframework.security.oauth2.client.REQUEST_SCOPE"} (optional) -
     * a {@code String[]} of scope(s) to be requested by the
     * {@link OAuth2AuthorizationContext#getClientRegistration() client}</li>
     * </ol>
     * @param context the context that holds authorization-specific state for the client
     * @return the {@link OAuth2AuthorizedClient} or an empty {@code Mono} if
     * re-authorization is not supported
     */
    @Override
    public Mono<OAuth2AuthorizedClient> authorize(OAuth2AuthorizationContext context) {
        Assert.notNull(context, "context cannot be null");
        OAuth2AuthorizedClient authorizedClient = context.getAuthorizedClient();
        if (authorizedClient == null || authorizedClient.getRefreshToken() == null
                || !hasTokenExpired(authorizedClient.getAccessToken())) {
            return Mono.empty();
        }
        Object requestScope = context.getAttribute(OAuth2AuthorizationContext.REQUEST_SCOPE_ATTRIBUTE_NAME);
        Set<String> scopes = Collections.emptySet();
        if (requestScope != null) {
            Assert.isInstanceOf(String[].class, requestScope, "The context attribute must be of type String[] '"
                    + OAuth2AuthorizationContext.REQUEST_SCOPE_ATTRIBUTE_NAME + "'");
            scopes = new HashSet<>(Arrays.asList((String[]) requestScope));
        }
        ClientRegistration clientRegistration = context.getClientRegistration();
        OAuth2RefreshTokenGrantRequest refreshTokenGrantRequest = new OAuth2RefreshTokenGrantRequest(clientRegistration,
                authorizedClient.getAccessToken(), authorizedClient.getRefreshToken(), scopes);
        return Mono.just(refreshTokenGrantRequest).flatMap(this.accessTokenResponseClient::getTokenResponse)
    /* ========================================================= 扩展结束 ======================================================= */
                /* == 刷新token后更新SecurityContext中的id_token（避免后续OIDC登出id_token对应不上）======= */
                //获取ServerWebExchange并与OAuth2AccessTokenResponse合并Tuple
                .zipWith(currentServerWebExchange)
                .flatMap(tuple -> {
                    OAuth2AccessTokenResponse oAuth2AccessTokenResponse = tuple.getT1();
                    ServerWebExchange serverWebExchange = tuple.getT2();
                    //打印当前返回结果中新的id_token值
                    String idTokenVal = (String) oAuth2AccessTokenResponse.getAdditionalParameters().get(OidcParameterNames.ID_TOKEN);
                    log.debug("SecurityContext reset new id_token: {}", idTokenVal);
                    //获取当前上下文中的SecurityContext
                    return ReactiveSecurityContextHolder.getContext()
                            //更新SecurityContext中的用户信息的id_token
                            .map(securityContext -> this.resetSecurityContextIdToken(securityContext, oAuth2AccessTokenResponse, clientRegistration))
                            //更新SecurityContext到仓库存储（WebSession.SPRING_SECURITY_CONTEXT）
                            .flatMap(securityContext -> this.serverSecurityContextRepository.save(serverWebExchange, securityContext))
                            //返回后续需要的OAuth2AccessTokenResponse（兼容原逻辑）
                            .thenReturn(oAuth2AccessTokenResponse);
                })
    /* ========================================================= 扩展结束 ======================================================= */
                .onErrorMap(OAuth2AuthorizationException.class,
                        (e) -> new ClientAuthorizationException(e.getError(), clientRegistration.getRegistrationId(),
                                e))
                .map((tokenResponse) -> new OAuth2AuthorizedClient(clientRegistration, context.getPrincipal().getName(),
                        tokenResponse.getAccessToken(), tokenResponse.getRefreshToken()));
    }

    /**
     * 重置SecurityContext.Authentication -> OAuth2AuthenticationToken -> principal -> OidcUser -> OidcIdToken
     *
     * @param securityContext           Spring Security用户信息上下文
     * @param oAuth2AccessTokenResponse 刷新token结果响应（包含最新的id_token）
     * @param clientRegistration        客户端注册信息
     * @return 修改后的Spring Security用户信息上下文
     */
    private SecurityContext resetSecurityContextIdToken(SecurityContext securityContext, OAuth2AccessTokenResponse oAuth2AccessTokenResponse, ClientRegistration clientRegistration) {
        // 返回刷新token结果中不包含id_token则直接返回
        if (null == oAuth2AccessTokenResponse.getAdditionalParameters()
                || !oAuth2AccessTokenResponse.getAdditionalParameters().containsKey(OidcParameterNames.ID_TOKEN)) {
            return securityContext;
        }

        // 解析SpringSecurityContext
        OAuth2AuthenticationToken oAuth2AuthenticationToken = Optional.ofNullable(securityContext)
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication instanceof OAuth2AuthenticationToken)
                .map(OAuth2AuthenticationToken.class::cast)
                .orElse(null);

        Optional.ofNullable(oAuth2AuthenticationToken)
                .map(OAuth2AuthenticationToken::getPrincipal)
                .filter(principal -> principal instanceof OidcUser)
                .map(OidcUser.class::cast)
                .ifPresent(oidcUser -> {
                    // 使用新的刷新token结果中的id_token构建新的用户信息
                    OidcIdToken idToken = createOidcToken(clientRegistration, oAuth2AccessTokenResponse);
                    OidcUser newOidcUser = new DefaultOidcUser(oidcUser.getAuthorities(), idToken, oidcUser.getUserInfo());
                    Authentication newOAuth2Authentication = new OAuth2AuthenticationToken(newOidcUser, oAuth2AuthenticationToken.getAuthorities(), oAuth2AuthenticationToken.getAuthorizedClientRegistrationId());
                    //更新SecurityContext对象中的用户信息
                    securityContext.setAuthentication(newOAuth2Authentication);
                });
        return securityContext;
    }

    /** =============== 解析IdToken，实现逻辑截取自OidcAuthorizationCodeAuthenticationProvider =========================== */
    private static final String INVALID_ID_TOKEN_ERROR_CODE = "invalid_id_token";
    private JwtDecoderFactory<ClientRegistration> jwtDecoderFactory = new OidcIdTokenDecoderFactory();

    private OidcIdToken createOidcToken(ClientRegistration clientRegistration,
                                        OAuth2AccessTokenResponse accessTokenResponse) {
        JwtDecoder jwtDecoder = this.jwtDecoderFactory.createDecoder(clientRegistration);
        Jwt jwt = getJwt(accessTokenResponse, jwtDecoder);
        OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
                jwt.getClaims());
        return idToken;
    }

    private Jwt getJwt(OAuth2AccessTokenResponse accessTokenResponse, JwtDecoder jwtDecoder) {
        try {
            Map<String, Object> parameters = accessTokenResponse.getAdditionalParameters();
            return jwtDecoder.decode((String) parameters.get(OidcParameterNames.ID_TOKEN));
        }
        catch (JwtException ex) {
            OAuth2Error invalidIdTokenError = new OAuth2Error(INVALID_ID_TOKEN_ERROR_CODE, ex.getMessage(), null);
            throw new OAuth2AuthenticationException(invalidIdTokenError, invalidIdTokenError.toString(), ex);
        }
    }
    /** ================================================================================================================ */

    private boolean hasTokenExpired(OAuth2Token token) {
        return this.clock.instant().isAfter(token.getExpiresAt().minus(this.clockSkew));
    }

    /**
     * Sets the client used when requesting an access token credential at the Token
     * Endpoint for the {@code refresh_token} grant.
     * @param accessTokenResponseClient the client used when requesting an access token
     * credential at the Token Endpoint for the {@code refresh_token} grant
     */
    public void setAccessTokenResponseClient(
            ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> accessTokenResponseClient) {
        Assert.notNull(accessTokenResponseClient, "accessTokenResponseClient cannot be null");
        this.accessTokenResponseClient = accessTokenResponseClient;
    }

    /**
     * Sets the maximum acceptable clock skew, which is used when checking the
     * {@link OAuth2AuthorizedClient#getAccessToken() access token} expiry. The default is
     * 60 seconds.
     *
     * <p>
     * An access token is considered expired if
     * {@code OAuth2AccessToken#getExpiresAt() - clockSkew} is before the current time
     * {@code clock#instant()}.
     * @param clockSkew the maximum acceptable clock skew
     */
    public void setClockSkew(Duration clockSkew) {
        Assert.notNull(clockSkew, "clockSkew cannot be null");
        Assert.isTrue(clockSkew.getSeconds() >= 0, "clockSkew must be >= 0");
        this.clockSkew = clockSkew;
    }

    /**
     * Sets the {@link Clock} used in {@link Instant#now(Clock)} when checking the access
     * token expiry.
     * @param clock the clock
     */
    public void setClock(Clock clock) {
        Assert.notNull(clock, "clock cannot be null");
        this.clock = clock;
    }
}

