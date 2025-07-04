package org.nan.cloud.auth.boot.oidc;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.auth.boot.config.OAuth2Constants;
import org.nan.cloud.auth.boot.config.OAuth2ServerProps;
import org.nan.cloud.auth.boot.utils.Jwks;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Slf4j
public class OidcLogoutSuccessHandler implements LogoutSuccessHandler {

    private RegisteredClientRepository registeredClientRepository;

    private OAuth2AuthorizationService oAuth2AuthorizationService;

    private OAuth2ServerProps oAuth2ServerProps;

    private RSAKey rsaKey;

    private JWSSigner jwsSigner;

    @SneakyThrows
    public OidcLogoutSuccessHandler(RegisteredClientRepository registeredClientRepository,
                                    OAuth2AuthorizationService oAuth2AuthorizationService,
                                    OAuth2ServerProps oAuth2ServerProps) {
        this.registeredClientRepository = registeredClientRepository;
        this.oAuth2AuthorizationService = oAuth2AuthorizationService;
        this.oAuth2ServerProps = oAuth2ServerProps;

        rsaKey = Jwks.convertRsaKey(this.oAuth2ServerProps);
        jwsSigner = new RSASSASigner(rsaKey);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (this.oAuth2ServerProps.getEnableOidcSLO()) {

        }
        postLogoutRedirectUriDirectly(request, response);
    }

    private void postLogoutRedirectUriDirectly(HttpServletRequest request, HttpServletResponse response) throws IOException{
        redirectPostLogoutRedirectUri(request, response, null);
    }

    /**
     * OIDC SLO单点登出
     * @param request
     * @param response
     * @param authentication
     * @throws IOException
     */
    private void oidcSloProcess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String idTokenHint = request.getParameter("id_token_hint");
        if (!StringUtils.hasText(idTokenHint)) {
            log.error("id_token_hint should not empty for OIDC end_session_point");
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().flush();
            return;
        }

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
}
