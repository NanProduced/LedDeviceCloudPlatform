package org.nan.cloud.auth.boot.oidc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.oidc.OidcProviderConfiguration;
import org.springframework.security.oauth2.server.authorization.oidc.http.converter.OidcProviderConfigurationHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * 自定义OIDC发现端点<br>
 * auth-server/.well-known/openid-configuration<br>
 * 替换OidcProviderConfigurationEndpointFilter<br>
 * @see org.springframework.security.oauth2.server.authorization.oidc.web.OidcProviderConfigurationEndpointFilter
 */
public final class OidcCustomProviderConfigurationEndpointFilter extends OncePerRequestFilter {

    private static final String DEFAULT_OIDC_PROVIDER_CONFIGURATION_ENDPOINT_URI = "/.well-known/openid-configuration";

    private final AuthorizationServerSettings settings;
    private final RequestMatcher requestMatcher;
    private final OidcProviderConfigurationHttpMessageConverter odcProviderConfigurationHttpMessageConverter = new OidcProviderConfigurationHttpMessageConverter();

    public OidcCustomProviderConfigurationEndpointFilter(AuthorizationServerSettings authorizationServerSettings) {
        this.settings = authorizationServerSettings;
        this.requestMatcher = new AntPathRequestMatcher(
                DEFAULT_OIDC_PROVIDER_CONFIGURATION_ENDPOINT_URI,
                HttpMethod.GET.name()
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String issuer = AuthorizationServerContextHolder.getContext().getIssuer();
        OidcProviderConfiguration providerConfiguration = OidcProviderConfiguration.builder()
                .issuer(issuer)
                .authorizationEndpoint(asUrl(issuer, settings.getAuthorizationEndpoint()))
                .tokenEndpoint(asUrl(issuer, settings.getTokenEndpoint()))
                .tokenEndpointAuthenticationMethods(clientAuthenticationMethods())
                .jwkSetUrl(asUrl(issuer, settings.getJwkSetEndpoint()))
                .userInfoEndpoint(asUrl(issuer, settings.getOidcUserInfoEndpoint()))
                .responseType(OAuth2AuthorizationResponseType.CODE.getValue())
                .grantType(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                .grantType(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                .grantType(AuthorizationGrantType.REFRESH_TOKEN.getValue())
                .subjectType("public")
                .idTokenSigningAlgorithm(SignatureAlgorithm.RS256.getName())
                .scope(OidcScopes.OPENID)
                .endSessionEndpoint(asUrl(issuer, "/end_session_endpoint"))
                .build();
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        odcProviderConfigurationHttpMessageConverter.write(providerConfiguration, MediaType.APPLICATION_JSON, httpResponse);
    }

    // todo: 待确认
    private static Consumer<List<String>> clientAuthenticationMethods() {
        return (authenticationMethods) -> {
            authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
            authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());
            //authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue());
            //authenticationMethods.add(ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue());
            authenticationMethods.add(ClientAuthenticationMethod.NONE.getValue());
        };
    }

    private static String asUrl(String issuer, String endpoint) {
        return UriComponentsBuilder.fromUriString(issuer).path(endpoint).build().toUriString();
    }
}
