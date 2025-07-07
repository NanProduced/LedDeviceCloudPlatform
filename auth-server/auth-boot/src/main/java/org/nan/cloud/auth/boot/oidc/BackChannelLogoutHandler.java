package org.nan.cloud.auth.boot.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.BackChannelLogoutRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.nan.cloud.auth.boot.config.OAuth2Constants;
import org.nan.cloud.auth.boot.config.OAuth2ServerProps;
import org.nan.cloud.auth.boot.oauth.OidcAuthorizationService;
import org.nan.cloud.auth.boot.oidc.enums.LoginStateEnum;
import org.nan.cloud.auth.boot.utils.Jwks;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class BackChannelLogoutHandler implements AuthenticationSuccessHandler {

    private RegisteredClientRepository registeredClientRepository;

    private OidcAuthorizationService oidcAuthorizationService;

    private OAuth2ServerProps oAuth2ServerProps;

    private RSAKey rsaKey;

    private JWSSigner jwsSigner;

    @SneakyThrows
    public BackChannelLogoutHandler(RegisteredClientRepository registeredClientRepository,
                                    OidcAuthorizationService oidcAuthorizationService,
                                    OAuth2ServerProps oAuth2ServerProps) {
        this.registeredClientRepository = registeredClientRepository;
        this.oidcAuthorizationService = oidcAuthorizationService;
        this.oAuth2ServerProps = oAuth2ServerProps;

        rsaKey = Jwks.convertRsaKey(this.oAuth2ServerProps);
        jwsSigner = new RSASSASigner(rsaKey);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 是否开启SLO
        if (!oAuth2ServerProps.getEnableOidcSLO()) {
            log.debug("Custom-Debug-log===>user: {} log out, OIDC SLO not enable", authentication.getName());
            return;
        }

        // OP 本地 Session 立即失效
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        // 已经验证通过的注销AuthorizationToken
        OidcLogoutAuthenticationToken logoutAuthenticationToken = (OidcLogoutAuthenticationToken) authentication;
        String sessionId = logoutAuthenticationToken.getSessionId();
        OAuth2Authorization curOAuth2Authorization = oidcAuthorizationService.findByIdToken(logoutAuthenticationToken.getIdTokenHint());
        if (null == curOAuth2Authorization) {
            log.error("Can not find OAuth2Authentication for idToken!");
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().flush();
            return;
        }
        // 查询当前RP的Client注册信息
        RegisteredClient curClient = registeredClientRepository.findById(curOAuth2Authorization.getRegisteredClientId());
        // 找出当前所有同一sessionId的登录记录
        List<OAuth2Authorization> allAuth = oidcAuthorizationService.findBySessionId(sessionId);
        Map<String, OAuth2Authorization> regClientMap = new HashMap<>(allAuth.size());
        allAuth.forEach(auth -> regClientMap.put(auth.getRegisteredClientId(), auth));

        // 依次更新登出状态
        allAuth.forEach(auth -> {
            // 更新用户登录状态
            auth = OAuth2Authorization.from(auth)
                    .attribute(OAuth2Constants.AUTHORIZATION_ATTRS.LOGIN_STATE, LoginStateEnum.LOGOUT.getCode())
                    .build();
            // 更新token无效状态
            auth = invalidate(auth, auth.getRefreshToken().getToken());
            oidcAuthorizationService.save(auth);
            // back channel 处理（front channel暂时不实现）
            if (!curOAuth2Authorization.getRegisteredClientId().equals(auth.getRegisteredClientId())) {
                RegisteredClient registeredClient = registeredClientRepository.findById(auth.getRegisteredClientId());
                if (null != registeredClient.getClientSettings() && null != registeredClient.getClientSettings().getSetting(OAuth2Constants.CLIENT_SETTINGS.BACKCHANNEL_REQUIRE)
                        && registeredClient.getClientSettings().getSetting(OAuth2Constants.CLIENT_SETTINGS.BACKCHANNEL_REQUIRE).equals(Boolean.TRUE)) {
                    if (null == registeredClient.getClientSettings().getSetting(OAuth2Constants.CLIENT_SETTINGS.BACKCHANNEL_LOGOUT_URI)) {
                        log.error("Custom-Debug-log===>No back channel logout uri setting for clientId:{}", registeredClient.getId());
                    }
                    String backchannelLogoutUri = registeredClient.getClientSettings().getSetting(OAuth2Constants.CLIENT_SETTINGS.BACKCHANNEL_LOGOUT_URI);
                    // 生成logout_token
                    JWT logoutToken = generateLogoutToken(registeredClient, regClientMap.get(registeredClient.getId()));
                    sendBackchannelLogoutRequest(backchannelLogoutUri, logoutToken);
                }
            }
        });

        // 重定向回SLO调用RP的登出回调页面
        redirectPostLogoutRedirectUri(request, response, curClient);

    }

    private void redirectPostLogoutRedirectUri(HttpServletRequest request, HttpServletResponse response, RegisteredClient registeredClient) throws IOException {
        response.sendRedirect(this.determinePostLogoutRedirectUri(request, registeredClient));
    }

    private final String STATE_PARAMETER_FORMAT = "%s?state=%s";

    private String determinePostLogoutRedirectUri(HttpServletRequest request, RegisteredClient registeredClient) {
        String postLogoutRedirectUri = request.getParameter(OAuth2Constants.OIDC_PARAMETERS.POST_LOGOUT_REDIRECT_URI);
        String state = request.getParameter(OAuth2ParameterNames.STATE);

        if (StringUtils.hasText(postLogoutRedirectUri)
                && null != registeredClient
                && registeredClient.getPostLogoutRedirectUris().contains(postLogoutRedirectUri)) {
            // 合法，重定向
            return StringUtils.hasText(state) ? String.format(STATE_PARAMETER_FORMAT, postLogoutRedirectUri, state) :
                    postLogoutRedirectUri;
        } else {
            // 不合法或没传，使用默认值或返回错误
            return oAuth2ServerProps.getLogOutDefaultRedirectUrl();
        }
    }

    @SneakyThrows
    private JWT generateLogoutToken(RegisteredClient registeredClient, OAuth2Authorization oAuth2Authorization) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(oAuth2ServerProps.getIssuer())
                .subject(oAuth2Authorization.getPrincipalName())
                .audience(registeredClient.getClientId())
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + registeredClient.getTokenSettings().getAccessTokenTimeToLive().toMillis()))
                //.jwtID()
                .claim(OAuth2Constants.CLAIMS.SID, oAuth2Authorization.getAttribute(OAuth2Constants.AUTHORIZATION_ATTRS.SESSION_ID))
                .claim(OAuth2Constants.CLAIMS.EVENTS, OAuth2Constants.CLAIMS.EVENTS_VALUE)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet);

        // Compute the RSA signature
        signedJWT.sign(jwsSigner);
        return signedJWT;
    }

    private void sendBackchannelLogoutRequest(String backChannelLogoutUri, JWT logoutToken) {
        try {
            URI backchannelLogoutEndpointForRP = new URI(backChannelLogoutUri);
            BackChannelLogoutRequest backchannelLogoutRequest = new BackChannelLogoutRequest(backchannelLogoutEndpointForRP, logoutToken);
            HTTPResponse httpResponse = backchannelLogoutRequest.toHTTPRequest().send();
            if (httpResponse.indicatesSuccess()) {
                log.debug("Custom-Debug-log===>send backchannel logout uri {} success with status_code {}", backChannelLogoutUri, httpResponse.getStatusCode());
            } else {
                log.debug("Custom-Debug-log===>send backchannel logout uri {} failed with status_code {}", backChannelLogoutUri, httpResponse.getStatusCode());
            }
        } catch (Throwable e) {
            log.error("Custom-Debug-log===>send backchannel logout uri: {} error", backChannelLogoutUri, e);
        }
    }

    /**
     *
     * @param authorization
     * @param token
     * @return
     * @param <T>
     * @see org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthenticationProviderUtils
     */
    private <T extends AbstractOAuth2Token> OAuth2Authorization invalidate(OAuth2Authorization authorization, T token) {
        // @formatter:off
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.from(authorization)
                .token(token,
                        (metadata) ->
                                metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true));

        if (OAuth2RefreshToken.class.isAssignableFrom(token.getClass())) {
            authorizationBuilder.token(
                    authorization.getAccessToken().getToken(),
                    (metadata) ->
                            metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true));

            OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                    authorization.getToken(OAuth2AuthorizationCode.class);
            if (authorizationCode != null && !authorizationCode.isInvalidated()) {
                authorizationBuilder.token(
                        authorizationCode.getToken(),
                        (metadata) ->
                                metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true));
            }
        }
        // @formatter:on

        return authorizationBuilder.build();
    }
}
